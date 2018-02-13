package org.snapitch.contaistner.listener;

import org.snapitch.contaistner.event.ServiceStartedEvent;
import org.snapitch.contaistner.event.ServiceStoppedEvent;

public interface ServiceListener {

    default void onServiceStarted(ServiceStartedEvent event) {}

    default void onServiceStopped(ServiceStoppedEvent event) {}

}
