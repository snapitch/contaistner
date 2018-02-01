package org.snapitch.contaistner;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

@Slf4j
public class ContainersFactorySpringApplicationRunListener implements SpringApplicationRunListener {

    private static final String PROPERTIES_PREFIX = "contaistner";
    private static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = "Docker bootstrap";
    private static final String GENERATED_PROPERTY_SOURCE_NAME = "Docker generated";
    private static final String APPLICATIVE_PROPERTY_SOURCE_NAME = "Docker applicative";

    public ContainersFactorySpringApplicationRunListener(SpringApplication application, String[] args) {

    }

    @Override
    public void starting() {

    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {

    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();

        ContaistnerProperties contaistnerProperties = getPropertiesFromBootstrapFile(context);
        if (!contaistnerProperties.getServices().isEmpty()) {
            startContainersAndGenerateProperties(contaistnerProperties,propertySources);
            loadApplicativeConfiguration(contaistnerProperties, propertySources);
        }
    }

    private ContaistnerProperties getPropertiesFromBootstrapFile(ConfigurableApplicationContext applicationContext) {

        try {
            ContaistnerProperties standardProperties = getDockerProperties(applicationContext);
            Resource bootstrapResource = standardProperties.getBootstrapResource();

            if(bootstrapResource.exists()) {
                applicationContext.getEnvironment().getPropertySources()
                        .addFirst(createYamlPropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME, bootstrapResource));

                // Reload docker properties in order to get properties from bootstrap resource
                return getDockerProperties(applicationContext);

            } else {
                // Return docker properties from standard properties
                return standardProperties;
            }

        } catch (Exception e) {
            throw new IllegalStateException("Impossible to load bootstrap configuration", e);
        }
    }

    private ContaistnerProperties getDockerProperties(ConfigurableApplicationContext applicationContext) {

        ContaistnerProperties properties = new ContaistnerProperties();
        properties.setApplicationContext(applicationContext);
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(properties, PROPERTIES_PREFIX);
        dataBinder.bind(new PropertySourcesPropertyValues(applicationContext.getEnvironment().getPropertySources()));
        return properties;
    }

    private PropertySource<?> createYamlPropertySource(String name, Resource yamlResource) throws IOException {
        PropertySource<?> propertySource = new YamlPropertySourceLoader().load(name, yamlResource, null);
        if(propertySource == null) {
            LOGGER.warn("Empty {}", yamlResource);
            propertySource = new MapPropertySource(name, new HashMap<>());
        }
        return propertySource;
    }

    private void startContainersAndGenerateProperties(ContaistnerProperties contaistnerProperties,
                                                      MutablePropertySources propertySources) {

        Properties generatedProperties = new Properties();

        for (String containerKey : contaistnerProperties.getServices().keySet()) {
            ContaistnerProperties.Service serviceProperties =
                    contaistnerProperties.getServices().get(containerKey);

            LOGGER.info("Run container {} with image {}", containerKey, serviceProperties.getImage());

            ContainerInfo containerInfo = startContainer(serviceProperties);
            addGeneratedProperties(generatedProperties, containerKey, containerInfo);

            LOGGER.info("Container {} is running and properties is overloaded", containerKey);
        }

        if (!generatedProperties.isEmpty()) {
            propertySources.addFirst(new PropertiesPropertySource(GENERATED_PROPERTY_SOURCE_NAME, generatedProperties));
        }
    }

    private ContainerInfo startContainer(ContaistnerProperties.Service serviceProperties) {
        try (Client client = new Client()) {
            return client.startContainer(serviceProperties);
        }
    }

    private void addGeneratedProperties(Properties properties,
                                        String containerKey,
                                        ContainerInfo containerInfo) {

        properties.put(PROPERTIES_PREFIX + ".services." + containerKey + ".id", containerInfo.id());
        properties.put(PROPERTIES_PREFIX + ".services." + containerKey + ".name", containerInfo.name());
        addGeneratedBindingPortProperties(properties, containerKey, containerInfo);
    }

    private void addGeneratedBindingPortProperties(Properties properties,
                                                   String containerKey,
                                                   ContainerInfo containerInfo) {

        ImmutableMap<String, List<PortBinding>> ports = containerInfo.networkSettings().ports();
        if(ports != null) {
            for (String port : ports.keySet()) {
                int bindingPort = getPort(ports.get(port));
                if(bindingPort != -1) {
                    properties.put(PROPERTIES_PREFIX + ".services." + containerKey + ".bindings." + port, bindingPort);
                }
            }
        }
    }

    private int getPort(List<PortBinding> portBindings) {
        if (portBindings != null && !portBindings.isEmpty()) {
            final PortBinding firstBinding = portBindings.get(0);
            return Integer.parseInt(firstBinding.hostPort());
        }

        return -1;
    }

    private void loadApplicativeConfiguration(ContaistnerProperties contaistnerProperties,
                                              MutablePropertySources propertySources) {

        Resource applicationResource = contaistnerProperties.getApplicationResource();
        if(applicationResource.exists()) {
            try {
                propertySources.addAfter(GENERATED_PROPERTY_SOURCE_NAME,
                        createYamlPropertySource(APPLICATIVE_PROPERTY_SOURCE_NAME, applicationResource));

            } catch (Exception e) {
                throw new IllegalStateException(
                        "Impossible to load applicative configuration from " + applicationResource.toString(), e);
            }
        }
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void finished(ConfigurableApplicationContext context, Throwable exception) {
        ContaistnerProperties properties = getDockerProperties(context);

        for (String containerKey : properties.getServices().keySet()) {
            ContaistnerProperties.Service serviceProperties = properties.getServices().get(containerKey);
            if(serviceProperties.getId() != null) {
                try (Client client = new Client()) {
                    LOGGER.info("Stop container {}" , containerKey);
                    client.stopContainer(serviceProperties.getId());
                }
            }
        }
    }
}
