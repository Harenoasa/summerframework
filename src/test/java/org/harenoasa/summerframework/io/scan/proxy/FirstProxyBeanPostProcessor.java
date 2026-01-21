package org.harenoasa.summerframework.io.scan.proxy;

import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.context.annotation.BeanPostProcessor;
import org.harenoasa.summerframework.context.annotation.Component;
import org.harenoasa.summerframework.context.annotation.Order;

import java.util.HashMap;
import java.util.Map;

@Order(100)
@Component
@Slf4j
public class FirstProxyBeanPostProcessor implements BeanPostProcessor {


    Map<String, Object> originBeans = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (OriginBean.class.isAssignableFrom(bean.getClass())) {
            log.debug("create first proxy for bean '{}': {}", beanName, bean);
            var proxy = new FirstProxyBean((OriginBean) bean);
            originBeans.put(beanName, bean);
            return proxy;
        }
        return bean;
    }

    @Override
    public Object postProcessOnSetProperty(Object bean, String beanName) {
        Object origin = originBeans.get(beanName);
        if (origin != null) {
            log.debug("auto set property for {} from first proxy {} to origin bean: {}", beanName, bean, origin);
            return origin;
        }
        return bean;
    }


}
