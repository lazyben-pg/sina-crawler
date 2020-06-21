package com.github.lazyben;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MybatisCrawlerDao implements CrawlerDao {
    private final SqlSessionFactory sqlSessionFactory;

    public MybatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isAlreadyProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne("countLink", link);
            return count != 0;
        }
    }

    @Override
    public synchronized String getNextLinkAndDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String link = (String) session.selectOne("selectNextLink");
            if (link != null) {
                session.delete("deleteLink", link);
            }
            return link;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("insertNews", new News(title, content, link));
        }
    }

    @Override
    public void insertLinkIntoLinksPool(String link) throws SQLException {
        chooseTableAndInsertLink(link, "LINKS_TO_BE_PROCESSED");
    }

    @Override
    public void insertLinkIntoProcessedPool(String link) throws SQLException {
        chooseTableAndInsertLink(link, "LINKS_ALREADY_PROCESSED");
    }

    private void chooseTableAndInsertLink(String link, String tableName) {
        Map<String, String> table = new HashMap<>();
        table.put("tableName", tableName);
        table.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("insertLink", table);
        }
    }
}
