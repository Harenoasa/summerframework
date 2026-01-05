package org.harenoasa.summerframework.summer.io;


import org.harenoasa.summerframework.config.ResourceResolver;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class ResourceResolverTest {

    @Test
    public void scanClass() {
        var pkg = "com.itranswarp.scan";
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        System.out.println(classes);
    }

}
