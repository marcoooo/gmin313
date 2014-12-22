/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.univ.montp2.master.ncbi.api;

import gov.nih.nlm.ncbi.www.soap.eutils.*;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleType;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author marcoooo
 */
public class ncbi {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ncbi.class);
    public static final String crawlResultDir = System.getProperty("user.home") + "/crawl/ncbi/";

    public static final String workingDir = System.getProperty("user.home") + "/";

    public static void getDataFromNcbi() throws Exception {
        List<PubmedArticleType> articles = new PubmedSearch().search("electronic cigarettes");
        System.out.println("Found article " + articles.size());
        java.io.File theFile = new java.io.File(workingDir + "/output/ncbi/");

        try {
            String strLine;
            for (int i = 1; i < articles.size(); i++) {
                PubmedArticleType article = articles.get(i);
                FileWriter fw = new FileWriter(theFile.getAbsolutePath() + "/" + i + ".txt");
                System.out.println("File Name " + theFile.getAbsolutePath() + "/" + article.getPubmedData().getArticleIdList().getArticleId()[0].getString());
                try {
                    EFetchPubmedServiceStub.AbstractTextType[] content = article.getMedlineCitation().getArticle().getAbstract().getAbstractText();
                    for (EFetchPubmedServiceStub.AbstractTextType line : content) {
                        strLine = line.getString();
                        System.out.println("current Abstract " + strLine);
                        fw.write(strLine + "\n");
                    }
                    fw.close();
                } catch (NullPointerException ex) {
                }
            }
            LOG.info("Done");
        } catch (Exception e) {
            System.out.println("exception ");
            LOG.error("Exception " + e.getMessage());
        }
    }

    public static List<String> getListClassifier(String fileName) {
        FileReader fileReader;
        List<String> lines = new ArrayList<>();
        try {
            fileReader = new FileReader(workingDir + "/classifier/" + fileName);
            try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line.trim());
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(ncbi.class.getName()).log(Level.SEVERE, null, ex);
        }

        return lines;
    }

    public static Instances getDataSet(String directoryPath) {
        Charset charset = Charset.defaultCharset();
        List<String> stringList;
        try {
            FastVector atts = new FastVector(2);
            atts.addElement(new Attribute("content", (FastVector) null));
            FastVector classes = new FastVector(3);
            classes.addElement("positive");
            classes.addElement("negative");
            classes.addElement("neutre");
            atts.addElement(new Attribute("class", classes));

            Instances data = new Instances("text_files_in_" + directoryPath, atts, 0);

            File dir = new File(directoryPath);
            List<String> stopWords = getListClassifier("stopwordsenglish.txt");
            String[] files = dir.list();
            for (String file : files) {
                if (file.endsWith(".txt") && file.length() > 0) {
                    try {
                        stringList = Files.readAllLines(new File(directoryPath + "/" + file).toPath(), charset);
                        String[] stringArray = stringList.toArray(new String[]{});
                        String strValue = "";
                        double[] newInst = new double[2];
                        for (int j = 0; j < stringArray.length - 1; j++) {
                            String replaceAll = stringArray[j].toLowerCase().replaceAll("[^a-zàáâãäåçèéêëìíîïðòóôõöùúûüýÿ]", " ");
                            StringTokenizer tokenizer = new StringTokenizer(replaceAll, " ");
                            String token;
                            while (tokenizer.hasMoreTokens()) {
                                token = tokenizer.nextToken();
                                if (!stopWords.contains(token.toLowerCase())) {
                                    strValue += token.toLowerCase() + " ";
                                }

                            }
                        }
                        newInst[0] = (double) data.attribute(0).addStringValue(strValue);
                        String opinion = "negative";
                        String strLine = stringArray[stringArray.length - 1];
                        if (strLine != null && !"".equals(strLine)) {
                            opinion = strLine.substring(strLine.indexOf('>') + 1);
                            System.out.println("strLine " + opinion);
                        }
                        newInst[1] = (double) data.attribute(1).indexOfValue(opinion);
                        data.add(new Instance(1.0, newInst));
                    } catch (Exception e) {
                        System.err.println("failed to convert file: " + directoryPath + File.separator + file + " Exception " + e.getMessage());
                    }
                }
            }
            return data;
        } catch (Exception ex) {
            Logger.getLogger(ncbi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //PropertyConfigurator.configure(ncbi.class.getResource("log4j.properties"));
        try {
            // TODO code application logic here
            getDataFromNcbi();
            Instances dataset = getDataSet(workingDir + "output/tagged/ncbi/");
            java.io.File theFile = new java.io.File(workingDir + "/output/taggedArticleNcbi.arff");
            System.out.println("Directory : " + theFile.getAbsolutePath());
            FileWriter fw = new FileWriter(theFile.getAbsolutePath());
            if (dataset != null) {
                try (BufferedWriter out = new BufferedWriter(fw)) {
                    out.write(dataset.toString());
                }
            } 
        } catch (Exception ex) {
            Logger.getLogger(ncbi.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
