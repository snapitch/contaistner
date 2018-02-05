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
     * Configuration file that contains applicative properties that depends on containers dynamic information (eq port...)
     */
    private String applicationFile = "classpath:application-contaistner.yml";

    /**
     * Configuration of containers
     */
    private Map<String, Service> services = new HashMap<>();

    public Resource getApplicationResource() {
        return applicationContext.getResource(applicationFile);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
                "Docker containers need ConfigurableApplicationContext");

        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
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
         * Specify the image to start the container from. Can either be a repository/tag or a partial image ID.
         */
        private String image;

        /**
         * Expose ports.
         */
        private List<String> ports = new ArrayList<>();

        /**
         * Ports bindings
         */
        private Map<String, String> bindings = new HashMap<>();

        /**
         * Add environment variables
         */
        private List<String> environment = new ArrayList<>();

        /**
         * Mount a temporary file system inside the container. Can be a single value or a list.
         */
        private List<String> tmpfs = new ArrayList<>();

        /**
         * Override the default command.
         */
        private List<String> cmd;

        /**
         * Override the default entrypoint.
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
