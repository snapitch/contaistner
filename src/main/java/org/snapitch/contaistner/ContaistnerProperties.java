package org.snapitch.contaistner;

import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("contaistner")
public class ContaistnerProperties implements ApplicationContextAware {

    private ConfigurableApplicationContext applicationContext;

    /**
     * Configuration file that describe containers to start
     */
    private String bootstrapFile = "classpath:application-bootstrap-contaistner.yml";

    /**
     * Configuration file that contains applicative properties that depends on containers dynamic information (eq port...)
     */
    private String applicationFile = "classpath:application-contaistner.yml";

    /**
     * Configuration of containers
     */
    private Map<String, Service> services = new HashMap<>();

    public Resource getBootstrapResource() {
        return applicationContext.getResource(bootstrapFile);
    }

    public Resource getApplicationResource() {
        return applicationContext.getResource(applicationFile);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
                "Docker containers need ConfigurableApplicationContext");

        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    public String getBootstrapFile() {
        return bootstrapFile;
    }

    public void setBootstrapFile(String bootstrapFile) {
        this.bootstrapFile = bootstrapFile;
    }

    public String getApplicationFile() {
        return applicationFile;
    }

    public void setApplicationFile(String applicationFile) {
        this.applicationFile = applicationFile;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public void setServices(Map<String, Service> services) {
        this.services = services;
    }

    @Data
    public static class Service {
        /**
         * Container identifier
         */
        private String id;

        /**
         * Container name
         */
        private String name;

        /**
         * Container image with version
         */
        private String image;

        /**
         * Container ports to expose
         */
        private List<String> ports = new ArrayList<>();

        /**
         * Ports bindings
         */
        private Map<String, String> bindings = new HashMap<>();

        /**
         * Container environment variables
         */
        private List<String> environment = new ArrayList<>();

        /**
         * Container startup command
         */
        private List<String> cmd;

        /**
         * Container entry point
         */
        private List<String> entrypoint;

        /**
         * Delay in seconds to wait after container start
         */
        private int waitDelay = 0;

        public String[] getPortsAsArray() {
            return this.getPorts().toArray(new String[this.getPorts().size()]);
        }
    }
}
