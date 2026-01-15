package org.harenoasa.summerframework.context.annotation;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.config.PropertyResolver;
import org.harenoasa.summerframework.config.ResourceResolver;
import org.harenoasa.summerframework.entity.exception.*;
import org.harenoasa.summerframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AnnotationConfigApplicationContext {
    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;
    Set<String> creatingBeanNames;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        Set<String> beanClassNames = scanForClassNames(configClass);
        beans = createBeanDefinitions(beanClassNames);

        creatingBeanNames = new HashSet<>();
        beans.values().stream()
                .filter(this::isConfigurationDefinition)
                .sorted().map(def ->{
                    createBeanAsEarlySingleton(def);
                    return def.getName();
                }).toList();
        List<BeanDefinition> defs = this.beans.values().stream()
                .filter(def -> def.getInstance() == null)
                .sorted().toList();
        defs.forEach(def -> {
            if(def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });

    }
    boolean isConfigurationDefinition(BeanDefinition def){
        return ClassUtils.getAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    public Object createBeanAsEarlySingleton(BeanDefinition def) {
        if(!creatingBeanNames.add(def.getName()))
            throw new UnsatisfiedLinkError(String.format("Circular dependency detected when create bean '%s'", def.getName()));
        Executable creatFn = def.getFactoryName() == null ? def.getConstructor() : def.getFactoryMethod();

        final Parameter[] parameters = creatFn.getParameters();
        final Annotation[][] parametersAnnos = creatFn.getParameterAnnotations();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            final Parameter param = parameters[i];
            final Annotation[] paramAnnos = parametersAnnos[i];
            final Value value = ClassUtils.getAnnotation(paramAnnos, Value.class);
            final Autowired autowired = ClassUtils.getAnnotation(paramAnnos, Autowired.class);

            final boolean isConfiguration = isConfigurationDefinition(def);
            if (isConfiguration && autowired != null)
                String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            if(value != null && autowired != null)
                throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            if (value == null && autowired == null) {
                throw new BeanCreationException(
                        String.format("Must specify @Autowired or @Value when create bean '%s': %s.", def.getName(), def.getBeanClass().getName()));
            }
            final Class<?> type = param.getType();
            if (value != null){
                args[i] = this.propertyResolver.getRequiredProperty(value.value(), type);
            } else {
                String name = autowired.name();
                boolean required = autowired.value();
                BeanDefinition dependsOnDef = name.isEmpty() ? findBeanDefinition(type) : findBeanDefinition(name, type);
                if(required && dependsOnDef == null)
                    throw new BeanCreationException(String.format("Missing autowired bean with type '%s' when create bean '%s': %s.", type.getName(),
                            def.getName(), def.getBeanClass().getName()));
                if(dependsOnDef != null) {
                    Object autowiredBeanInstance = dependsOnDef.getInstance();
                    if (autowiredBeanInstance == null && !isConfiguration){
                        autowiredBeanInstance = createBeanAsEarlySingleton(dependsOnDef);
                    }
                    args[i] = autowiredBeanInstance;
                } else {
                    args[i]= null;
                }
            }
        }

        Object instance = null;
        if( def.getFactoryName() == null){
            try{
                instance = def.getConstructor().newInstance(args);
            }catch(Exception e){
                throw new BeanCreationException(String.format("Exception when create bean '%s': %s", def.getName(), def.getBeanClass().getName()));
            }
        } else {
            Object configInstance = getBean(def.getFactoryName());
            try{
                instance = def.getFactoryMethod().invoke(configInstance, args);
            }catch(Exception e){
                throw new BeanCreationException(String.format("Exception when create bean '%s' : '%s'", def.getName(),def.getBeanClass().getName()),e);
            }
        }
        def.setInstance(instance);
        return def.getInstance();
    }

    public <T> T getBean(String name){
        BeanDefinition def = beans.get(name);
        if(def == null){
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s'.", name));
        }
        return (T) def.getRequiredInstance();
    }

    private Set<String> scanForClassNames(Class<?> configClass) {
        ComponentScan scan = ClassUtils.getAnnotation(configClass, ComponentScan.class);
        String[] scanPackages = scan == null || scan.value().length == 0 ? new String[] {configClass.getPackage().getName()} : scan.value();

        Set<String> classNameSet = new HashSet<>();
        for(String pkg: scanPackages){
            log.info("scan packae:{},pkg");
            ResourceResolver rr = new ResourceResolver(pkg);
            classNameSet.addAll(rr.scan(res -> {
                String name = res.name();
                if(name.endsWith(".class")){
                    String s = name.substring(0,name.length() -6).replace("/", ".").replace("\\", ".");
                    return s;
                }
                return null;}));
        }

        Import importConfig = configClass.getAnnotation(Import.class);
        if (importConfig != null) {
            for (Class<?> importConfigClass : importConfig.value()){
                String importClassName = importConfigClass.getName();
                classNameSet.add(importClassName);
            }
        }
        return classNameSet;
    }

    Map<String, BeanDefinition> createBeanDefinitions(Set<String> classNameSet){
        HashMap<String, BeanDefinition> defs = new HashMap<String, BeanDefinition>();
        for (String className : classNameSet) {
            Class<?> clazz = null;
            try{
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if(clazz.isAnnotation()|| clazz.isEnum() || clazz.isInterface() || clazz.isRecord())
                continue;
            Component component = ClassUtils.getAnnotation(clazz, Component.class);
            if (component != null) {
                log.info("found component: {}", clazz.getName());
                int mod = clazz.getModifiers();
                if(Modifier.isAbstract(mod))
                    throw new BeanDefinitionException("@Component class can not be abstract: " + clazz.getName() + "must not be abstract");
                if(Modifier.isPrivate(mod))
                    throw new BeanDefinitionException("@Component class can not be private: " + clazz.getName() + "must not be private");
                String beanName= ClassUtils.getBeanName(clazz);
                BeanDefinition def = new BeanDefinition(
                        beanName,
                        clazz,
                        getSuitableConstructor(clazz),
                        getOrder(clazz),
                        clazz.isAnnotationPresent(Primary.class),
                        null,
                        null,
                        ClassUtils.findAnnotationMethod(clazz, PostConstruct.class),
                        ClassUtils.findAnnotationMethod(clazz, PreDestroy.class));
                addBeanDefinitions(defs,def);

                Configuration configuration = ClassUtils.getAnnotation(clazz, Configuration.class);
                if (configuration != null)
                    scanFacotryMethods(beanName, clazz, defs);
            }

        }

        return defs;
    }

    private void scanFacotryMethods(String factoryBeanName, Class<?> clazz, HashMap<String, BeanDefinition> defs) {
        for (Method method : clazz.getDeclaredMethods()) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null){
                int mod = method.getModifiers();
                if(Modifier.isAbstract(mod))
                    throw  new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + "must not be Abstract.");
                if(Modifier.isFinal(mod))
                    throw  new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + "must not be Final.");
                if(Modifier.isPrivate(mod))
                    throw  new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + "must not be Private.");
            }
            Class<?> returnType = method.getReturnType();
            if(returnType.isPrimitive())
                throw new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + "return type must not be primitive.");
            if(returnType == void.class || returnType == Void.class)
                throw  new BeanDefinitionException("@Bean method " + clazz.getName() + "." + method.getName() + "return type must not be void.");
            addBeanDefinitions(defs , new BeanDefinition(
                    ClassUtils.getBeanName(method),
                    returnType,
                    factoryBeanName,
                    method,getOrder(method),
                    method.isAnnotationPresent(Primary.class),
                    bean.initMethod().isEmpty() ? null : bean.initMethod(),
                    bean.destroyMethod().isEmpty() ? null : bean.destroyMethod(),null,null
            ));
        }
    }

    int getOrder(Method method){
        Order order = method.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    void addBeanDefinitions(Map<String, BeanDefinition> defs, BeanDefinition def){
        if (defs.put(def.getName(), def)!= null){
            throw new BeanDefinitionException("Duplicate bean Name:" + def.getName());
        }
    }

    private int getOrder(Class<?> clazz) {
        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Integer.MAX_VALUE : order.value();
    }

    private Constructor<?> getSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] cons = clazz.getConstructors();
        if (cons.length == 0) {
            cons = clazz.getDeclaredConstructors();
            if (cons.length != 1) {
                throw new BeanDefinitionException("More than one  Constructor found in class " +  clazz.getName() + ".");
            }
        }
        if(cons.length != 1){
            throw new BeanDefinitionException("More than one public constructor found in class " + clazz.getName() +".");
        }
        return cons[0];
    }

    @Nullable
    public BeanDefinition findBeanDefinition(String name){
        return beans.get(name);
    }

    @Nullable
    public BeanDefinition findBeanDefinition(String name, Class<?> requiredType){
        BeanDefinition def = findBeanDefinition(name);
        if(def == null)return null;
        if(!requiredType.isAssignableFrom(def.getBeanClass())) {
            throw new BeanNotOfRequiredTypeException(String.format("Autowire required type '%s' but bean '%s' has actual type '%s'.", requiredType.getName(),
                    name, def.getBeanClass().getName()));
        }
    }
    public List<BeanDefinition> findBeanDefinitions(Class<?> type){
        return beans.values().stream().filter(definition -> type.isAssignableFrom(definition.getBeanClass()))
                .sorted().collect(Collectors.toList());

    }

    public BeanDefinition findBeanDefinition(Class<?> type){
        List<BeanDefinition> defs = findBeanDefinitions(type);
        if(defs.isEmpty())return null;
        if(defs.size()==1)return defs.get(0);
        List<BeanDefinition> primarys = defs.stream().filter(BeanDefinition::isPrimary).toList();
        if(primarys.size() == 1) return primarys.get(0);
        if(primarys.isEmpty()) throw new NoUniqueBeanDefinitionException(String.format("Multipl bean with type '%s' found, but no @Primary specified.",type.getName()));
        throw new NoUniqueBeanDefinitionException(String.format("Multipl bean with type '%s' found, and @Primary specified.",type.getName()));

    }
}
