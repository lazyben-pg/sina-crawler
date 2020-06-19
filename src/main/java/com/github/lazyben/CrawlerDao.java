package com.github.lazyben;

import java.sql.SQLException;

public interface CrawlerDao {
    boolean isAlreadyProcessed(String link) throws SQLException;

    String getNextLink() throws SQLException;

    String getNextLinkAndDelete() throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    void insertLinkIntoLinksPool(String href) throws SQLException;

    void insertLinkIntoProcessedPool(String link) throws SQLException;
}
