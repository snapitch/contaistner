package org.snapitch.contaistner.listener;

import org.snapitch.contaistner.FutureReadinessProbe;
import org.snapitch.contaistner.Service;
import org.snapitch.contaistner.event.ServiceStartedEvent;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

public abstract class ReadinessProbeServiceListener implements ServiceListener {

    public void onServiceStarted(ServiceStartedEvent event) {
        onServiceStart(event.getService());
    }

    protected abstract void onServiceStart(Service service);

    protected Future<Boolean> getFutureProbe(Callable<Boolean> callable) {
        return newSingleThreadExecutor().submit(callable);
    }

    protected void addProbeToService(Service service, Future<Boolean> probeResult) {
        service.addReadinessProbe(new FutureReadinessProbe(
                probeResult, service.getProperties().getReadiness().getMaxWaitingDelay()));
    }
}
