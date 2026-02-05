package org.harenoasa.summerframework.context.annotation;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.config.PropertyResolver;
import org.harenoasa.summerframework.config.ResourceResolver;
import org.harenoasa.summerframework.context.ApplicationContextUtils;
import org.harenoasa.summerframework.context.ConfigurableApplicationContext;
import org.harenoasa.summerframework.entity.exception.*;
import org.harenoasa.summerframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class AnnotationConfigApplicationContext implements ConfigurableApplicationContext {
    protected final PropertyResolver propertyResolver;
    protected final Map<String, BeanDefinition> beans;
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    private Set<String> creatingBeanNames;

    public AnnotationConfigApplicationContext(Class<?> configClass, PropertyResolver propertyResolver) {
        ApplicationContextUtils.setApplicationContext(this);
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
        List<BeanPostProcessor> processors = beans.values().stream()
                .filter(this::isBeanPostProcessorDefinition)
                .sorted()
                .map(def -> (BeanPostProcessor) createBeanAsEarlySingleton(def)).toList();
        beanPostProcessors.addAll(processors);
        createNormalBeans();

        beans.values().forEach(this::injectBean);
        beans.values().forEach(this::initBean);

    }
    void createNormalBeans(){
        List<BeanDefinition> defs = this.beans.values().stream()
                .filter(def -> def.getInstance() == null)
                .sorted().toList();
        defs.forEach(def -> {
            if(def.getInstance() == null) {
                createBeanAsEarlySingleton(def);
            }
        });
    }
    boolean isBeanPostProcessorDefinition(BeanDefinition def){
        return BeanPostProcessor.class.isAssignableFrom(def.getBeanClass());
    }

    void callMethod(Object beanInstance, Method method, String namedMethod){
        if (method != null){
            try{
                method.invoke(beanInstance);
            }catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }else if( namedMethod!= null){
            Method named = ClassUtils.getNamedMethod(beanInstance.getClass(), namedMethod);
            named.setAccessible(true);
            try{
                named.invoke(beanInstance);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
    void initBean(BeanDefinition def){
        callMethod(def.getInstance(), def.getInitMethod(), def.getInitMethodName());
    }

    void injectBean(BeanDefinition def){
        Object beanInstance = getProxiedInstance(def);
        try {
            injectProperties(def, def.getBeanClass(), beanInstance);
        } catch (ReflectiveOperationException e) {
            throw new BeanCreationException(e);
        }
    }

    private Object getProxiedInstance(BeanDefinition def){
        Object beanInstance = def.getInstance();
        List<BeanPostProcessor> reversedBeanPostProcessors = new ArrayList<>(beanPostProcessors);
        Collections.reverse(reversedBeanPostProcessors);
        for (BeanPostProcessor beanPostProcessor : reversedBeanPostProcessors) {
            Object restoredInstance = beanPostProcessor.postProcessOnSetProperty(beanInstance, def.getName());
            if (restoredInstance != beanInstance) {
                beanInstance = restoredInstance;
                log.debug("BeanPostProcessor {} specified injection from {} to {}.", beanPostProcessor.getClass().getSimpleName(),
                        beanInstance.getClass().getSimpleName(), restoredInstance.getClass().getSimpleName());
            }
        }
        return beanInstance;
     }
    private void injectProperties(BeanDefinition def, Class<?> clazz, Object bean) throws ReflectiveOperationException {
        for (Field f : clazz.getDeclaredFields()) {
            tryInjectProperties(def, clazz, bean, f);
        }
        for (Method m : clazz.getDeclaredMethods()) {
            tryInjectProperties(def, clazz, bean, m);
        }
        Class<?> superClazz = clazz.getSuperclass();
        if(superClazz != null){
            injectProperties(def, superClazz , bean);
        }

    }

    private void tryInjectProperties(BeanDefinition def, Class<?> clazz, Object bean, AccessibleObject acc) throws IllegalAccessException, InvocationTargetException {
        Value value = acc.getAnnotation(Value.class);
        Autowired autowired = acc.getAnnotation(Autowired.class);
        if(value == null && autowired == null)return ;

        Field field = null;
        Method method = null;
        if(acc instanceof Field f){
            checkFieldOrMethod(f);
            f.setAccessible(true);
            field = f;
        }
        if(acc instanceof Method m){
            checkFieldOrMethod(m);
            if (m.getParameters().length != 1) {
                throw new BeanDefinitionException(
                        String.format("Cannot inject a non-setter method %s for bean '%s': %s", m.getName(), def.getName(), def.getBeanClass().getName()));
            }
            m.setAccessible(true);
            method = m;
        }

        String accessibleName = field != null ? field.getName() : method.getName();
        Class<?> accessibleType = field != null ? field.getType() : method.getParameterTypes()[0];

        if (value != null && autowired != null) {
            throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
        }

        if(value !=null )
        {
            if(autowired != null) throw new BeanCreationException(String.format("Cannot specify both @Autowired and @Value when inject %s.%s for bean '%s': %s",
                    clazz.getSimpleName(), accessibleName, def.getName(), def.getBeanClass().getName()));
            Object propValue = propertyResolver.getRequiredProperty(value.value(), accessibleType);
            if (field != null) {
                log.debug("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, propValue);
                field.set(bean, propValue);
            }
            if (method != null) {
                log.debug("Method injection: {}.{} ({})", def.getBeanClass().getName(), accessibleName, propValue);
                method.invoke(bean, propValue);
            }

        }else if(autowired != null){
            String name = autowired.name();
            boolean required = autowired.value();
            Object depends = name.isEmpty() ? findBean(accessibleType) : findBean(accessibleType, name);
            if(required && depends == null)
                throw new UnsatisfiedDependencyException(String.format("Dependency bean not found when inject %s.%s for bean '%s': %s", clazz.getSimpleName(),
                        accessibleName, def.getName(), def.getBeanClass().getName()));
            if(depends != null){
                if (field != null){
                    field.set(bean, depends);
                    log.debug("Field injection: {}.{} = {}", def.getBeanClass().getName(), accessibleName, depends);
                }
                if(method != null){
                    method.invoke(bean,depends);
                    log.debug("Method injecttion , {}.{} = {}",def.getBeanClass().getName(), accessibleName, depends);
                }
            }
        }
    }
    protected <T> T findBean(Class<T> requiredType, String name){
        BeanDefinition def = findBeanDefinition(name, requiredType);
        if(def == null)return null;
        return (T) def.getRequiredInstance();
    }

    protected <T> T findBean(Class<T>requiredType){
        BeanDefinition def  = findBeanDefinition(requiredType);
        if(def == null){
            return null;
        }
        return (T) def.getRequiredInstance();
    }


    private void checkFieldOrMethod(Member m) {
        int mod = m.getModifiers();
        if (Modifier.isStatic(mod))
            throw new BeanDefinitionException("Cannot inject static method: "+ m);
        if (Modifier.isFinal(mod)) {
            if (m instanceof Field field)
                throw new BeanDefinitionException("Cannot inject final field: " + field);
        }
        if (m instanceof Method method) {
            log.warn(
                    "Inject final method should be careful because it is not called on target bean when bean is proxied and may cause NullPointerException.");
        }
    }

    boolean isConfigurationDefinition(BeanDefinition def){
        return ClassUtils.getAnnotation(def.getBeanClass(), Configuration.class) != null;
    }

    /**
     * 提供形式参数
     * @param def
     * @return
     */
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
                String.format("Cannot specify @Autowired when create @Configuration bean '%s': %s.", def.getName(), def.getBeanClass().getName());
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
        if(def.getFactoryName() == null){
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
                e.printStackTrace();
                throw new BeanCreationException(String.format("Exception when create bean '%s' : '%s'", def.getName(),def.getBeanClass().getName()));
            }
        }
        def.setInstance(instance);

        for (BeanPostProcessor processor : beanPostProcessors) {
            Object processed = processor.postProcessBeforeInitialization(def.getInstance(), def.getName());
            if (processed == null) {
                throw new BeanCreationException(String.format("PostBeanProcessor returns null when process bean '%s' by %s", def.getName(), processor));
            }
            if (def.getInstance() != processed) {
                def.setInstance(processed);
            }
        }

        return def.getInstance();
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        T t = findBean(requiredType, name);
        if(t == null)
            throw new NoSuchBeanDefinitionException(String.format("No bean defined with name '%s' and type '%s'.", name, requiredType));
        return t;
    }

    public <T> T getBean(Class<T> requiredType){
        BeanDefinition def = beans.get(ClassUtils.getBeanName(requiredType));
        return (T) def.getRequiredInstance();

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
//                log.info("found component: {}", clazz.getName());
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
        return def;
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


    @Override
    public boolean containsBean(String name) {
        return beans.containsKey(name);
    }



    @Override
    public void close() {
        log.info("closing{}...",getClass().getName());
        beans.values().forEach(def -> {
            Object beanInstance = getProxiedInstance(def);
            callMethod(beanInstance, def.getDestroyMethod(), def.getDestroyMethodName());
        });
        beans.clear();
        ApplicationContextUtils.setApplicationContext(null);
    }
}
