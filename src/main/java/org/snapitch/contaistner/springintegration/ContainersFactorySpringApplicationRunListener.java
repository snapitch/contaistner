package org.snapitch.contaistner.springintegration;

import lombok.extern.slf4j.Slf4j;
import org.snapitch.contaistner.ContaistnerException;
import org.snapitch.contaistner.PropertiesFactory;
import org.snapitch.contaistner.Service;
import org.snapitch.contaistner.ServiceContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.snapitch.contaistner.ServiceContext.getFor;

@Slf4j
public class ContainersFactorySpringApplicationRunListener implements SpringApplicationRunListener {

    public ContainersFactorySpringApplicationRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void starting() {
    }

    @Override
    public void environmentPrepared(ConfigurableEnvironment environment) {
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {
        try {
            getFor(context).getAllServices().parallelStream().forEach(this::startServiceAndWaitingReadiness);

            PropertiesFactory.loadGeneratedProperties(context);
            PropertiesFactory.loadApplicativeConfiguration(context);

        } catch (Exception e) {
            throw new ContaistnerException("Impossible to start services", e);
        }
    }

    @Override
    public void contextLoaded(ConfigurableApplicationContext context) {

    }

    @Override
    public void started(ConfigurableApplicationContext context) {

    }

    @Override
    public void running(ConfigurableApplicationContext context) {

    }

    @Override
    public void failed(ConfigurableApplicationContext context, Throwable exception) {
        ServiceContext.deleteFor(context);
    }

    private void startServiceAndWaitingReadiness(Service service) {
        service.start();
        service.waitReadiness();
    }
}
