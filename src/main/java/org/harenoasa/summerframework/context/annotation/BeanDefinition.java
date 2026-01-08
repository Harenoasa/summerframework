package org.harenoasa.summerframework.context.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BeanDefinition {
    String name;

    Class<?> beanClass;

    Object instance = null;

    Constructor<?> constructor;

    String factoryName;

    Method factoryMethod;

    int order;

    boolean primary;

    String initMethodName;
    String destroyMethodName;

    Method initMethod;
    Method destoryMethod;
}
