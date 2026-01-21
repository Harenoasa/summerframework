package org.harenoasa.summerframework.util;


import jakarta.annotation.Nullable;
import org.harenoasa.summerframework.context.annotation.Bean;
import org.harenoasa.summerframework.context.annotation.Component;
import org.harenoasa.summerframework.entity.exception.BeanDefinitionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ClassUtils {

    public static <A extends Annotation> A getAnnotation(Class<?> target, Class<A> annoClass){
        A a = target.getAnnotation(annoClass);
        for (Annotation anno : target.getAnnotations()) {
            Class<? extends Annotation> annoType = anno.annotationType();
            if(!annoType.getPackageName().equals("java.lang.annotation")) {
                A found = getAnnotation(annoType,annoClass);
                if(found == null) continue;
                else if (a != null) throw new BeanDefinitionException(String.format("duplicate @%s found on class %s:", annoClass.getName(), target.getName()));
                a = found;
            }
        }
        return a;
    }

    public static <A extends Annotation> A getAnnotation(Annotation[] annos, Class<A> annoClass){
        for (Annotation anno : annos) {
            if(annoClass.isInstance(anno)){
                return annoClass.cast(anno);
            }
        }
        return null;
    }

    @Nullable
    public static Method findAnnotationMethod(Class<?> clazz, Class<? extends Annotation> annoClass) {

        List<Method> ms = Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(annoClass)).map(m -> {
            if(m.getParameterCount() != 0) {
                throw new BeanDefinitionException(
                        String.format("Method '%s' with @%s must not have argument: %s", m.getName(), annoClass.getSimpleName(), clazz.getName()));
            }
            return m;
        }).toList();
        if (ms.isEmpty()) {
            return null;
        }
        if(ms.size() == 1){
            return ms.get(0);
        }
        throw new BeanDefinitionException(String.format("Multiple methods with @%s found in class: %s", annoClass.getSimpleName(), clazz.getName()));
    }


    public static String getBeanName(Class<?> clazz) {
        String name = "";
        Component component = clazz.getAnnotation(Component.class);
        if(component != null) name = component.value();
        else {
            for (Annotation anno : clazz.getAnnotations()) {
                if(getAnnotation(anno.annotationType(),Component.class) != null) {
                    try {
                        name = (String) anno.annotationType().getMethod("value").invoke(anno);
                    } catch (ReflectiveOperationException e) {
                            throw new BeanDefinitionException("Cannot get annotation value.", e);
                    }
                }
            }
        }
        if(name.isEmpty()){
            name = clazz.getSimpleName();
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        return name;
    }
    public static String getBeanName(Method method) {
        Bean bean = method.getAnnotation(Bean.class);
        String name = bean.value();
        if (name.isEmpty()) {
            name = method.getName();
        }
        return name;
    }

    public static Method getNamedMethod(Class<?> clazz, String methodName) {
        try {
            return clazz.getDeclaredMethod(methodName);
        } catch (ReflectiveOperationException e) {
            throw new BeanDefinitionException(String.format("Method '%s' not found in class: %s", methodName, clazz.getName()));
        }
    }
}
