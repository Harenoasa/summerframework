package org.harenoasa.summerframework.io.scan.nested;


import org.harenoasa.summerframework.context.annotation.Component;

@Component
public class OuterBean {

    @Component
    public static class NestedBean {

    }
}
