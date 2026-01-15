package org.harenoasa.summerframework.io.scan.destroy;


import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.Configuration;
import org.harenoasa.summerframework.context.annotation.Value;

@Configuration
public class SpecifyDestroyConfiguration {

    @Bean(destroyMethod = "destroy")
    SpecifyDestroyBean createSpecifyDestroyBean(@Value("${app.title}") String appTitle) {
        return new SpecifyDestroyBean(appTitle);
    }
}
