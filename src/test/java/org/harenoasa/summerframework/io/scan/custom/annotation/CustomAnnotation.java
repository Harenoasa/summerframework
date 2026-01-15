package org.harenoasa.summerframework.io.scan.custom.annotation;


import org.harenoasa.summerframework.context.annotation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CustomAnnotation {

    String value() default "";

}
