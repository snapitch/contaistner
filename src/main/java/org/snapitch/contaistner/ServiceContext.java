package org.snapitch.contaistner;

import org.snapitch.contaistner.configuration.ContaistnerProperties;
import org.snapitch.contaistner.configuration.ContaistnerProperties.ServiceProperties;
import org.snapitch.contaistner.event.ServiceStartedEvent;
import org.snapitch.contaistner.event.ServiceStoppedEvent;
import org.snapitch.contaistner.listener.ServiceListener;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ServiceContext {

    private static final ConcurrentMap<ConfigurableApplicationContext, ServiceContext> REPOSITORIES = new ConcurrentHashMap<>();

    public static ServiceContext getFor(ConfigurableApplicationContext applicationContext) {
        return REPOSITORIES.computeIfAbsent(applicationContext, ac -> new ServiceContext(applicationContext));
    }

    public static void deleteFor(ConfigurableApplicationContext applicationContext) {
        ServiceContext serviceContext = REPOSITORIES.get(applicationContext);
        serviceContext.getAllServices().parallelStream().forEach(Service::stop);
        serviceContext.client.close();
        REPOSITORIES.remove(applicationContext);
    }

    private final Map<String, Service> services = new HashMap<>();
    private final ApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
    private final Client client = new Client();

    private ServiceContext(ConfigurableApplicationContext applicationContext) {
        ContaistnerProperties properties = PropertiesFactory.getForApplicationContext(applicationContext);

        SpringFactoriesLoader.loadFactories(ServiceListener.class, this.getClass().getClassLoader())
                .forEach(this::addApplicationListeners);

        for (Entry<String, ServiceProperties> service : properties.getServices().entrySet()) {
            services.put(service.getKey(), new Service(applicationContext, service.getKey(), client, eventMulticaster));
        }
    }

    private void addApplicationListeners(ServiceListener listener) {
        eventMulticaster.addApplicationListener((ApplicationListener<ServiceStartedEvent>) listener::onServiceStarted);
        eventMulticaster.addApplicationListener((ApplicationListener<ServiceStoppedEvent>) listener::onServiceStopped);
    }

    public Set<Service> getAllServices() {
        return new HashSet<>(services.values());
    }

    public Service getServiceByName(String name) {
        return services.get(name);
    }
}
