package org.harenoasa.summerframework.summer.io;


import org.harenoasa.summerframework.config.ResourceResolver;
import org.junit.Test;


import java.util.Collections;
import java.util.List;

public class ResourceResolverTest {

    @Test
    public void scanClass() {
        var pkg = "org.junit";
        var rr = new ResourceResolver(pkg);
        List<String> classes = rr.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            return null;
        });
        Collections.sort(classes);
        classes.stream().forEach(item -> {
            System.out.println("item name : "  + item);
        });
    }

}
