package org.snapitch.contaistner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ContainersRemover {

    private final ContaistnerProperties properties;

    public void removeContainers() {
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
