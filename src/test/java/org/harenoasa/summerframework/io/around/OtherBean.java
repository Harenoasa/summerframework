package org.harenoasa.summerframework.io.around;


import org.harenoasa.summerframework.context.annotation.Autowired;
import org.harenoasa.summerframework.context.annotation.Component;
import org.harenoasa.summerframework.context.annotation.Order;

@Order(0)
@Component
public class OtherBean {

    public OriginBean origin;

    public OtherBean(@Autowired OriginBean origin) {
        this.origin = origin;
    }
}
