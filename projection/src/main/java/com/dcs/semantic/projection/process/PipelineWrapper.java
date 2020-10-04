package com.dcs.semantic.projection.process;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import is2.lemmatizer.Lemmatizer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
//import se.lth.cs.srl.SemanticRoleLabeler;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import zalando.analytics.base.*;
//import zalando.analytics.base.Language;

/**
 * Wrapper around several NLP libraries to support tokenization, lemmatization, PoS tagging, dependency parsing and
 * semantic role labeling for various languages. The Language needs to be passed to the constructor, then the pipeline
 * is automatically set up.
 * <p>
 * Created by Alan Akbik on 8/28/17.
 */

class PipelineWrapper {

    // Language of this pipeline, defaults to English.
    private Language language = Language.ENGLISH;

    // Anna lemmatizer for this pipeline (note: some languages such as Chinese do not have lemmatizers)
    private Lemmatizer lemmatizer;

    // Stanford CoreNLP object for most NLP preprocessing
    private StanfordCoreNLP pipeline = null;

    // MATE semantic role labeler for SRL in English
//    private SemanticRoleLabeler semanticRoleLabeler;

    private Logger logger = LogManager.getLogger(LanguageDAO.class);

    private String serverAddress = "35.224.254.129";

    /**
     * Constructor for pipeline object. Require language to be specified.
     *
     * @param language Target language for this pipeline.
     */
    PipelineWrapper(Language language) {

        this.language = language;
//        String languageString = language.toString().substring(0, 1).toUpperCase() + language.toString().substring(1).toLowerCase();

        //---------------------------------
        // StanfordNLP
        //---------------------------------
        Properties props = new Properties();

        // use NN parsers for non-English languages
        if (language.equals(Language.ENGLISH)) {
            props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
            props.setProperty("ssplit.isOneSentence", "true");

            // initialize STANFORD NLP
            pipeline = new StanfordCoreNLP(props);
        }
    }

    /**
     * Method to parse a plain text sentence with the full NLP pipeline.
     *
     * @param text Plain text sentence.
     * @return Parsed sentence.
     */
    Sentence parse(String text) {
        String sentencetoParse = text;
        if (text.trim().equals("")) {
            return new Sentence();
        }

        //For Languages other than sinhala
        if (!language.equals(Language.SINHALA)) {
            // create an empty Annotation just with the given text
            Annotation document = new Annotation(text);

            // run all Annotators on this text
            pipeline.annotate(document);

            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            for (CoreMap sentence : sentences) {

                Sentence parse = new Sentence();
                // traversing the words in the current sentence
                // a CoreLabel is a CoreMap with additional token-specific methods

                for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {

                    // this is the text of the token
                    String word = token.get(CoreAnnotations.TextAnnotation.class);

                    // this is the POS tag of the token
                    String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);

                    Token newtoken = parse.newToken().setText(word).setPos(pos);

                    // this is the lemma of the token
                    if (language.equals(Language.ENGLISH)) {
                        String lemma = token.get(CoreAnnotations.LemmaAnnotation.class);
                        newtoken.setLemma(lemma);

                        String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
                        if (!ne.equals("O"))
                            newtoken.setMisc("Ner=" + StringHelper.capitalizeFirst(ne));
                    }
                }

                // Determine universal dependencies from tagset
                toUniversalDependencies(parse);

                for (Token token : parse.getTokens()) {
                    if (token.getHeadId() == 0) token.setDeprel("root");
                }

                if (language.equals(Language.ENGLISH)) {

                    int size = parse.getHeadIds().size();
                    int[] heads = new int[size];
                    Integer[] temp = parse.getHeadIds().toArray(new Integer[size]);
                    for (int n = 0; n < size; ++n) {
                        heads[n] = temp[n];
                    }

                    sentencetoParse = parse.toSentence();
                    ArrayList<ArrayList<String>> predicates = this.getPredicates(sentencetoParse);
                    JSONObject englishSRL = this.getEnglishSRL(sentencetoParse); // get English SRL from AllenNLP
                    assert englishSRL != null;
                    ArrayList verbs = (ArrayList) englishSRL.get("verbs");
                    ArrayList words = (ArrayList) englishSRL.get("words");

                    assert predicates != null;
                    for (ArrayList<String> p : predicates) {
                        Frame frame = parse.getToken(words.indexOf(p.get(0))+1).addNewFrame(p.get(1));

                        for (Object verbObject : verbs) {
                            JSONObject verbJson = (JSONObject) verbObject;
                            String verb = (String) verbJson.get("verb");

                            if (verb.equals(p.get(0))) {
                                for (Token token : parse.getTokens()) {
                                    ArrayList tags = (ArrayList) verbJson.get("tags");
                                    String tag = (String) tags.get(words.indexOf(token.getText()));
                                    frame.addRole(new Role(tag, token, ""));
                                }
                            }
                        }
                    }
                }
                return parse;
            }
        } else {    // For sinhala language

//            String[] wordList = text.split(" ");
            Boolean isVerbIdentified = false;
            Sentence parse = new Sentence();
//            Map PosTagMap = this.getSinhalaPosTag(text);
            Map.Entry<List<String>, Map<String, String>> result = this.identifyCompoundVerbs(text);      // identify compound verbs in sentence
            ArrayList<String> tokenList = (ArrayList<String>) result.getKey();
            Map<String, String> posTagMap = result.getValue();

            for(Object pos: posTagMap.values()){
                String posStr = (String) pos;           // check whether sentence have a verb. (We assuemes sentence without verb in sinhala as sentences with be worbs in english)
                if (posStr.contains("V")){
                    isVerbIdentified = true;
                    break;
                }
            }

            if (!isVerbIdentified) {
                tokenList.add("*");
                posTagMap.put("*","VERB");
            }
            for (String word : tokenList) {
                String pos = posTagMap.get(word);
                if (pos.contains("V") && word.startsWith("නො")){
                    JSONObject splitterResult = this.getBaseWord(word);

                    if (splitterResult == null){
                        Token newtoken = parse.newToken().setText(word).setPos(pos);
                        newtoken.setLemma("-");
                    } else {
                        List debugList = (List) splitterResult.get("debug");
                        for (Object instance:debugList){
                            List listInstance = (List) instance;
                            String prefix = (String) listInstance.get(0);
                            String base = (String) listInstance.get(1);
                            if (prefix.equals("නො")){
                                Token newtoken = parse.newToken().setText(prefix).setPos("DT");
                                Token newtoken2 = parse.newToken().setText(base).setPos(pos);
                            }
                        }
                    }

                } else {
                    Token newtoken = parse.newToken().setText(word).setPos(pos);
                    JSONObject splitterResult = this.getBaseWord(word); // get base word eg:දරුවාට base:දරුවා
                    if (splitterResult == null){
                        newtoken.setLemma("-");
                    } else {
                        newtoken.setLemma((String) splitterResult.get("base"));
                    }

                }
                // Determine universal dependencies from tagset
                toUniversalDependencies(parse);

            }

            toUniversalDependencies(parse);
            for (Token token : parse.getTokens()) {
                if (token.getHeadId() == 0) token.setDeprel("root");
            }

            return parse;
        }

