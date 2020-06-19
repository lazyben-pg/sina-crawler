package com.github.lazyben;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.util.stream.Collectors;

public class Crawler {
    JdbcCrawlerDao dao = new JdbcCrawlerDao();

    public static void main(String[] args) throws IOException, SQLException {
        new Crawler().run();
    }

    public void run() throws SQLException, IOException {
        String link;
        while ((link = dao.getNextLinkAndDelete()) != null) {
            if (dao.isAlreadyProcessed(link)) {
                continue;
            }

            if (isInterestedLink(link)) {
                dao.insertLinkIntoProcessedPool(link);
                System.out.println("link = " + link);
                Document doc = httpGetAndParseHtml(link);
                parseUrlAndInsertNewLinksIntoDatabase(doc);
                storeIntoDatabaseIfItIsNewsPage(doc, link);
            }
        }
    }

    private void parseUrlAndInsertNewLinksIntoDatabase(Document doc) throws SQLException {
        Elements aTags = doc.select("a");
        for (Element aTag : aTags) {
            dao.insertLinkIntoLinksPool(aTag.attr("href"));
        }
    }


    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element article : articleTags) {
                String title = article.child(0).text();
                String content = article.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                System.out.println(title);
                dao.insertNewsIntoDatabase(link, title, content);
            }
        }
    }

    private Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Safari/537.36");
        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();
            return Jsoup.parse(EntityUtils.toString(entity1));
        }
    }

    private boolean isInterestedLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) &&
                isNotLoginPage(link);
    }

    private boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }
}
