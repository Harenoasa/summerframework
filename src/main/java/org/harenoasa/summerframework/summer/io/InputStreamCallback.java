package org.harenoasa.summerframework.summer.io;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface InputStreamCallback<T> {

    public T doWithInputStream(InputStream stream) throws IOException;

}
