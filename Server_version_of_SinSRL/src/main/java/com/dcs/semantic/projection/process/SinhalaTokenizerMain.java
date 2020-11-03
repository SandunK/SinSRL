package com.dcs.semantic.projection.process;

import org.springframework.stereotype.Controller;

import java.io.*;
import java.util.LinkedList;

/* Class to tokenize files using hte SinhalaTokenizer class
*/
@Controller
public class SinhalaTokenizerMain {

//    private static final String proDirPath= "/home/aloka/smt/si-en-kyoto/";
//    private static final String dataInDir= "corpus/";
//    private static final String prefix= "set7-";
//    private static final String dateString="19-07-09";
//    private static final String postfix= "-cl-1";
//    private static final String dataOutDir= "corpus/";

//    private static final String inputF = "/home/aloka/trilingual_corpus/si-en-corpus-preparation/2.pre-processing-25.05.2020/28.05.2020/ann-28.05.2020.si-en.si";
//    private static final String inputF = "D:/Projects/WordAlign/sats/corpus/training/dev.si";
//    private static final String outputF = "/home/aloka/trilingual_corpus/si-en-corpus-preparation/2.pre-processing-25.05.2020/28.05.2020/ann-28.05.2020.tok.si-en.si";
//    private static final String outputF = "D:/Projects/WordAlign/sats/corpus/training/dev.tok.si";

    String tokenize(String sentence) {

      //String[]  fileSet ={"let0-", "let1-", "let2-", "let3-", "let4-", "let5-","extract-"};
        String suffix = ".si-en.si";
        String tokSuffix =  ".tok";

        //String fileIndex = args[0];
        //int fileIndex = Integer.parseInt(args[0]);
        /*
        File fileIn = new File(inputF+suffix);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileIn));
        File fileOut = new File(outputF+ tokSuffix + suffix);
        fileOut.getParentFile().mkdirs();
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileOut));
        */
//        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath)));
//        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath+".tok")));
//
        SinhalaTokenizer tamilTokenizer = new SinhalaTokenizer();
//
//        String line = bufferedReader.readLine().trim();

//        while (sentence != null) {

            LinkedList<String> tokenized = tamilTokenizer.splitWords(sentence);

            StringBuilder str = new StringBuilder();
            for (String term : tokenized) {
                str.append(term).append(" ");
            }

            str = new StringBuilder(str.toString().trim());

            String[] parts = str.toString().split("\\s+");

            StringBuilder lineToWrite = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                if (i == parts.length - 1) {
                    lineToWrite.append(parts[i].trim());
                    break;
                }

                if (parts[i].equalsIgnoreCase("/") || parts[i].equalsIgnoreCase(".")) {
                    if (i > 0 && isNumeric(parts[i - 1])) {
                        lineToWrite = new StringBuilder(lineToWrite.toString().trim());

                        if (i < parts.length - 1 && isNumeric(parts[i + 1]))
                            lineToWrite.append(parts[i]);
                        else
                            lineToWrite.append(parts[i]).append(" ");
                    } else {
                        lineToWrite.append(parts[i]).append(" ");
                    }
                } else {
                    lineToWrite.append(parts[i]).append(" ");
                }

            }



            return lineToWrite.toString();


//            line = bufferedReader.readLine();
//
//            if (line != null)
//                line = line.trim();
//        }

//        bufferedReader.close();
//        bufferedWriter.close();
    }

    private static boolean isNumeric(String str) {
        try {
            Integer integer = Integer.parseInt(str);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
