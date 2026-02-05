package org.harenoasa.summerframework.io.with.tx;


import org.harenoasa.summerframework.context.annotation.ComponentScan;
import org.harenoasa.summerframework.context.annotation.Configuration;
import org.harenoasa.summerframework.context.annotation.Import;
import org.harenoasa.summerframework.jdbc.JdbcConfiguration;

@ComponentScan
@Configuration
@Import(JdbcConfiguration.class)
public class JdbcWithTxApplication {

}
