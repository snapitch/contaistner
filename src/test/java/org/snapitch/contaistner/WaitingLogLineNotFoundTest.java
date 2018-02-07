package org.snapitch.contaistner;

import org.junit.Test;
import org.springframework.boot.SpringApplication;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

public class WaitingLogLineNotFoundTest {

    @Test
    public void containerIsNotReady() {
        try {
            SpringApplication springApplication = new SpringApplication(ContainersFinalizerOnApplicationContextLoadingErrorTest.Application.class);
            springApplication.setAdditionalProfiles("waiting-log-line-not-found");
            springApplication.run();

        } catch (Exception e) {
            assertThat(e, instanceOf(ContaistnerException.class));
        }
    }
}
