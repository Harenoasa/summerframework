package org.harenoasa.summerframework.io.before;


import org.harenoasa.summerframework.config.PropertyResolver;
import org.harenoasa.summerframework.context.annotation.AnnotationConfigApplicationContext;
import org.junit.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BeforeProxyTest {

    @Test
    public void testBeforeProxy() {
        try (var ctx = new AnnotationConfigApplicationContext(BeforeApplication.class, createPropertyResolver())) {
            BusinessBean proxy = ctx.getBean(BusinessBean.class);
            // should print log:
            assertEquals("Hello, Bob.", proxy.hello("Bob"));
            assertEquals("Morning, Alice.", proxy.morning("Alice"));
        }
    }

    PropertyResolver createPropertyResolver() {
        var ps = new Properties();
        var pr = new PropertyResolver(ps);
        return pr;
    }
}
