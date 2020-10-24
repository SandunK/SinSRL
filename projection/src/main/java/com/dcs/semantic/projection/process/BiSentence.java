package com.dcs.semantic.projection.process;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;

/**
 * Created by Alan Akbik on 8/28/17.
 * <p>
 * Class that holds a bisentence, i.e. two sentences that are translations of each other. One of the sentences is the
 * source sentence, while the other is the target (for the purpose of annotation projection).
 */
public class BiSentence {

    // The source side of the bisentence
    private Sentence sentenceSL;

    // Target side of the bisentence
    private Sentence sentenceTL;

    // Word alignments between both sentences
    Table<Token, Token, Double> aligments = HashBasedTable.create();

    /**
     * Constructor for the bisentence. Requires source and target sentences.
     *
     * @param sentenceSL Source sentence
     * @param sentenceTL Target sentence
     */
    BiSentence(Sentence sentenceSL, Sentence sentenceTL) {
        this.sentenceSL = sentenceSL;
        this.sentenceTL = sentenceTL;
    }

    /**
     * Compute word alignments between source and target sentence using a HeuristicAligner
     *
     * @param aligner Aligner that contains source/target word similarities
     * @return Aligned sentence
     */
    public BiSentence align(HeuristicAligner aligner) {

        // this map records all possible alignments
        Map<Alignment, Double> pairDistance = Maps.newHashMap();

        // Go through all source and target language tokens
        for (Token targetToken : sentenceTL.getTokens()) {

            // skip tokens with los alignment expectation
            if (targetToken.hasLowAlignmentExpectation()) continue;

            for (Token sourceToken : sentenceSL.getTokens()) {

                if (sourceToken.hasLowAlignmentExpectation()) continue;

                // get token similarity, try combinations of lemmas and text
                double similarity = aligner.getSimilarity(sourceToken.getText().toLowerCase(), targetToken.getText().toLowerCase());
                double other = aligner.getSimilarity(sourceToken.getLemma().toLowerCase(), targetToken.getText().toLowerCase());
                if (other > similarity) similarity = other;
                other = aligner.getSimilarity(sourceToken.getText().toLowerCase(), targetToken.getLemma().toLowerCase());
                if (other > similarity) similarity = other;
                other = aligner.getSimilarity(sourceToken.getLemma().toLowerCase(), targetToken.getLemma().toLowerCase());
                if (other > similarity) similarity = other;
                if (targetToken.getText().equals(sourceToken.getText())) {
                    other = 0.5;
                    if (other > similarity) similarity = other;
                }
//                System.out.println(targetToken.getText() + sourceToken.getText() + similarity);
                // if similarity exists, add alignment
                if (similarity > 0.8) {

                    int tokenDistance = sourceToken.getId() - targetToken.getId();
                    if (targetToken.getId() > sourceToken.getId())
                        tokenDistance = targetToken.getId() - sourceToken.getId();

                    similarity -= (double) tokenDistance / 100;
                    pairDistance.put(new Alignment(sourceToken, targetToken), similarity);
                }
            }
        }

        Set<Token> mappedSource = Sets.newHashSet();
        Set<Token> mappedTarget = Sets.newHashSet();

        // go through all possible alignments, sorted by most probably alignment first
        for (Map.Entry<Alignment, Double> entry : CollectionHelper.sortMapByValueDesc(pairDistance)) {

            // add alignment if source and target token are not yet aligned
            if (entry.getValue() > 0 && !mappedSource.contains(entry.getKey().sl) && !mappedTarget.contains(entry.getKey().tl)) {
                this.aligments.put(entry.getKey().sl, entry.getKey().tl, 1.);
                mappedSource.add(entry.getKey().sl);
                mappedTarget.add(entry.getKey().tl);
            }
        }

        return this;
    }


    /**
     * Return aligned token if one exists, else returns null
     *
     * @param token Token to which alignment is sough
     * @return Aligned token if exists (null otherwise)
     */
    Token getAligned(Token token) {

        if (aligments.rowKeySet().contains(token)) {
            Map<Token, Double> row = aligments.row(token);
            return CollectionHelper.sortMapByValueDesc(row).first().getKey();
        }
        if (aligments.columnKeySet().contains(token)) {
            Map<Token, Double> column = aligments.column(token);
            return CollectionHelper.sortMapByValueDesc(column).first().getKey();
        }

        return null;
    }


    /**
     * Copy alignments from other BiSentence
     *
     * @param biSentence BiSentence from which to copy alignments
     */
    void copyAlignments(BiSentence biSentence) {

        for (Table.Cell<Token, Token, Double> alignment : biSentence.aligments.cellSet()) {

            Token source = this.getSentenceSL().getToken(Objects.requireNonNull(alignment.getRowKey()).getId());
            Token target = this.getSentenceTL().getToken(Objects.requireNonNull(alignment.getColumnKey()).getId());
            this.aligments.put(source, target, alignment.getValue());
        }
    }

