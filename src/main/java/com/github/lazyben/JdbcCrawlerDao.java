package com.github.lazyben;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class JdbcCrawlerDao implements CrawlerDao {
    private final Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public JdbcCrawlerDao() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:/Users/lazyben/Projects/sina-crawler/news", "root", "root");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void upDateDatabase(String link, String s) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(s)) {
            preparedStatement.setString(1, link);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public boolean isAlreadyProcessed(String link) throws SQLException {
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

    private String getNextLink() throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("select link from links_to_be_processed"); ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    @Override
    public String getNextLinkAndDelete() throws SQLException {
        String link = getNextLink();
        if (link == null) {
            return null;
        }
        upDateDatabase(link, "delete from links_to_be_processed where link = ?");
        return link;
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into news (title, content, url,created_at, modified_at) values(?,?,?,now(), now())")) {
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, content);
            preparedStatement.setString(3, link);
            preparedStatement.executeUpdate();
        }
    }

    @Override
    public void insertLinkIntoLinksPool(String href) throws SQLException {
        upDateDatabase(href, "insert into links_to_be_processed (link) values (?)");
    }

    @Override
    public void insertLinkIntoProcessedPool(String link) throws SQLException {
        upDateDatabase(link, "insert into links_already_processed (link) values (?)");
    }
}
