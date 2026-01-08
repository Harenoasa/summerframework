package org.harenoasa.summerframework.util;

import org.harenoasa.summerframework.summer.io.InputStreamCallback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ClassPathUtils {

    public static <T> T readInputStream(String path, InputStreamCallback<T> inputStreamCallback){
        if (path.startsWith("/"))
            path = path.substring(1);
        try(InputStream input = getContextClassLoader().getResourceAsStream(path)) {
            if(input == null){
                throw new FileNotFoundException("File not found in calsspath: " + path);
            }
            return inputStreamCallback.doWithInputStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }
    static ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if(cl == null) {
            cl = ClassPathUtils.class.getClassLoader();
        }
        return cl;
    }
}
