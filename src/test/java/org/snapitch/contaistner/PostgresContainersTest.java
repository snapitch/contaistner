package org.snapitch.contaistner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = PostgresContainersTest.Application.class)
@ActiveProfiles("postgres")
public class PostgresContainersTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void executeSql() {
        jdbcTemplate.execute("SELECT 1");
    }

    @SpringBootApplication
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(PostgresContainersTest.Application.class, args);
        }
    }
}
