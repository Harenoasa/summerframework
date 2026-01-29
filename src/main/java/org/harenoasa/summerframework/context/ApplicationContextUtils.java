package org.harenoasa.summerframework.context;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class ApplicationContextUtils {
    private static ApplicationContext applicationContext = null;

    public ApplicationContextUtils() {
    }

    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return (ApplicationContext) Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set.");
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }
}