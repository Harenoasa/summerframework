package org.harenoasa.summerframework.io.before;


import org.harenoasa.summerframework.aop.AroundProxyBeanPostProcessor;
import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.ComponentScan;
import org.harenoasa.summerframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class BeforeApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
