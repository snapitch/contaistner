package org.snapitch.contaistner.configuration;

import org.snapitch.contaistner.springintegration.ContextClosedEventListener;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContaistnerProperties.class)
public class ContaistnerAutoConfiguration {

    @Bean
    public ContextClosedEventListener contaistnerContextClosedEventListener(ConfigurableApplicationContext applicationContext) {
        return new ContextClosedEventListener(applicationContext);
    }
}
