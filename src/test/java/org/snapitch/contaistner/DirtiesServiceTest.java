package org.snapitch.contaistner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.snapitch.contaistner.ContaistnerPropertiesFactory.createFromApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DirtiesServiceTest.Application.class)
@ActiveProfiles("dirties-service")
public class DirtiesServiceTest {

    private static String containerId = null;
    private static String containerName = null;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Test
    @DirtiesService("redis-it")
    public void test1() {
        checkServiceIdAndNameChange();
    }

    @Test
    @DirtiesService("redis-it")
    public void test2() {
        checkServiceIdAndNameChange();
    }

    private void checkServiceIdAndNameChange() {
        ContaistnerProperties properties = createFromApplicationContext(applicationContext);
        if(containerId != null) {
            assertThat(properties.getServices().get("redis-it").getId(), not(containerId));
            assertThat(properties.getServices().get("redis-it").getName(), not(containerName));
        } else {
            containerId = properties.getServices().get("redis-it").getId();
            containerName = properties.getServices().get("redis-it").getName();
        }
    }

    @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(DirtiesServiceTest.Application.class, args);
        }
    }
}
