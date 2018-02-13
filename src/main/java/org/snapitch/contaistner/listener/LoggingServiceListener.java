package org.snapitch.contaistner.listener;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snapitch.contaistner.Service;
import org.snapitch.contaistner.event.ServiceStartedEvent;

@Slf4j
public class LoggingServiceListener implements ServiceListener {

    public void onServiceStarted(ServiceStartedEvent event) {
        Service service = event.getService();
        if (service.getProperties().isLogging()) {
            LOGGER.info("Start append service {} logs to console", service.getServiceName());

            new Thread(() -> service.getContainerInfo().ifPresent(
                    containerInfo -> {
                        Logger logger = LoggerFactory.getLogger("service." + service.getServiceName());
                        service.getClient().listenLogs(containerInfo.id(), logger::info);
                    }
            )).start();
        }
    }
}
