package org.snapitch.contaistner;

import com.spotify.docker.client.messages.ContainerInfo;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;
import static org.snapitch.contaistner.ContaistnerPropertiesFactory.PROPERTIES_PREFIX;

/**
 * {@link org.springframework.test.context.TestExecutionListener} that manage {@link DirtiesService} annotations.
 */
public class DirtiesContainerTestExecutionListener extends AbstractTestExecutionListener {

    private List<String> dirtiesContainers = EMPTY_LIST;

    @Override
    public void beforeTestMethod(TestContext testContext) {
        if(!dirtiesContainers.isEmpty()) {
            ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) testContext.getApplicationContext();
            ContaistnerProperties contaistnerProperties = ContaistnerPropertiesFactory
                    .createFromApplicationContext(applicationContext);

            for (String dirtiesContainer : dirtiesContainers) {
                ContaistnerProperties.Service properties = contaistnerProperties.getServices().get(dirtiesContainer);
                if(properties != null) {
                    try(Client client = new Client()) {
                        client.stopContainer(properties.getId());
                        ContainerInfo containerInfo = client.startContainer(properties);
                        updateGeneratedProperties(applicationContext, containerInfo, dirtiesContainer);
                    }
                }
            }

            dirtiesContainers = EMPTY_LIST;
        }
    }

    private void updateGeneratedProperties(ConfigurableApplicationContext applicationContext, ContainerInfo containerInfo, String containerKey) {
        PropertiesPropertySource propertySource = (PropertiesPropertySource) applicationContext.getEnvironment()
                .getPropertySources().get(ContainersFactorySpringApplicationRunListener.GENERATED_PROPERTY_SOURCE_NAME);

        propertySource.getSource().put(PROPERTIES_PREFIX + ".services." + containerKey + ".id", containerInfo.id());
        propertySource.getSource().put(PROPERTIES_PREFIX + ".services." + containerKey + ".name", containerInfo.name());
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        DirtiesService annotation = AnnotatedElementUtils.findMergedAnnotation(testContext.getTestMethod(), DirtiesService.class);
        if(annotation != null) {
            dirtiesContainers = Arrays.asList(annotation.value());
        }
    }
}
