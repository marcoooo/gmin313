/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.univ.montp2.master.gmin313;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.core.*;

/**
 *
 * @author marcoooo
 */
public class DataCrawler {

    public static final String crawlResultDir = System.getProperty("user.home") + "/crawl/";

    public static final String workingDir = System.getProperty("user.home") + "/";

    public static void main(String[] args) {
        try {
            //crawlTwitter();
            File crawlDir = new File(crawlResultDir);
            delete(crawlDir);
            crawlDir.mkdir();
            MyCrawler crawler = new MyCrawler();
            crawler.crawlWebSites();
            Instances dataset = createDataset(crawlResultDir);
            java.io.File theFile = new java.io.File(workingDir + "/output/weka.arff");
            System.out.println("Directory : " + theFile.getAbsolutePath());
            FileWriter fw = null;
            fw = new FileWriter(theFile.getAbsolutePath());
            try (BufferedWriter out = new BufferedWriter(fw)) {
                out.write(dataset.toString());
            }
            //System.out.println(dataset)
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(DataCrawler.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void delete(File file)
            throws IOException {
        System.out.println("File to delete : "
                + file.getAbsolutePath());
        if (file.isDirectory()) {

            //directory is empty, then delete it
            if (file.list().length == 0) {

                file.delete();
                System.out.println("Directory is deleted : "
                        + file.getAbsolutePath());

            } else {

                //list all the directory contents
                String files[] = file.list();

                for (String temp : files) {
                    //construct the file structure
                    File fileDelete = new File(file, temp);

                    //recursive delete
                    delete(fileDelete);
                }

                //check the directory again, if empty then delete it
                if (file.list().length == 0) {
                    file.delete();
                    System.out.println("Directory is deleted : "
                            + file.getAbsolutePath());
                }
            }

        } else {
            //if file, then delete it
            file.delete();
            System.out.println("File is deleted : " + file.getAbsolutePath());
        }
    }

    public static List<String> getListClassifier(String fileName) {
        FileReader fileReader;
        List<String> lines = new ArrayList<String>();
        try {
            fileReader = new FileReader(workingDir + "/classifier/" + fileName);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line.trim());
            }
            bufferedReader.close();
        } catch (Exception ex) {
            Logger.getLogger(DataCrawler.class.getName()).log(Level.SEVERE, null, ex);
        }

        return lines;
    }

    public static Instances createDataset(String directoryPath) throws Exception {

        FastVector atts = new FastVector(4);
        atts.addElement(new Attribute("filename", (FastVector) null));
        atts.addElement(new Attribute("title", (FastVector) null));
        atts.addElement(new Attribute("content", (FastVector) null));
        FastVector classes = new FastVector(3);
        classes.addElement("positif");
        classes.addElement("negatif");
        classes.addElement("neutre");
        atts.addElement(new Attribute("class", classes));
        Instances data = new Instances("text_files_in_" + directoryPath, atts, 0);

        File dir = new File(directoryPath);
        List<String> stopWords = getListClassifier("stopw.txt");
        List<String> posWords = getListClassifier("GI_pos_sansNeutre.txt");
        List<String> negWords = getListClassifier("GI_neg_sansNeutre.txt");
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            if (files[i].endsWith(".txt") && files[i].length() > 0) {
                try {
                    double[] newInst = new double[4];
                    newInst[0] = (double) data.attribute(0).addStringValue(files[i]);
                    File txt = new File(directoryPath + File.separator + files[i]);
                    FileInputStream is = new FileInputStream(txt);
                    int c;
                    DataInputStream in = new DataInputStream(is);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String strLine, strValue = "";
                    int score_positif = 0;
                    int score_negatif = 0;
                    for (int j = 1; j < 3; j++) {
                        strLine = br.readLine();
                        System.out.println(strLine);
                        StringTokenizer tokenizer = new StringTokenizer(strLine, "  ,;'’:%?!");
                        String token;
                        while (tokenizer.hasMoreTokens()) {
                            token = tokenizer.nextToken();
                            //System.out.println("Current Token " + token);
                            if (!stopWords.contains(token.toLowerCase())) {
                                //System.out.println("Added Token " + token);
                                strValue += token.toLowerCase() + " ";
                            }
                            // valcul du score
                            // si positif score
                            if (posWords.contains(token.toLowerCase()))
                                score_positif++;
                            if (negWords.contains(token.toLowerCase()));
                                score_negatif++;
                        }
                        newInst[j] = (double) data.attribute(j).addStringValue(strValue);
                    }
                    if (score_positif > score_negatif) {
                        newInst[3] = (double) data.attribute(3).indexOfValue("positif");
                    } else if (score_positif < score_negatif) {
                        newInst[3] = (double) data.attribute(3).indexOfValue("negatif");
                    } else {
                        newInst[3] = (double) data.attribute(3).indexOfValue("neutre");
                    }
                    //newInst[1] = (double) data.attribute(1).addStringValue(txtStr.toString());
                    data.add(new Instance(1.0, newInst));
                } catch (Exception e) {
                    System.err.println("failed to convert file: " + directoryPath + File.separator + files[i]);
                }
            }
        }
        return data;
    }
}
