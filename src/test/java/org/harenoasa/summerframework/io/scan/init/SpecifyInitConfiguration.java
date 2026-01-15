package org.harenoasa.summerframework.io.scan.init;


import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.Configuration;
import org.harenoasa.summerframework.context.annotation.Value;

@Configuration
public class SpecifyInitConfiguration {

    @Bean(initMethod = "init")
    SpecifyInitBean createSpecifyInitBean(@Value("${app.title}") String appTitle, @Value("${app.version}") String appVersion) {
        return new SpecifyInitBean(appTitle, appVersion);
    }
}
