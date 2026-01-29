package org.harenoasa.summerframework.io.around;


import org.harenoasa.summerframework.context.annotation.Around;
import org.harenoasa.summerframework.context.annotation.Component;
import org.harenoasa.summerframework.context.annotation.Value;

@Component
@Around("aroundInvocationHandler")
public class OriginBean {

    @Value("${customer.name}")
    public String name;

    @Polite
    public String hello() {
        return "Hello, " + name + ".";
    }

    public String morning() {
        return "Morning, " + name + ".";
    }
}
