package org.harenoasa.summerframework.context;

public interface ApplicationContext extends AutoCloseable{

    boolean containsBean(String name);

    <T> T getBean(String name);

    <T> T getBean(String name, Class<T> requiredType);

    <T> T getBean(Class<T> requiredType);

    void close() ;
}
