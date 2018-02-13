package org.snapitch.contaistner;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snapitch.contaistner.configuration.ContaistnerProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.Assert;

import java.util.Map.Entry;

@Slf4j
public class ServicesLogger implements ApplicationContextAware, InitializingBean, DisposableBean {

    private ConfigurableApplicationContext applicationContext;
    private Client client;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(applicationContext instanceof ConfigurableApplicationContext) {
            this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        }
    }

    @Override
    public void destroy() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(applicationContext, "ApplicationContext must be a ConfigurableApplicationContext");

        ContaistnerProperties properties = PropertiesFactory.getForApplicationContext(applicationContext);

        for (Entry<String, ContaistnerProperties.ServiceProperties> serviceEntry : properties.getServices().entrySet()) {
            ContaistnerProperties.ServiceProperties serviceProperties = serviceEntry.getValue();
            if(serviceProperties.isLogging()) {
                Logger logger = LoggerFactory.getLogger("org.snapitch.container." + serviceEntry.getKey());
                getClient().listenLogs(serviceProperties.getId(), logger::info);
            }
        }
    }

    private Client getClient() {
        if(client == null) {
            client = new Client();
        }
        return client;
    }
}
