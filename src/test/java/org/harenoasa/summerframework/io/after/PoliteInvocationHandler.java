package org.harenoasa.summerframework.io.after;


import org.harenoasa.summerframework.aop.AfterInvocationHandlerAdapter;
import org.harenoasa.summerframework.context.annotation.Component;

import java.lang.reflect.Method;

@Component
public class PoliteInvocationHandler extends AfterInvocationHandlerAdapter {

    @Override
    public Object after(Object proxy, Object returnValue, Method method, Object[] args) {
        if (returnValue instanceof String s) {
            if (s.endsWith(".")) {
                return s.substring(0, s.length() - 1) + "!";
            }
        }
        return returnValue;
    }
}
