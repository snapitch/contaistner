package org.snapitch.contaistner;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.snapitch.contaistner.configuration.ContaistnerProperties;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static java.util.Collections.singletonList;

/**
 * Facade for {@link DockerClient}
 */
@Slf4j
public class Client implements Closeable {

    private final DockerClient client;

    @SneakyThrows
    Client() {
        client = DefaultDockerClient.fromEnv().build();
    }

    @Override
    public void close() {
        client.close();
    }

    @SneakyThrows
    public ContainerInfo startContainer(ContaistnerProperties.ServiceProperties serviceProperties) {
        String image = serviceProperties.getImage();

        pullImage(image);
        ContainerCreation container = createAndStartContainer(serviceProperties);
        return client.inspectContainer(container.id());
    }

    private void pullImage(String image) throws DockerException, InterruptedException {
        try {
            client.inspectImage(image);

        } catch (ImageNotFoundException e) {
            LOGGER.info("Pull image {}", image);
            client.pull(image);
        }
    }

    private ContainerCreation createAndStartContainer(ContaistnerProperties.ServiceProperties properties)
            throws DockerException, InterruptedException {

        ContainerCreation container = client.createContainer(
                createContainerProperties(properties));

        client.startContainer(container.id());
        return container;
    }

    private ContainerConfig createContainerProperties(ContaistnerProperties.ServiceProperties properties) {
        return ContainerConfig.builder()
                .image(properties.getImage())
                .exposedPorts(properties.getPortsAsArray())
                .env(properties.getEnvironment())
                .cmd(properties.getCmd())
                .entrypoint(properties.getEntrypoint())
                .networkDisabled(false)
                .hostConfig(createHostConfig(properties)).build();
    }

    private HostConfig createHostConfig(ContaistnerProperties.ServiceProperties properties) {
        return HostConfig.builder()
                .tmpfs(createTmpFs(properties.getTmpfs()))
                .portBindings(createPortBindings(properties)).build();
    }

    private Map<String, String> createTmpFs(List<String> tmpfs) {
        Map<String, String> tmpFsMap = new HashMap<>();
        for (String value : tmpfs) {
            String[] splitValue = value.split(":");
            tmpFsMap.put(splitValue[0], (splitValue.length >= 2 ? splitValue[1] : ""));
        }
        return tmpFsMap;
    }

    private Map<String, List<PortBinding>> createPortBindings(ContaistnerProperties.ServiceProperties properties) {
        final Map<String, List<PortBinding>> portBindings = new HashMap<>();
        String[] portsArray = properties.getPortsAsArray();
        if (portsArray != null) {
            for (final String port : portsArray) {
                final List<PortBinding> hostPorts;
                if (properties.getBindings().get(port) == null) {
                    hostPorts = singletonList(randomPort("0.0.0.0"));
                } else {
                    hostPorts = singletonList(PortBinding.of("0.0.0.0", properties.getBindings().get(port)));
                }

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
            LOGGER.debug("Impossible to stop container with id {}", containerId);
        }
    }

    private void stopContainer(ContainerInfo containerInfo) {
        ContainerState state = containerInfo.state();
        if (state != null && state.running() != null && state.running()) {
            try {
                client.stopContainer(containerInfo.id(), 10);
            } catch (Exception e) {
                LOGGER.warn("Impossible to stop container {}", containerInfo.name());
            }
        }
    }

    private void removeContainer(ContainerInfo containerInfo) throws DockerException, InterruptedException {
        client.removeContainer(containerInfo.id());
    }

    public Thread listenLogs(String containerId, Consumer<String> logConsumer) {
        LogsListener logsListener = new LogsListener(client, containerId, logConsumer);
        Thread thread = new Thread(logsListener::listenLogs);
        thread.start();
        return thread;
    }
}