    // ------------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------------
    Sentence getSentenceSL() {
        return sentenceSL;
    }

    Sentence getSentenceTL() {
        return sentenceTL;
    }


    // ------------------------------------------------------------------------
    // Overridden methods
    // ------------------------------------------------------------------------
    public String toString() {

        // first determine longest token
        int longestSLToken = 0;
        for (Token token : this.sentenceSL.getTokens()) {
            int length = token.getText().length();
            if (length > longestSLToken) longestSLToken = length;
        }
        int longestTLToken = 0;
        for (Token token : this.sentenceTL.getTokens()) {
            int length = token.getText().length();
            if (length > longestTLToken) longestTLToken = length;
        }

        StringBuilder out = new StringBuilder("\n" + StringHelper.addWhitespaces("", longestTLToken));
        String separatir = " ";
        for (Token sl : this.sentenceSL.getTokens()) {
            out.append(separatir + StringHelper.addWhitespaces(sl.getText(), sl.getText().length() + 1));
        }
        out.append("\n");
        for (Token tl : this.sentenceTL.getTokens()) {
            out.append(StringHelper.addWhitespaces(tl.getText(), longestTLToken));
            for (Token sl : this.sentenceSL.getTokens()) {
                if (this.aligments.contains(sl, tl)) {
                    out.append(separatir + StringHelper.centerInWhitespaces("X", sl.getText().length()) + " ");
                } else out.append(separatir + StringHelper.addWhitespaces("", sl.getText().length() + 1));

            }
            out.append("\n");
        }
        return out.toString();

    }


