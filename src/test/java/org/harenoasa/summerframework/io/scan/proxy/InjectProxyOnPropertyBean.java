package org.harenoasa.summerframework.io.scan.proxy;

import org.harenoasa.summerframework.context.annotation.Autowired;
import org.harenoasa.summerframework.context.annotation.Component;

@Component
public class InjectProxyOnPropertyBean {

    @Autowired
    public OriginBean injected;
}
