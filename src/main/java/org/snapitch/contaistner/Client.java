package org.snapitch.contaistner;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static java.util.Collections.singletonList;

@Slf4j
class Client implements Closeable {

    private final DockerClient client;

    Client() {
        try {
            this.client = DefaultDockerClient.fromEnv().build();

        } catch (DockerCertificateException e) {
            throw new IllegalStateException("Unable to create docker client", e);
        }
    }

    @Override
    public void close() {
        this.client.close();
    }

    public ContainerInfo startContainer(ContaistnerProperties.Service serviceProperties) {
        String image = serviceProperties.getImage();

        try {
            pullImage(image);
            ContainerCreation container = createAndStartContainer(serviceProperties);
            return client.inspectContainer(container.id());

        } catch (Exception e) {
            throw new IllegalStateException("Impossible to start container " + image, e);
        }
    }

    private void pullImage(String image) throws DockerException, InterruptedException {
        try {
            this.client.inspectImage(image);

        } catch (ImageNotFoundException e) {
            LOGGER.info("Pull image {}", image);
            this.client.pull(image);
        }
    }

    private ContainerCreation createAndStartContainer(ContaistnerProperties.Service properties)
            throws DockerException, InterruptedException {

        ContainerCreation container = this.client.createContainer(
                createContainerProperties(properties));

        this.client.startContainer(container.id());
        return container;
    }

    private ContainerConfig createContainerProperties(ContaistnerProperties.Service properties) {
        return ContainerConfig.builder()
                .image(properties.getImage())
                .exposedPorts(properties.getPortsAsArray())
                .env(properties.getEnvironment())
                .cmd(properties.getCmd())
                .entrypoint(properties.getEntrypoint())
                .networkDisabled(false)
                .hostConfig(createHostConfig(properties.getPortsAsArray())).build();
    }

    private HostConfig createHostConfig(String[] ports) {
        return HostConfig.builder().portBindings(createPortBindings(ports)).build();
    }

    private Map<String, List<PortBinding>> createPortBindings(final String[] exposedPorts) {
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        if (exposedPorts != null) {
            for (final String port : exposedPorts) {
                final List<PortBinding> hostPorts = singletonList(randomPort("0.0.0.0"));
                portBindings.put(port, hostPorts);
            }
        }
        return portBindings;
    }

    public void stopContainer(String containerId) {
        try {
            ContainerInfo containerInfo = client.inspectContainer(containerId);
            LOGGER.info("Stop and remove existing container {}", containerId);
            stopContainer(containerInfo);
            removeContainer(containerInfo);

        } catch (Exception e) {
            LOGGER.debug("No container existing with id {}", containerId);
        }
    }

    private void stopContainer(ContainerInfo containerInfo) {
        try {
            ContainerState state = containerInfo.state();
            if (state != null && state.running() != null && state.running()) {
                try {
                    client.stopContainer(containerInfo.id(), 10);
                } catch (Exception e) {
                    LOGGER.warn("Impossible to stop container {}", containerInfo.name());
                }
            }

        } catch (Exception e) {
            LOGGER.warn("Impossible to stop container {}", containerInfo.name());
        }
    }

    private void removeContainer(ContainerInfo containerInfo) {
        try {
            client.removeContainer(containerInfo.id());

        } catch (Exception e) {
            LOGGER.warn("Impossible to remove container {}", containerInfo.name());
        }
    }
}
