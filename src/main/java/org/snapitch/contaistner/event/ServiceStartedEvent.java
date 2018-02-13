package org.snapitch.contaistner.event;

import org.snapitch.contaistner.Service;
import org.springframework.context.ApplicationEvent;

public class ServiceStartedEvent extends ApplicationEvent {

    public ServiceStartedEvent(Service service) {
        super(service);
    }

    public Service getService() {
        return (Service) getSource();
    }
}
