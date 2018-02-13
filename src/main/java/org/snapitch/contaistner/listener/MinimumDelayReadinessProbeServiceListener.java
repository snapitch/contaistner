package org.snapitch.contaistner.listener;

import org.snapitch.contaistner.Service;
import org.snapitch.contaistner.configuration.ContaistnerProperties.ServiceProperties;

public class MinimumDelayReadinessProbeServiceListener extends ReadinessProbeServiceListener {

    @Override
    protected void onServiceStart(Service service) {
        ServiceProperties properties = service.getProperties();

        if (properties.getReadiness().getMinWaitingDelay() > 0L) {
            addProbeToService(service, getFutureProbe(() -> waitMinimumDelay(properties)));
        }
    }

    private Boolean waitMinimumDelay(ServiceProperties properties) {
        try {
            Thread.sleep(properties.getReadiness().getMinWaitingDelay() * 1000L);
        } catch (Exception ignored) {
        }
        return true;
    }
}
