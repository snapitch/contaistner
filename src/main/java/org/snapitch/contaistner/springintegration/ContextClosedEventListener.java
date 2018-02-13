package org.snapitch.contaistner.springintegration;

import lombok.RequiredArgsConstructor;
import org.snapitch.contaistner.ServiceContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

@RequiredArgsConstructor
public class ContextClosedEventListener implements ApplicationListener<ContextClosedEvent> {

    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        ServiceContext.deleteFor(applicationContext);
    }
}
