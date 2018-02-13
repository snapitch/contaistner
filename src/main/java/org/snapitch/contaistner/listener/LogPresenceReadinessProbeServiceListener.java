package org.snapitch.contaistner.listener;

import lombok.extern.slf4j.Slf4j;
import org.snapitch.contaistner.Service;
import org.snapitch.contaistner.configuration.ContaistnerProperties.ServiceProperties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class LogPresenceReadinessProbeServiceListener extends ReadinessProbeServiceListener {

    @Override
    public void onServiceStart(Service service) {
        if (service.getProperties().getReadiness().getWaitingLogLine() != null) {
            addProbeToService(service, getFutureProbe(() -> waitingLog(service)));
        }
    }

    private boolean waitingLog(Service service) {
        try {
            ServiceProperties properties = service.getProperties();
            if (properties.getReadiness().getWaitingLogLine() != null) {
                LOGGER.info("Waiting log line '{}' for service {}",
                        properties.getReadiness().getWaitingLogLine(), service.getServiceName());

                Thread thread = service.getClient()
                        .listenLogs(
                                service.getContainerInfo().map(ci -> ci.id()).orElse(null),
                                logLine -> isLogLinePresent(logLine, properties));

                await().atMost(properties.getReadiness().getMaxWaitingDelay(), SECONDS)
                        .until(() -> !thread.isAlive());

                LOGGER.info("Log line containing'{}' is found", properties.getReadiness().getWaitingLogLine());
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void isLogLinePresent(String logLine, ServiceProperties properties) {
        if (logLine.contains(properties.getReadiness().getWaitingLogLine())) {
            throw new RuntimeException("Log line matching is found");
        }
    }
}
