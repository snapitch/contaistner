package org.snapitch.contaistner.listener;

import org.snapitch.contaistner.event.ServiceStartedEvent;

public interface ServiceListener {

    void onServiceStarted(ServiceStartedEvent event);

}
