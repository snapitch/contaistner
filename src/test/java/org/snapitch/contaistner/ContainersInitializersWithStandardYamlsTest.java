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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ContainersInitializersWithStandardYamlsTest.Application.class)
@ActiveProfiles("standard-yaml")
public class ContainersInitializersWithStandardYamlsTest {

    @Autowired
    private PropertyResolver propertyResolver;

    @Test
    public void addBootstrapPropertiesToEnvironment() {
        assertProperty("contaistner.services.redis-it.image", is("redis:3.2.11"));
    }

    @Test
    public void addGeneratedPropertiesToEnvironment() {
        assertProperty("contaistner.services.redis-it.bindings.6379/tcp", notNullValue());
    }

    @Test
    public void addApplicationPropertiesWithGeneratedPropertiesReplacement() {
        assertProperty("my.property", is("localhost:" + propertyResolver.getProperty("contaistner.services.redis-it.bindings.6379/tcp")));
    }

    private void assertProperty(String property, Matcher<Object> matcher) {
        assertThat("Property " + property , propertyResolver.getProperty(property), matcher);
    }

    @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }
    }
}
