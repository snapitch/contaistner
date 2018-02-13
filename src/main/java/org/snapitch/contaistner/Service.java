package org.snapitch.contaistner;

import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.snapitch.contaistner.configuration.ContaistnerProperties.ServiceProperties;
import org.snapitch.contaistner.event.ServiceStartedEvent;
import org.snapitch.contaistner.event.ServiceStoppedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.*;

import static java.lang.Integer.parseInt;
import static org.snapitch.contaistner.PropertiesFactory.PROPERTIES_PREFIX;

@Slf4j
@RequiredArgsConstructor
public class Service {

    private final ConfigurableApplicationContext applicationContext;
    private final String serviceName;
    private final Client client;
    private final ApplicationEventMulticaster applicationEventMulticaster;
    private final List<ReadinessProbe> readinessProbes = Collections.synchronizedList(new ArrayList<>());
    private ContainerInfo containerInfo = null;

    public void start() {
        ServiceProperties properties = PropertiesFactory.getForService(applicationContext, serviceName);
        containerInfo = client.startContainer(properties);
        applicationEventMulticaster.multicastEvent(new ServiceStartedEvent(this));
    }

    public Map<String, String> getGeneratedProperties() {
        HashMap<String, String> properties = new HashMap<>();
        if(containerInfo != null) {
            ImmutableMap<String, List<PortBinding>> ports = containerInfo.networkSettings().ports();
            if (ports != null) {
                for (String port : ports.keySet()) {
                    int bindingPort = getPort(ports.get(port));
                    if (bindingPort != -1) {
                        properties.put(PROPERTIES_PREFIX + ".services." + serviceName + ".bindings." + port, String.valueOf(bindingPort));
                    }
                }
            }
        }
        return properties;
    }

    private int getPort(List<PortBinding> portBindings) {
        if (portBindings != null && !portBindings.isEmpty()) {
            final PortBinding firstBinding = portBindings.get(0);
            return parseInt(firstBinding.hostPort());
        }

        return -1;
    }

    public void waitReadiness() {
        for (ReadinessProbe readinessProbe : readinessProbes) {
            readinessProbe.check();
        }
    }

    public Optional<ContainerInfo> getContainerInfo() {
        return Optional.ofNullable(containerInfo);
    }

    public void stop() {
        if(containerInfo != null) {
            client.stopContainer(containerInfo.id());
            applicationEventMulticaster.multicastEvent(new ServiceStoppedEvent(this));
        }
    }

    public void restart() {
        stop();
        start();
    }

    public ServiceProperties getProperties() {
        return PropertiesFactory.getForService(applicationContext, serviceName);
    }

    public Client getClient() {
        return client;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void addReadinessProbe(ReadinessProbe probe) {
        readinessProbes.add(probe);
    }
}