        return null;
    }

    /**
     * Get predicates from a sentence
     * @param sentence sentence to tag
     * @return predicat arraylist
     */
    private ArrayList<ArrayList<String>> getPredicates (String sentence){
        ArrayList<ArrayList<String>> predicates = new ArrayList<>();
        Properties props = this.loadPropFile();
//        String postUrl = "http://" + props.getProperty("server.Address") + ":3001/getpredicates";// put in your url
        String postUrl = "http://" + serverAddress + ":3001/getpredicates";// put in your url
        Map<String, String> obj = new HashMap<>();
        obj.put("word", sentence);
        String jsonText = JSONValue.toJSONString(obj);
        System.out.println("Requesting English predicates from flair...");
        try {
            JSONObject result = this.makeHttpPost(postUrl, jsonText);
            assert result != null;
            String sentenceStr = result.get("sentence").toString();     // Predicate tagged sentence
            List<String> words = Arrays.asList(sentenceStr.split(" "));
            // Filter predicates
            String pattern = "\\<(.+?)\\>";
//            String pattern = "(?<=\\<)(\\s*.*\\s*)(?=\\>)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(sentenceStr);
            while (m.find()) {
                ArrayList<String> predicateWithForms = new ArrayList<>();
                String predicate=m.group();
                System.out.println(predicate);
                String verb = words.get(words.indexOf(predicate)-1); // get actual word of the predicate
                predicateWithForms.add(verb);
                predicateWithForms.add(predicate.substring(1,predicate.length()-1)); // <predicate.01> --> predicate.01
                predicates.add(predicateWithForms);
            }
            return predicates;

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Function to identify the compound verbs of the sentence
     *
     * @param sentence Sinhala Sentence
     * @return pair object of wordlist, Map of tokens and their pos tags
     */
    private Map.Entry<List<String>, Map<String, String>> identifyCompoundVerbs(String sentence) {
        logger.info("Finding compound verbs");
        String str[] = sentence.split(" ");     // Whitespace tokenizing
        List<String> wordList = new ArrayList<>(Arrays.asList(str));
        wordList.removeIf(t -> t.equals("")); // remove spaces in the middle of a sentence
        Map<String, String> posTagMap = this.getSinhalaPosTag(sentence);        // Postags for each word in sentence
        List<String> verbTags = Arrays.asList("VNN", "VFM", "VBP", "VNF", "VP","NCV","JCV", "RPCV","RRPCV", "SVCV","VBZ");     // NCV Tag set to identify compound verbs
        ArrayList<String> compVerbWords = new ArrayList<>();        // Words that have above verb tags as pos
        ArrayList<String> compVerbs = new ArrayList<>();        // Identified compound verbs
        ArrayList<String> compVerbObject = new ArrayList<>();       // List to store words of each compound verb
        Map<String, String> pos2Word = new HashMap<>();     // Final pos to token map includes compound verbs
        boolean isPairIdentified = false;       // Var to notify whether comp verb is identified

        for (String word : wordList) {
            assert posTagMap != null;
            String pos = posTagMap.get(word);       // get words containing above tags
            if (verbTags.contains(pos)) {
                compVerbWords.add(word);
            }
        }

        if (compVerbWords.size() > 1) {
            for (int i = 0; i < compVerbWords.size() - 1; i++) {        // If has more than two words
                String firstWord = compVerbWords.get(i);
                String secondWord = compVerbWords.get(i + 1);
                int indexDiff = wordList.indexOf(secondWord) - wordList.indexOf(firstWord); // get index difference of two nearby words from original sentence
                if (indexDiff == 1) {
                    isPairIdentified = true;        // If difference =1 identify as nearby words (comp verbs here, logic: nearby words containing above tags are compound verbs)
                    if (i + 1 == compVerbWords.size() - 1) {
                        compVerbObject.add(firstWord);
                        compVerbObject.add(secondWord);     // if second word is the last add both
                        compVerbs.add(Joiner.on(" ").join(compVerbObject));

                        removeIdentifiedCompWordsAndPos(wordList, posTagMap, compVerbObject); // clear identified words and their pos from original arrays

                        isPairIdentified = false;
                    } else {
                        compVerbObject.add(firstWord);
                    }
                } else {
                    if (isPairIdentified) {     // if difference not 1 and pair identified before
                        compVerbObject.add(firstWord);
                        compVerbs.add(Joiner.on(" ").join(compVerbObject));

                        removeIdentifiedCompWordsAndPos(wordList, posTagMap, compVerbObject);

                        compVerbObject.clear();     // clear the list to get next compound word set
                        isPairIdentified = false;
                    }
                }
            }

            logger.info("Compound verbs found: " + compVerbs.toString());

            for (String compVerb : compVerbs) {
                wordList.set(wordList.indexOf(compVerb.split(" ")[0]), compVerb);    // replace normal word with compound verb in original list
            }

            for (String word : wordList) {
                assert posTagMap != null;
                if (compVerbs.contains(word)) {
                    pos2Word.put(word, "VERB");     // Add pos of compound verb as VERB
                } else {
                    pos2Word.put(word, posTagMap.get(word));
                }

            }

            return new AbstractMap.SimpleEntry<>(wordList, pos2Word);

        } else {
            for (String word : wordList) {
                pos2Word.put(word, posTagMap.get(word));     // Return as it is
            }
            return new AbstractMap.SimpleEntry<>(wordList,pos2Word);
        }
    }

    /**
     * Method to get English SRL from AllenNLP Toolkit
     * @param sentence sentence to annotate
     * @return  Json object of SRL
     */
    private JSONObject getEnglishSRL(String sentence) {
        String url = "https://demo.allennlp.org/api/semantic-role-labeling/predict";
        Map<String, String> obj = new HashMap<>();
        Map<String, ArrayList> resultMap = new HashMap<>();
        obj.put("sentence", sentence);
        String jsonText = JSONValue.toJSONString(obj);
        System.out.println("Requesting English SRL from AllenNLP");
        try {
            JSONObject result = this.makeHttpPost(url, jsonText);
            return result;

        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Function to remove identified compound verbs and their pos from originals
     *
     * @param wordList       Original word list
     * @param posTagMap      Original pos tag map
     * @param compVerbObject Compound word list
     */
    private void removeIdentifiedCompWordsAndPos(List<String> wordList, Map<String, String> posTagMap, ArrayList<String> compVerbObject) {
        for (int j = 1; j < compVerbObject.size(); j++) {
            wordList.remove(compVerbObject.get(j));
            assert posTagMap != null;
            int finalJ = j;
            posTagMap.keySet().removeIf(key -> key.equals(compVerbObject.get(finalJ)));
        }
    }

    /**
     * Method to make http post request with json object
     *
     * @param url Url to execute
     * @return JsonObject
     */
    private JSONObject makeHttpPost(String url, String jsonText) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        StringEntity postingString = null;//gson.tojson() converts your pojo to json
        try {
            postingString = new StringEntity(jsonText, "UTF-8");
            post.setEntity(postingString);
            post.setHeader("Content-type", "application/json");
            HttpResponse response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(EntityUtils.toString(entity, "UTF-8"));
        } catch (org.json.simple.parser.ParseException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get splitted word
     *
     * @param word original word
     */
    private JSONObject getBaseWord(String word) {

        Properties props = this.loadPropFile();
//        String postUrl = "http://" + props.getProperty("serverAddress") + ":3000/split";// put in your url
        String postUrl = "http://" + serverAddress + ":3000/split";// put in your url
        if (word.length() > 1) {
            Map<String, String> obj = new HashMap<>();
            obj.put("word", word);
            String jsonText = JSONValue.toJSONString(obj);
            System.out.println(jsonText);
            return this.makeHttpPost(postUrl, jsonText);
//            if (result != null) {
//                System.out.println((String) result.get("base"));
//                return (String) result.get("base");
//            }
        }
//        return "-";
        return null;
    }


    /**
     * Get sinhala postag
     *
     * @param text Sentence to get postag
     */
    private Map<String, String> getSinhalaPosTag(String text) {

        String str[] = text.split(" ");     // Whitespace tokenizing
        List<String> wordList = new ArrayList<>(Arrays.asList(str));
        wordList.removeIf(t -> t.equals("")); // remove spaces in the middle of a sentence
        Properties props = this.loadPropFile();
//        String postUrl = "http://" + props.getProperty("serverAddress") + ":3002/getpos";// put in your url
        String postUrl = "http://" + serverAddress + ":3002/getpos";// put in your url
        Map<String, String> postagMap = new HashMap<String, String>();
        String sentence = String.join(" ",wordList);
        Map<String, String> postJson = new HashMap<String, String>();
        postJson.put("sentence", sentence);
        String jsonText = JSONValue.toJSONString(postJson);         // Create POST request json string

        JSONObject result = this.makeHttpPost(postUrl, jsonText);    // Call http post request and get results

        if (result != null) {
            for (String word : wordList) {
                System.out.println(word);
                System.out.println((String) result.get(word));         // Put outputs into a map
                postagMap.put(word, (String) result.get(word));
            }
            return postagMap;
        } else {
            return null;
        }
    }

    /**
     * Load config.properties file for server details
     *
     * @return Properties object
     */
    private Properties loadPropFile() {
        String resourceName = "config.conf";      // property file to load server details
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream(resourceName)) {
            props.load(resourceStream);
            System.out.println("Successfully loaded 'config.properties' file...");
        } catch (IOException e) {
            System.out.println("Error loading 'config.properties' file...");
        }
        return props;
    }

    /**
     * Helper method to format sentence for SRL (MATE SRL somehow needs the root field as textual string in array).
     *
     * @return Fields as string with extra root field.
     */
    private String[] prepareFields(List<String> fields) {

        List<String> input = Lists.newArrayList("<root>");
        input.addAll(fields);

        int size = input.size();
        String[] out = new String[size];
        String[] temp = input.toArray(new String[size]);
        System.arraycopy(temp, 0, out, 0, size);

        return out;
    }

    /**
     * Add universal dependencies to a sentence from language-specific dependencies.
     *
     * @param sentence Sentence for which universal dependencies are produced
     * @return True if sentence fully converted.
     */
    private static boolean toUniversalDependencies(Sentence sentence) {

        for (Token token : sentence.getTokens()) {

            if (token.getPos().startsWith("v")) token.setPosUniversal("VERB");
            if (token.getPos().startsWith("n")) token.setPosUniversal("NOUN");
            if (token.getPos().startsWith("pp")) token.setPosUniversal("PRON");
            if (token.getPos().startsWith("r")) token.setPosUniversal("ADV");
            if (token.getPos().startsWith("s")) token.setPosUniversal("PREP");
            if (token.getPos().startsWith("pr")) token.setPosUniversal("PREP");

            if (token.getPos().equals("NFP")) token.setPosUniversal("SYM");

            if (token.getPos().equals("#")) token.setPosUniversal("NOUN");

            if (token.getPos().startsWith("V")) token.setPosUniversal("VERB");

            if (token.getPos().startsWith("NNP")) token.setPosUniversal("PROPN");

            if (token.getPos().startsWith("DT")) token.setPosUniversal("DET");
            if (token.getPos().startsWith("IN")) token.setPosUniversal("ADP");
            if (token.getPos().equals("NN")) token.setPosUniversal("NOUN");
            if (token.getPos().equals("NNC")) token.setPosUniversal("NOUN");
            if (token.getPos().equals("NNS")) token.setPosUniversal("NOUN");
            if (token.getPos().startsWith("JJ")) token.setPosUniversal("ADJ");
            if (token.getPos().startsWith("PRP")) token.setPosUniversal("PRON");
            if (token.getPos().equals("MD")) token.setPosUniversal("AUX");
            if (token.getPos().equals("CD")) token.setPosUniversal("NUM");
            if (token.getPos().equals("CC")) token.setPosUniversal("CCONJ");

            if (token.getPos().equals("TO")) token.setPosUniversal("ADP");
            if (token.getPos().equals("TO") && token.getDeprel().equals("mark")) token.setPosUniversal("PART");
            if (token.getPos().equals("TO") && token.getDeprel().equals("xcomp")) token.setPosUniversal("PART");
            if (token.getPos().equals("TO") && token.getDeprel().equals("ccomp")) token.setPosUniversal("PART");
            if (token.getPos().equals("TO") && token.getDeprel().equals("advcl")) token.setPosUniversal("PART");
            if (token.getPos().equals("TO") && token.getDeprel().equals("case")) token.setPosUniversal("ADP");
            if (token.getPos().equals("TO") && token.getDeprel().equals("dep")) token.setPosUniversal("ADP");
            if (token.getPos().equals("TO") && token.getDeprel().equals("mwe")) token.setPosUniversal("ADP");
            if (token.getPos().equals("TO") && token.getDeprel().equals("nsubj")) token.setPosUniversal("ADP");
            if (token.getPos().equals("TO") && token.getDeprel().equals("conj")) token.setPosUniversal("ADP");

            if (token.getPos().equals("RB")) token.setPosUniversal("ADV");
            if (token.getPos().equals("RB") && token.getLemma().equals("not")) token.setPosUniversal("PART");

            if (token.getPos().equals("POS")) token.setPosUniversal("PART");
            if (token.getPos().startsWith("WP")) token.setPosUniversal("PRON");
            if (token.getPos().equals("WRB")) token.setPosUniversal("ADV");
            if (token.getPos().equals("WDT")) token.setPosUniversal("PRON");
            if (token.getPos().equals("EX")) token.setPosUniversal("PRON");
            if (token.getPos().equals("PDT")) token.setPosUniversal("DET");
            if (token.getPos().equals("RBS")) token.setPosUniversal("ADV");
            if (token.getPos().equals("RBR")) token.setPosUniversal("ADV");
            if (token.getPos().equals("RP")) token.setPosUniversal("ADP");
            if (token.getPos().equals("UH")) token.setPosUniversal("INTJ");
            if (token.getPos().equals("AFX")) token.setPosUniversal("X");
            if (token.getPos().equals("FW")) token.setPosUniversal("X");
            if (token.getPos().equals("LS")) token.setPosUniversal("X");
            if (token.getPos().equals("ADD")) token.setPosUniversal("X");
            if (token.getPos().equals("XX")) token.setPosUniversal("X");

            if (token.getPos().equals(".")) token.setPosUniversal("PUNCT");
            if (token.getPos().equals(",")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("?")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("!")) token.setPosUniversal("PUNCT");
            if (token.getText().equals(";")) token.setPosUniversal("PUNCT");
            if (token.getPos().equals(":")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("``")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("''")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("--")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("-")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("`")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("'")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("...")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("..")) token.setPosUniversal("PUNCT");
            if (token.getPos().matches("[\\.']+")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("\"")) token.setPosUniversal("PUNCT");
            if (token.getPos().equals("-LRB-")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("-LCB-")) token.setPosUniversal("PUNCT");
            if (token.getPos().equals("-RRB-")) token.setPosUniversal("PUNCT");
            if (token.getText().equals("-RCB-")) token.setPosUniversal("PUNCT");
            if (token.getPos().equals("HYPH")) token.setPosUniversal("PUNCT");

            if (token.getPos().equals("PUNCT")) token.setDeprel("punct");

            if (token.getPos().equals("$")) token.setPosUniversal("SYM");

            if (token.getPosUniversal().trim().equals("")) {
                System.out.println("token = " + token);
                token.setPosUniversal(token.getPos());
            }
        }
        return false;
    }
}
