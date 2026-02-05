package org.harenoasa.summerframework.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.web.util.WebUtils;

@Slf4j
public class ContextLoaderListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("init {},", getClass().getName());
        ServletContext propertyResolver = sce.getServletContext();
        String encoding =  propertyResolver.
        WebUtils.createPropertyResolver();
    }
}
