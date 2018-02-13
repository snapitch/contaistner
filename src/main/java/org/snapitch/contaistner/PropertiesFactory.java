package org.snapitch.contaistner;

import lombok.extern.slf4j.Slf4j;
import org.snapitch.contaistner.configuration.ContaistnerProperties;
import org.snapitch.contaistner.configuration.ContaistnerProperties.ServiceProperties;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class PropertiesFactory {

    public static final String PROPERTIES_PREFIX = "contaistner";
    private static final String GENERATED_PROPERTY_SOURCE_NAME = "Docker generated";
    private static final String APPLICATIVE_PROPERTY_SOURCE_NAME = "Docker applicative";
    private static final ConcurrentMap<ConfigurableApplicationContext, ContaistnerProperties> PROPERTIES = new ConcurrentHashMap<>();

    public static ContaistnerProperties getForApplicationContext(ConfigurableApplicationContext applicationContext) {
        return PROPERTIES.computeIfAbsent(applicationContext, PropertiesFactory::loadPropertiesFunction);
    }

    public static void loadGeneratedProperties(ConfigurableApplicationContext applicationContext) {

        Map<String, Object> generatedProperties = ServiceContext.getFor(applicationContext).getAllServices().stream()
                .map(s -> s.getGeneratedProperties().entrySet())
                .flatMap(Collection::stream)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        PropertySource<?> propertySource = applicationContext.getEnvironment().getPropertySources()
                .get(GENERATED_PROPERTY_SOURCE_NAME);

        if (propertySource == null) {
            applicationContext.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource(GENERATED_PROPERTY_SOURCE_NAME, generatedProperties));

        } else {
            throw new IllegalStateException("Contaistner generated properties must be of type MapPropertySource");
        }

        PROPERTIES.put(applicationContext, loadPropertiesFunction(applicationContext));
    }

    private static ContaistnerProperties loadPropertiesFunction(ConfigurableApplicationContext applicationContext) {
        ContaistnerProperties properties = new ContaistnerProperties();
        properties.setApplicationContext(applicationContext);
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(properties, PROPERTIES_PREFIX);
        dataBinder.bind(new PropertySourcesPropertyValues(applicationContext.getEnvironment().getPropertySources()));
        return properties;
    }

    public static ServiceProperties getForService(ConfigurableApplicationContext applicationContext,
                                                  String serviceName) {

        return getForApplicationContext(applicationContext).getServices().get(serviceName);
    }

    public static void loadApplicativeConfiguration(ConfigurableApplicationContext applicationContext) {
        ContaistnerProperties properties = PropertiesFactory.getForApplicationContext(applicationContext);

        Resource applicationResource = properties.getApplicationResource();
        if (applicationResource.exists()) {
            try {
                applicationContext.getEnvironment().getPropertySources().addFirst(
                        createYamlPropertySource(APPLICATIVE_PROPERTY_SOURCE_NAME, applicationResource));

            } catch (Exception e) {
                throw new IllegalStateException(
                        "Impossible to load applicative configuration from " + applicationResource.toString(), e);
            }
        }
    }

    private static PropertySource<?> createYamlPropertySource(String name, Resource yamlResource) throws IOException {
        return new YamlPropertySourceLoader().load(name, yamlResource, null);
    }
}
