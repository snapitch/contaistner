package org.snapitch.contaistner;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class FutureReadinessProbe implements ReadinessProbe {

    private final Future<Boolean> probe;
    private final long maxDelay;

    @Override
    public void check() {
        try {
            Boolean probeResult = probe.get(maxDelay, TimeUnit.SECONDS);
            if(probeResult == null || probeResult == Boolean.FALSE) {
                throw new ContaistnerException("Readiness probe return false");
            }

        } catch (Exception e) {
            throw new ContaistnerException("Readiness probe check fail", e);
        }
    }
}