    /**
     * Write output semantic frames into a csv file
     */
    ArrayList<JSONObject> getOutputJson(Language language) {
        ArrayList<JSONObject> jsonLst = new ArrayList<>();
        JSONParser parser = new JSONParser();
        Sentence sentence = null;
        if (language.equals(Language.SINHALA)) {
            sentence = this.sentenceTL;
        } else if (language.equals(Language.ENGLISH)) {
            sentence = this.sentenceSL;
        }
        try {
            assert sentence != null;
            for (Token tl : sentence.getTokens()) {
                Map<String, String> tokenJsonObj = new HashMap<>();
                ArrayList<String> frameLst = new ArrayList<>();
                for (Frame frame : sentence.getFrames()) {
                    if (frame.hasTokenRole(tl)) {
                        if (tl.evokesFrame()) {
                            String roleLabel = frame.getTokenRole(tl);
                            if (roleLabel.equals("B-V")) {
                                tokenJsonObj.put("text", tl.getText());
                                tokenJsonObj.put("frame", tl.getFrame().getLabel());
                                if (!frameLst.contains(tl.getFrame().getLabel())) {          // Check whether frame label available to avoid repetition
                                    frameLst.add(tl.getFrame().getLabel());
                                }
                            } else {
                                tokenJsonObj.put("text", tl.getText());
                                tokenJsonObj.put("frame", frame.getTokenRole(tl));
                                frameLst.add(frame.getTokenRole(tl)); // Add tokenroles into list
                            }
                        } else {
                            if (frame.getTokenRole(tl) != null) {
                                tokenJsonObj.put("text", tl.getText());
                                tokenJsonObj.put("frame", frame.getTokenRole(tl));
                                frameLst.add(frame.getTokenRole(tl)); // Add tokenroles into list
                            } else {
                                tokenJsonObj.put("text", tl.getText());
                                tokenJsonObj.put("frame", tl.getFrame().getLabel());
                                if (!frameLst.contains(tl.getFrame().getLabel())) {          // Check whether frame label available to avoid repetition
                                    frameLst.add(tl.getFrame().getLabel());
                                }

                            }
                        }
//                    } else if (tl.evokesFrame()) {
//                        tokenJsonObj.put("text", tl.getText());
////                    tokenJsonObj.put("pos", tl.getPos());
//                        tokenJsonObj.put("frame", tl.getFrame().getLabel());
//                        if (!frameLst.contains(tl.getFrame().getLabel())) {          // Check whether frame label available to avoid repetition
//                            frameLst.add(tl.getFrame().getLabel());
//                        }
                    }
//                    } else {
//                            tokenJsonObj.put("text", tl.getText());
////                    tokenJsonObj.put("pos", tl.getPos());
//                            tokenJsonObj.put("frame", "_");
//                            frameLst.add("_");
//
//                    }
                }
                if (frameLst.size() != 0) {
                    tokenJsonObj.put("frame", frameLst.toString());         // Append Role list into jsonObject map
                }

                try {
                    jsonLst.add((JSONObject) parser.parse(JSONValue.toJSONString(tokenJsonObj)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            return processMissingTags(jsonLst);
//            return jsonLst;
        } catch (NullPointerException e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
            System.out.println("Invalid language definition!");
            return null;
        }
    }

    /**
     * Method to process missing tags
     * @param jsonLst json object list for tokens
     * @return processed json list
     */
    public ArrayList<JSONObject> processMissingTags(ArrayList<JSONObject> jsonLst) {
//    private void processMissingTags(ArrayList<JSONObject> jsonLst) {
        ArrayList<Integer> missingIndexes = new ArrayList<>();
        JSONParser parser = new JSONParser();

        for (JSONObject token : jsonLst) {
            if (token.get("frame").equals("[_]")) {
                missingIndexes.add(jsonLst.indexOf(token));
            }
        }

        List<List<Integer>> ranges = getConsecutiveRanges(missingIndexes);

        for (List<Integer> range:ranges){
            if (range.size()==0){
                continue;
            } else if(range.size()==1){
                Integer index = range.get(0);
                String prevTag = (String) jsonLst.get(index-1).get("frame");
                String afterTag = (String) jsonLst.get(index+1).get("frame");
                if (prevTag.equals(afterTag)){
                    String tags = prevTag.substring(0,prevTag.length());
                }
            } else {
                Integer startIndex = range.get(0);
                Integer endIndex = range.get(1);
                String prevTag = (String) jsonLst.get(startIndex-1).get("frame");
                String afterTag = (String) jsonLst.get(endIndex+1).get("frame");
                prevTag = prevTag.substring(prevTag.indexOf("-")+1,prevTag.length()-1);
                afterTag = afterTag.substring(afterTag.indexOf("-")+1,afterTag.length()-1);
                if (prevTag.equals(afterTag)){
                    for (int i = startIndex;i<=endIndex;i++){
                        JSONObject tokenObject = jsonLst.get(i);
                        Map<String, String> tokenJsonObj = new HashMap<>();
                        tokenJsonObj.put("text", (String) tokenObject.get("text"));
                        tokenJsonObj.put("frame", "[I-"+prevTag+"]");

                        try {
                            jsonLst.add(i,(JSONObject) parser.parse(JSONValue.toJSONString(tokenJsonObj)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
        return jsonLst;
    }

    /**
     * Method to find consecutive index ranges
     * @param a integer list
     * @return list of lists containing ranges
     */
    static List<List<Integer>> getConsecutiveRanges ( ArrayList<Integer> a){
        int length = 1;
        List<List<Integer>> list = new ArrayList<List<Integer>>();
        // If the array is empty,
        // return the list
        if (a.size() == 0) {
            return list;
        }

        // Traverse the array from first position
        for (int i = 1; i <= a.size(); i++) {

            // Check the difference between the
            // current and the previous elements
            // If the difference doesn't equal to 1
            // just increment the length variable.
            if (i == a.size() || a.get(i) - a.get(i - 1) != 1) {

                // If the range contains
                // only one element.
                // add it into the list.
                if (length == 1) {
                    List<Integer> range = new ArrayList<>();
                    range.add(a.get(i - length));
                    list.add(range);
                } else {
                    List<Integer> range = new ArrayList<>();
                    // Build the range between the first
                    // element of the range and the
                    // current previous element as the
                    // last range.
                    range.add(a.get(i - length));
                    range.add(a.get(i - 1));
                    list.add(range);
                }

                // After finding the first range
                // initialize the length by 1 to
                // build the next range.
                length = 1;
            } else {
                length++;
            }
        }

        return list;
    }

//    public void toCSV() {
//        File file = new File("output.csv");
////        try {
////            Files.deleteIfExists(file.toPath());
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//        FileWriter fr = null;
//        BufferedWriter br = null;
//        try {
//            // to append to file, you need to initialize FileWriter using below constructor
//            fr = new FileWriter(file, true);
//            br = new BufferedWriter(fr);
//            for (Token tl : this.sentenceTL.getTokens()) {
//                if (tl.evokesFrame()) {
//                    br.write(tl.getText() + "," + tl.getPos()+"," + tl.getFrame().getLabel());
//                    br.newLine();
//                } else {
//                    for (Frame frame : this.sentenceTL.getFrames()) {
//                        if (frame.hasTokenRole(tl)) {
//                            br.write(tl.getText() + "," +tl.getPos()+"," + frame.getTokenRole(tl));
//                            br.newLine();
//                        }
//                        else {
//                            br.write(tl.getText() + "," + tl.getPos()+"," + "_");
//                            br.newLine();
//                        }
//                    }
//                }
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                assert br != null;
//                br.close();
//                fr.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    /**
     * private class for alignments
     */
    private class Alignment {
        Alignment(Token sl, Token tl) {
            this.sl = sl;
            this.tl = tl;
        }

        Token sl;
        Token tl;

        @Override
        public String toString() {
            return sl.getText() + " -- " + tl.getText();
        }
    }

}

