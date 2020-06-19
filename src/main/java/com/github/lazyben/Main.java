package com.github.lazyben;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.ArrayList;
import java.util.List;

public class Main {
    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:/Users/lazyben/Projects/sina-crawler/news", "root", "root");
        while (true) {
            List<String> linkPool = loadLinksFromDatabase(connection, "select link from links_to_be_processed");

            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.get(0);
            upDateLinksToDatabase(connection, link, "delete from links_to_be_processed where link = ?");

            if (isAlreadyProcessed(connection, link)) {
                continue;
            }

            if (isInterestedLink(link)) {
                upDateLinksToDatabase(connection, link, "insert into links_already_processed (link) values (?)");
                System.out.println("link = " + link);
                Document doc = httpGetAndParseHtml(link);
                parseUrlAndInsertIntoDatabase(connection, doc);
                storeIntoDatabaseIfItIsNewsPage(doc);
            }
        }
    }

    private static void parseUrlAndInsertIntoDatabase(Connection connection, Document doc) throws SQLException {
        Elements aTags = doc.select("a");
        for (Element aTag : aTags) {
            upDateLinksToDatabase(connection, aTag.attr("href"), "insert into links_to_be_processed (link) values (?)");
        }
    }

    private static void upDateLinksToDatabase(Connection connection, String link, String s) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(s)) {
            preparedStatement.setString(1, link);
            preparedStatement.executeUpdate();
        }
    }

    private static boolean isAlreadyProcessed(Connection connection, String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("select link from links_already_processed where link = ?")) {
            preparedStatement.setString(1, link);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
    }

    private static List<String> loadLinksFromDatabase(Connection connection, String selectSql) throws SQLException {
        List<String> linkPool = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql); ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                linkPool.add(resultSet.getString(1));
            }
        }
        return linkPool;
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element article : articleTags) {
                System.out.println(article.child(0).text());
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
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

    private static boolean isInterestedLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) &&
                isNotLoginPage(link);
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }
}
