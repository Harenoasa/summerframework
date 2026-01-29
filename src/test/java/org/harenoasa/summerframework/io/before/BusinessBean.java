package org.harenoasa.summerframework.io.before;


import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.context.annotation.Around;
import org.harenoasa.summerframework.context.annotation.Component;

@Component
@Around("logInvocationHandler")
@Slf4j
public class BusinessBean {


    public String hello(String name) {
        log.info("Hello, {}.", name);
        return "Hello, " + name + ".";
    }

    public String morning(String name) {
        log.info("Morning, {}.", name);
        return "Morning, " + name + ".";
    }
}
