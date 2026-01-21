package org.harenoasa.summerframework.io.scan.proxy;

import org.harenoasa.summerframework.context.annotation.Autowired;
import org.harenoasa.summerframework.context.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {

    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}