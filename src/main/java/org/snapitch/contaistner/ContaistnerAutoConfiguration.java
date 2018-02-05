package org.snapitch.contaistner;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContaistnerProperties.class)
public class ContaistnerAutoConfiguration {

    @Bean
    public ContextClosedEventListener contaistnerContextClosedEventListener(ContaistnerProperties properties) {
        return new ContextClosedEventListener(properties);
    }
}
