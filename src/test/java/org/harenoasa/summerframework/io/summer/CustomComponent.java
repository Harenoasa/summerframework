package org.harenoasa.summerframework.io.summer;

import org.harenoasa.summerframework.context.annotation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CustomComponent {

    String value() default "";

}
