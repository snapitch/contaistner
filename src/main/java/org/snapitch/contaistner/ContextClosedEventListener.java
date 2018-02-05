package org.snapitch.contaistner;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@RequiredArgsConstructor
public class ContextClosedEventListener implements ApplicationListener<ContextClosedEvent> {

    private final ContaistnerProperties properties;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        new ContainersRemover(properties).removeContainers();
    }
}
