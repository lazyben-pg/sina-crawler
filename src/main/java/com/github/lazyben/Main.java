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
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        List<String> linkPool = new ArrayList<>();
        List<String> processedLinkPool = new ArrayList<>();
        linkPool.add("https://sina.cn");
        while (!linkPool.isEmpty()) {
            String link = linkPool.remove(linkPool.size() - 1);

            if (processedLinkPool.contains(link)) {
                continue;
            }

            if (!link.contains("passport") && link.contains("news.sina.cn") || link.equals("https://sina.cn")) {
                processedLinkPool.add(link);
                System.out.println("link = " + link);
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpGet httpGet = new HttpGet(link);
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.106 Safari/537.36");
                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                    System.out.println(response1.getStatusLine());
                    HttpEntity entity1 = response1.getEntity();
                    Document doc = Jsoup.parse(EntityUtils.toString(entity1));

                    Elements aTags = doc.select("a");
                    for (Element aTag : aTags) {
                        linkPool.add(aTag.attr("href"));
                    }

                    Elements articleTags = doc.select("article");
                    if (!articleTags.isEmpty()) {
                        for (Element article : articleTags) {
                            System.out.println(article.child(0).text());
                        }
                    }
                }
            }
        }
    }
}
