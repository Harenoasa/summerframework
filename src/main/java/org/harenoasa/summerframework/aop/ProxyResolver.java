package org.harenoasa.summerframework.aop;

import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.InvocationHandler;

@Slf4j
public class ProxyResolver {

    ByteBuddy byteBuddy = new ByteBuddy();

    public <T> T createProxy(T bean, InvocationHandler handler){
        Class<?> targetClass = bean.getClass();
        Class<?> proxyClass = byteBuddy
                .subclass(targetClass, ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR)
                .method(ElementMatchers.isPublic()).intercept(InvocationHandlerAdapter.of(
                        new InvocationHandler() {

                        }
                ))
                .
    }

}
