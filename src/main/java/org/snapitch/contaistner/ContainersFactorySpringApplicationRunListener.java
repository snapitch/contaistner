package org.snapitch.contaistner;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.*;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.snapitch.contaistner.ContaistnerPropertiesFactory.PROPERTIES_PREFIX;
import static org.snapitch.contaistner.ContaistnerPropertiesFactory.createFromApplicationContext;

@Slf4j
public class ContainersFactorySpringApplicationRunListener implements SpringApplicationRunListener {

    public static final String GENERATED_PROPERTY_SOURCE_NAME = "Docker generated";
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
        try {
            MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
            ContaistnerProperties properties = createFromApplicationContext(context);
            if (!properties.getServices().isEmpty()) {
                startContainersAndGenerateProperties(properties, propertySources);
                loadApplicativeConfiguration(properties, propertySources);
            }

            waitingContainersReadiness(context);

        } catch (Exception e) {
            throw new ContaistnerException("Impossible to start containers", e);
        }
    }

    private void waitingContainersReadiness(ConfigurableApplicationContext applicationContext) {
        // Reload properties in order to have generated ones
        ContaistnerProperties contaistnerProperties = createFromApplicationContext(applicationContext);

        for (Entry<String, ContaistnerProperties.Service> service : contaistnerProperties.getServices().entrySet()) {
            ContaistnerProperties.Service properties = service.getValue();

            waitingMinimumDelay(properties);
            waitingPortsAccessibility(service, properties);
            waitingLog(service, properties);
        }
    }

    private void waitingLog(Entry<String, ContaistnerProperties.Service> service, ContaistnerProperties.Service properties) {
        if (properties.getReadiness().getWaitingLogLine() != null) {
            LOGGER.info("Waiting log line '{}' for service {}", properties.getReadiness().getWaitingLogLine(), service.getKey());
            try (Client client = new Client()) {
                Thread thread = client.listenLogs(properties.getId(), isLogLinePresent(properties));
                await().atMost(properties.getReadiness().getMaxWaitingDelay(), SECONDS)
                        .until(() -> !thread.isAlive());

                LOGGER.info("Log line containing'{}' is found", properties.getReadiness().getWaitingLogLine());
            }
        }
    }

    private Consumer<String> isLogLinePresent(ContaistnerProperties.Service properties) {
        return logLine -> {
            if (logLine.contains(properties.getReadiness().getWaitingLogLine())) {
                throw new RuntimeException("Log line matching is found");
            }
        };
    }

    private void waitingPortsAccessibility(Entry<String, ContaistnerProperties.Service> service, ContaistnerProperties.Service properties) {
        for (Entry<String, String> bindings : properties.getBindings().entrySet()) {
            await().atMost(properties.getReadiness().getMaxWaitingDelay(), SECONDS)
                    .until(isPortAccessible(service.getKey(), parseInt(bindings.getValue())));
        }
    }

    private void waitingMinimumDelay(ContaistnerProperties.Service properties) {
        if (properties.getReadiness().getMinWaitingDelay() != 0) {
            try {
                Thread.sleep(properties.getReadiness().getMinWaitingDelay() * 1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private Callable<Boolean> isPortAccessible(String serviceName, int port) {
        return () -> {
            LOGGER.debug("Check port {} accessibility for service {}", port, serviceName);
            try (final Socket ignored = new Socket("localhost", port)) {
                return true;
            } catch (IOException e) {
                return false;
            }
        };
    }

    private PropertySource<?> createYamlPropertySource(String name, Resource yamlResource) throws IOException {
        PropertySource<?> propertySource = new YamlPropertySourceLoader().load(name, yamlResource, null);
        if (propertySource == null) {
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
        if (ports != null) {
            for (String port : ports.keySet()) {
                int bindingPort = getPort(ports.get(port));
                if (bindingPort != -1) {
                    properties.put(PROPERTIES_PREFIX + ".services." + containerKey + ".bindings." + port, bindingPort);
                }
            }
        }
    }

    private int getPort(List<PortBinding> portBindings) {
        if (portBindings != null && !portBindings.isEmpty()) {
            final PortBinding firstBinding = portBindings.get(0);
            return parseInt(firstBinding.hostPort());
        }

        return -1;
    }

    private void loadApplicativeConfiguration(ContaistnerProperties contaistnerProperties,
                                              MutablePropertySources propertySources) {

        Resource applicationResource = contaistnerProperties.getApplicationResource();
        if (applicationResource.exists()) {
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
        boolean isApplicationContextLoadingFails = exception != null;
        if (isApplicationContextLoadingFails) {
            new ContainersRemover(context).removeContainers();
        }
    }
}
