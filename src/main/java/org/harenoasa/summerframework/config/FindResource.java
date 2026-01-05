package org.harenoasa.summerframework.config;

import java.io.IOException;
import java.net.URISyntaxException;

public class FindResource {

    public void startScanning(){
        ResourceResolver resolver = new ResourceResolver("org.example");
        resolver.scan(res -> {
            String name = res.name();
            if (name.endsWith(".class")) {
                return name.substring(0, name.length() - 6).replace("/",".").replace("\\",".");
            }
            return null;
        });

    }
    


    public static void main(String[] args) throws IOException, URISyntaxException {
    }


}
