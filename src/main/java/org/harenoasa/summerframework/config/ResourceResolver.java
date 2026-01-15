package org.harenoasa.summerframework.config;

import org.harenoasa.summerframework.entity.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ResourceResolver {

    Logger logger = LoggerFactory.getLogger(getClass());

    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource, R> mapper) {
        String basePackagePath =this.basePackage.replace(".","/");
        String path = basePackagePath ;
        ArrayList<R> collector = null;
        try {
            collector = new ArrayList<>();
            scan0(basePackagePath, path, collector, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return collector;
    }

    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws Exception {
//        System.out.println("scanning " + path);
        Enumeration<URL> en = getClassLoader().getResources(path);
        int i = 0;
//        System.out.println("scanning " + path);
        ArrayList<URL> urls = new ArrayList<>();
        while(en.hasMoreElements()) {
            URL url = en.nextElement();
            urls.add(url);
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uri.toString());
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if(uriBaseStr.startsWith("file:"))
                uriBaseStr = uriBaseStr.substring(5);
            if(uriBaseStr.startsWith("jar:"))
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri),collector, mapper);
            else
                scanFile(false, uriBaseStr, Paths.get(uri),collector, mapper);

            i++;
        }
    }

    <R> void scanFile(boolean isJar,String base, Path root, List<R> collector, Function<Resource, R> mapper)throws IOException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if(isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:"+path,name);
            }

            R r = mapper.apply(res);
            if(r != null) {
                collector.add(r);
            }
        });
    }

    Path jarUriToPath(String basePackagePath, URI jarUri)throws IOException {
        System.out.println("Path jarUriToPath(String basePackagePath - > " + basePackagePath);
        Path path = FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
        System.out.println("FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);" + path);
        return path;
    }
    String removeLeadingSlash(String s){
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }

    String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    ClassLoader getClassLoader() {
        ClassLoader classLoader = null;
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader;
    }
}
