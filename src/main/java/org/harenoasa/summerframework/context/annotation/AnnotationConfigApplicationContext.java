package org.harenoasa.summerframework.context.annotation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
public class AnnotationConfigApplicationContext {
    Map<String ,BeanDefinition> beans;

    @Nullable
    public BeanDefinition findBeanDefinition(String name){
        return beans.get(name);
    }

    List<BeanDefinition> findBeanDefinitions(Class<?> type){
        return beans.values().stream()
                .filter(def -> type.isAssignableFrom(def.getBeanClass()))
                .sorted().collect(Collectors.toList())

    }
}
