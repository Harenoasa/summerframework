package org.harenoasa.summerframework.io.after;


import org.harenoasa.summerframework.aop.AroundProxyBeanPostProcessor;
import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.ComponentScan;
import org.harenoasa.summerframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class AfterApplication {

    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor() {
        return new AroundProxyBeanPostProcessor();
    }
}
