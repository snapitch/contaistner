package org.snapitch.contaistner.listener;

import lombok.extern.slf4j.Slf4j;
import org.snapitch.contaistner.Service;
import org.snapitch.contaistner.configuration.ContaistnerProperties.ServiceProperties;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@Slf4j
public class PortsAccessibilityReadinessProbeServiceListener extends ReadinessProbeServiceListener {

    @Override
    protected void onServiceStart(Service service) {
        ServiceProperties properties = service.getProperties();

        addProbeToService(service, getFutureProbe(() -> checkAllPortsAccessibles(service, properties)));
    }

    private Boolean checkAllPortsAccessibles(Service service, ServiceProperties properties) {
        try {
            for (Map.Entry<String, String> bindings : properties.getBindings().entrySet()) {
                await().atMost(properties.getReadiness().getMaxWaitingDelay(), SECONDS)
                        .until(() -> isPortAccessible(service.getServiceName(), parseInt(bindings.getValue())));
            }

            return true;

        } catch (Exception ignored) {
            return false;
        }
    }

    private Boolean isPortAccessible(String serviceName, int port) {
        LOGGER.debug("Check port {} accessibility for service {}", port, serviceName);
        try (final Socket ignored = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
