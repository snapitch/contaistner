package org.snapitch.contaistner;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;

public class ContainersFinalizerOnApplicationContextLoadingErrorTest {

    @Test
    public void onErrorApplicationContextLoading() {
        try {
            SpringApplication.run(Application.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SpringBootApplication
    public static class Application {

        // This injection fails and application context loading have an exception
        @Autowired
        private DataSource dataSource;

        public static void main(String[] args) {
            SpringApplication.run(ContainersInitializersWithStandardYamlsTest.Application.class, args);
        }
    }
}
