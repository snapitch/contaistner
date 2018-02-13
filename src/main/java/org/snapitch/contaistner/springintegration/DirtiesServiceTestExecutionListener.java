package org.snapitch.contaistner.springintegration;

import org.snapitch.contaistner.DirtiesService;
import org.snapitch.contaistner.ServiceContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.EMPTY_LIST;

/**
 * {@link org.springframework.test.context.TestExecutionListener} that manage {@link DirtiesService} annotations.
 */
public class DirtiesServiceTestExecutionListener extends AbstractTestExecutionListener {

    private List<String> dirtiesServices = EMPTY_LIST;

    @Override
    public void beforeTestMethod(TestContext testContext) {
        if(!dirtiesServices.isEmpty()) {

            ConfigurableApplicationContext applicationContext =
                    (ConfigurableApplicationContext) testContext.getApplicationContext();

            ServiceContext serviceContext = ServiceContext.getFor(applicationContext);

            dirtiesServices.parallelStream().forEach(
                    dirtyService -> serviceContext.getServiceByName(dirtyService).restart());

            dirtiesServices = EMPTY_LIST;
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        DirtiesService annotation = AnnotatedElementUtils.findMergedAnnotation(testContext.getTestMethod(), DirtiesService.class);
        if(annotation != null) {
            dirtiesServices = Arrays.asList(annotation.value());
        }
    }
}
