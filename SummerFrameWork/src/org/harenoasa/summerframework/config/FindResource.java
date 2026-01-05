package org.harenoasa.summerframework.config;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
