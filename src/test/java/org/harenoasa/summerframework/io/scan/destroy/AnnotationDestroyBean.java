package org.harenoasa.summerframework.io.scan.destroy;

import jakarta.annotation.PreDestroy;
import org.harenoasa.summerframework.context.annotation.Component;
import org.harenoasa.summerframework.context.annotation.Value;

@Component
public class AnnotationDestroyBean {

    @Value("${app.title}")
    public String appTitle;

    @PreDestroy
    void destroy() {
        this.appTitle = null;
    }
}
