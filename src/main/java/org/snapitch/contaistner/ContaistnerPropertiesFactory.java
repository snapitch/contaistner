package org.snapitch.contaistner;

import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.context.ConfigurableApplicationContext;

public class ContaistnerPropertiesFactory {

    public static final String PROPERTIES_PREFIX = "contaistner";

    public static ContaistnerProperties createFromApplicationContext(ConfigurableApplicationContext applicationContext) {
        ContaistnerProperties properties = new ContaistnerProperties();
        properties.setApplicationContext(applicationContext);
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(properties, PROPERTIES_PREFIX);
        dataBinder.bind(new PropertySourcesPropertyValues(applicationContext.getEnvironment().getPropertySources()));
        return properties;
    }
}
