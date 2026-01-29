package org.harenoasa.summerframework.io.before;


import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.aop.BeforeInvocationHandlerAdapter;
import org.harenoasa.summerframework.context.annotation.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class LogInvocationHandler extends BeforeInvocationHandlerAdapter {


    @Override
    public void before(Object proxy, Method method, Object[] args) {
        log.info("[Before] {}()", method.getName());
    }
}
