package org.snapitch.contaistner;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = ContainersInitializerWithNoStandardYamlsTest.Application.class)
@TestPropertySource(properties = {
        "contaistner.bootstrap-file=bootstrap-unknown.yml",
        "contaistner.application-file=application-unknown.yml",
        "contaistner.services.redis-no-standard-yamls.image=redis:3.2.11",
        "contaistner.services.redis-no-standard-yamls.ports=6379"})
public class ContainersInitializerWithNoStandardYamlsTest {

    @Autowired
    private PropertyResolver propertyResolver;

    @Test
    public void addGeneratedPropertiesToEnvironment() {
        assertProperty("contaistner.services.redis-no-standard-yamls.id", notNullValue());
        assertProperty("contaistner.services.redis-no-standard-yamls.name", notNullValue());
        assertProperty("contaistner.services.redis-no-standard-yamls.bindings.6379/tcp", notNullValue());
    }

    @Test
    public void addApplicationPropertiesWithGeneratedPropertiesReplacement() {
        assertProperty("my.property", is("localhost:" + propertyResolver.getProperty("contaistner.services.redis-no-standard-yamls.bindings.6379/tcp")));
    }

    private void assertProperty(String property, Matcher<Object> matcher) {
        assertThat("Property " + property , propertyResolver.getProperty(property), matcher);
    }

    @SpringBootApplication
    @PropertySource("classpath:no-standard-yamls.properties")
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(ContainersInitializersWithStandardYamlsTest.Application.class, args);
        }
    }
}
