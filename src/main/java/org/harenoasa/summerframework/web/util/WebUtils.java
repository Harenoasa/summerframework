package org.harenoasa.summerframework.web.util;

import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.config.PropertyResolver;
import org.harenoasa.summerframework.context.ApplicationContextUtils;
import org.harenoasa.summerframework.util.ClassUtils;
import org.harenoasa.summerframework.util.YamlUtils;
import org.harenoasa.summerframework.web.DispatcherServlet;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class WebUtils {
    static final String CONFIG_APP_YAML = "/application.yaml";
    static final String CONFIG_APP_PROP = "/application.properties";

    public static void registerDispatcherServlet(ServletContext servletContext, PropertyResolver propertyResolver){
        var dispatcherServlet = new DispatcherServlet(ApplicationContextUtils.getRequiredApplicationContext(), propertyResolver);
        log.info("register servlet {} for URL '/'", dispatcherServlet.getClass().getName());
        var dispatcherReg = servletContext.addServlet("dispatcherServlet", dispatcherServlet);
        dispatcherReg.addMapping("/");
        dispatcherReg.setLoadOnStartup(0);
    }

    public static PropertyResolver createPropertyRsolver(){
        final Properties props=  new Properties();

        try {
            Map<String, Object> ymlMap = YamlUtils.loadYamlAsPlainMap(CONFIG_APP_YAML);
            log.info("load config: {}", CONFIG_APP_YAML);
            for (String key : ymlMap.keySet()) {
                log.info("key {}",key);
                Object value = ymlMap.get(key);
                if(value instanceof String strValue) {
                    props.put(key, strValue);
                }
            }
        }catch (UncheckedIOException e){
            if(e.getCause() instanceof FileNotFoundException) {
                ClassUtils.readInputStream(CONFIG_APP_PROP,(input) -> {
                    log.info("load config: {}", CONFIG_APP_PROP);
                    props.load(input);
                    return true;
                });
            }
            log.error("load config error",e);
        }

        return new PropertyResolver(props);

    }


}
