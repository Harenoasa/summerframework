package org.harenoasa.summerframework.io.scan.primary;


import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.Configuration;
import org.harenoasa.summerframework.context.annotation.Primary;

@Configuration
public class PrimaryConfiguration {

    @Primary
    @Bean
    DogBean husky() {
        return new DogBean("Husky");
    }

    @Bean
    DogBean teddy() {
        return new DogBean("Teddy");
    }
}
