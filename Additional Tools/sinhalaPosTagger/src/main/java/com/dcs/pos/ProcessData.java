package com.dcs.pos;


import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ProcessData {
    public void processSentence(String sentence){
        String[] wordList = sentence.split(" ");
        File file = new File("~/sinhalaPosTagger/tag.txt");
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileWriter fr = null;
        BufferedWriter br = null;
        try {
            // to append to file, you need to initialize FileWriter using below constructor
            fr = new FileWriter(file, true);
            br = new BufferedWriter(fr);
            for (String aWordList : wordList) {
                // you can use write or append method
                br.write(aWordList.replaceAll("^[ \t]+|[ \t]+$", ""));
                br.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert br != null;
                br.close();
                fr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, String> getTaggings(){
        String s;
        Process p;
        Map<String, String> taggedDataMap=new HashMap<String, String>();
//            StringBuilder output = new StringBuilder();

        try {
            p = Runtime.getRuntime().exec("tnt ./tnt/models/sinhala_final ./tag.txt");
            BufferedReader bRInput = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));

            BufferedReader bRError = new BufferedReader(
                    new InputStreamReader(p.getErrorStream()));

            // Read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = bRError.readLine()) != null) {
                System.out.println(s);
            }

            while ((s = bRInput.readLine()) != null) {
                if (!s.startsWith("%")) {
                    ArrayList<String> wordsPos = new ArrayList<>();
                    for (String word : s.split("\t")) {
                        word = word.replaceAll("^[ \t]+|[ \t]+$", "");
                        if (!word.equals("")) {
                            wordsPos.add(word);
                        }
                    }
                    // this is the text of the token
                    String word = wordsPos.get(0);
                    String pos;
                    // this is the POS tag of the token
                    if (wordsPos.get(1).equals("NNC") || wordsPos.get(1).equals("NNP")) {
                        pos = "NN";
                    } else if (wordsPos.get(1).equals("VP")) {
                        pos = "VBP";
                    } else {
                        pos = wordsPos.get(1);
                    }
                    System.out.println(word+" "+pos);
                    taggedDataMap.put(word,pos);
                }
            }

        }catch (IOException e) {
            e.printStackTrace();
        }
        return taggedDataMap;
    }
}
