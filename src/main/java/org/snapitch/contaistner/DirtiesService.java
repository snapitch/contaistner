package org.snapitch.contaistner;

import java.lang.annotation.*;

/**
 * Indicate that a {@link org.junit.Test} have corrupt a container and then the next test must restart it.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DirtiesService {

    /**
     * Service names to restart
     */
    String[] value() default {};
}
