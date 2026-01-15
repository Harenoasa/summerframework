package org.harenoasa.summerframework.io.scan;

import org.harenoasa.summerframework.context.annotation.ComponentScan;
import org.harenoasa.summerframework.context.annotation.Import;
import org.harenoasa.summerframework.io.imported.LocalDateConfiguration;
import org.harenoasa.summerframework.io.imported.ZonedDateConfiguration;

@ComponentScan
@Import({ LocalDateConfiguration.class, ZonedDateConfiguration.class })
public class ScanApplication {

}
