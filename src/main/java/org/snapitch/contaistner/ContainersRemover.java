package org.snapitch.contaistner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;

import static org.snapitch.contaistner.ContaistnerPropertiesFactory.createFromApplicationContext;

@RequiredArgsConstructor
@Slf4j
public class ContainersRemover {

    private final ConfigurableApplicationContext applicationContext;

    public void removeContainers() {
        ContaistnerProperties properties = createFromApplicationContext(applicationContext);
        for (String containerKey : properties.getServices().keySet()) {
            ContaistnerProperties.Service serviceProperties = properties.getServices().get(containerKey);
            if (serviceProperties.getId() != null && serviceProperties.isStopAndRemove()) {
                try (Client client = new Client()) {
                    LOGGER.info("Stop container {}", containerKey);
                    client.stopContainer(serviceProperties.getId());
                }
            }
        }
    }
}
