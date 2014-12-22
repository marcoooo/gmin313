/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.univ.montp2.master.gmin313;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import java.util.regex.Pattern;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author marcoooo
 */
public class MyCrawler extends WebCrawler {

    private static final String[] webSitesCrawlUrl = {
            "http://www.my-cigarette-electronique.com/blog/", 
            "http://ma-cigarette.fr/", 
            "http://www.alacigaretteelectronique.fr/blog",
           // "http://www.ecigarette-public.com/f113-1-sante-labo-tous-les-sujets"
    };

   
    
    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css\\??|js|bmp|gif|jpe?g"
            + "|png|tiff?|mid|mp2|mp3|mp4"
            + "|wav|avi|mov|mpeg|ram|m4v|pdf"
            + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");

    private static final Map<String, String[]> webSitesCrawl = new HashMap<String, String[]>() {
        {
            // Domain => String[] titleSelector, contentSelector, dateSelector, commentSelector
            put("my-cigarette-electronique.com", new String[]{".blog-view > h1", "#post_view > .rte > p", ".blog-view > p > span", null});
            put("ma-cigarette.fr", new String[]{".entry-title", ".entry-content p", ".post .p_date span", null});
            put("alacigaretteelectronique.fr", new String[]{"#center_column > h1", "#center_column > p:not(.info_blog)", "#center_column > p.info_blog", null});
            //put("www.ecigarette-public.com", new String[]{".page-title", ".post > div.inner", "p.author", null});
        }
    };

    /**
     * You should implement this function to specify whether the given url
     * should be crawled or not (based on your crawling logic).
     */
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getURL().toLowerCase();
        boolean toVisit = !FILTERS.matcher(href).matches()
                && (href.startsWith("http://www.my-cigarette-electronique.com/blog")
                || (href.startsWith("http://www.ma-cigarette.fr") && !href.contains("/avis-"))
                || href.startsWith("http://www.alacigaretteelectronique.fr")
                || (href.startsWith("http://www.ecigarette-public.com")));
        System.out.println("should Visit " + toVisit + " url " + href);
        return toVisit;
    }

    private String getTitleSelector(String domain) {
        return webSitesCrawl.get(domain)[0];
    }

    private String getContentSelector(String domain) {
        return webSitesCrawl.get(domain)[1];
    }

    private String getDateSelector(String domain) {
        return webSitesCrawl.get(domain)[2];
    }

    private String getCommentSelector(String domain) {
        return webSitesCrawl.get(domain)[3];
    }

    /**
     * This function is called when a page is fetched and ready to be processed
     * by your program.
     */
    @Override
    @SuppressWarnings("empty-statement")
    public void visit(Page page) {
        FileWriter fw = null;
        try {
            String url = page.getWebURL().getURL();
            if (page.getParseData() instanceof HtmlParseData) {
                System.out.println("URL: " + url);
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                Document document = Jsoup.parse(htmlParseData.getHtml());
                System.out.println("css " + page.getWebURL().getDomain());
                String domain = page.getWebURL().getDomain();
                String content = document.select(getContentSelector(domain)).text();
                String title = document.select(getTitleSelector(domain)).text();
                String date = document.select(getDateSelector(domain)).text();
                System.out.println("Title:" + title + "/Date : " + date);
                java.io.File theFile = new java.io.File(DataCrawler.crawlResultDir + "/" + domain + page.getWebURL().getPath().replace("/", "_") + ".txt");
                System.out.println("Directory : " + theFile.getAbsolutePath());
                if (!theFile.exists() || theFile.length() == 0) {
                    fw = new FileWriter(theFile.getAbsolutePath());
                    if (!"".equals(content)) {
                        try (BufferedWriter out = new BufferedWriter(fw)) {
                            out.write("<title>" + title + "\n");
                            out.write("<content>" + content + "\n");
                            out.write("\n");
                        }
                    }
                }
            }
            fw.close();
        } catch (IOException ex) {
            System.out.println("IO Exception : " + ex.getMessage());
            Logger.getLogger(MyCrawler.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            
        }
    }

    protected void crawlWebSites() throws Exception {
        // crawl specified website
        String crawlStorageFolder = "data/crawl/root";
        int numberOfCrawlers = 7;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);

        /*
         * Instantiate the controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        /*
         * For each crawl, you need to add some seed urls. These are the first
         * URLs that are fetched and then the crawler starts following links
         * which are found in these pages
         */
        
        for (String webSitesCrawl1 : webSitesCrawlUrl) {
            controller.addSeed(webSitesCrawl1);
        }
        controller.start(MyCrawler.class, numberOfCrawlers);
    }
}
