package org.harenoasa.summerframework.io.imported;

import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.Configuration;

import java.time.ZonedDateTime;

@Configuration
public class ZonedDateConfiguration {

    @Bean
    ZonedDateTime startZoneDateTime() {
        return ZonedDateTime.now();
    }


}
