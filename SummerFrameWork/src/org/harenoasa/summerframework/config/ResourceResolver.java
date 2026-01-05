package org.harenoasa.summerframework.config;

import org.harenoasa.summerframework.entity.Resource;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

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

        try {
            ArrayList<R> collector = new ArrayList<>();
            scan0(basePackagePath, path, collector, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    <R> void scan0(String basePackagePath, String path, List<R> collector, Function<Resource, R> mapper) throws Exception {
        System.out.println("scanning " + path);
        Enumeration<URL> en = getClassLoader().getResources(path);
        while(en.hasMoreElements()) {
            URL url = en.nextElement();
            URI uri = url.toURI();
            System.out.printf("url : %s\n", url);
            System.out.println("uri: " + uri);
            String uriStr = removeTrailingSlash(uri.toString());
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if(uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            if(uriBaseStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri),collector, mapper);
            }else {
                scanFile(false, uriBaseStr, Paths.get(uri),collector, mapper);
            }
        }
    }

    <R> void scanFile(boolean isJar,String base, Path root, List<R> collector, Function<Resource, R> mapper)throws IOException {
        String baseDir = removeTrailingSlash(base);
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res = null;
            if(isJar) {
                res = new Resource(baseDir, removeLeadingSlash());
            }
        });
    }

    Path jarUriToPath(String basePackagePath, URI jarUri)throws IOException {
        System.out.println("Path jarUriToPath(String basePackagePath - > " + basePackagePath);
        Path path = FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);
        System.out.println("FileSystems.newFileSystem(jarUri, Map.of()).getPath(basePackagePath);" + path);
        return path;
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
