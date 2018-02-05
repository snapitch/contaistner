package org.snapitch.contaistner;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ContainersInitializerWithNoStandardYamlsTest.Application.class)
@ActiveProfiles("unknown-application-file")
public class ContainersInitializerWithNoStandardYamlsTest {

    @Autowired
    private PropertyResolver propertyResolver;

    @Test
    public void addGeneratedPropertiesToEnvironment() {
        assertProperty("contaistner.services.redis-it.id", notNullValue());
        assertProperty("contaistner.services.redis-it.name", notNullValue());
        assertProperty("contaistner.services.redis-it.bindings.6379/tcp", notNullValue());
    }

    private void assertProperty(String property, Matcher<Object> matcher) {
        assertThat("Property " + property , propertyResolver.getProperty(property), matcher);
    }

    @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(ContainersInitializersWithStandardYamlsTest.Application.class, args);
        }
    }
}
