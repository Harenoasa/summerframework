package org.harenoasa.summerframework.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.harenoasa.summerframework.entity.PropertyExpr;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.function.Function;

public class PropertyResolver {

    Map<String, String> properties = new HashMap<>();
    Map<Class<?>, Function<String, Object>> converters = new HashMap<>();

    public PropertyResolver(Properties props) {
        this.properties.putAll(System.getenv());
        props.forEach((key, value) -> this.properties.put(key.toString(), value.toString()));
        ArrayList<String> keys = new ArrayList<>(properties.keySet());
        Collections.sort(keys);
        keys.forEach(key -> System.out.printf("PropertyResolver: {%s} = {%s}",key,properties.get(key)));
        converters.put(String.class, s -> s);
        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);
        converters.put(byte.class, Byte::parseByte);
        converters.put(Byte.class, Byte::valueOf);
        converters.put(short.class, Short::parseShort);
        converters.put(Short.class, Short::valueOf);
        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);
        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);
        converters.put(float.class, Float::parseFloat);
        converters.put(Float.class, Float::valueOf);
        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);
        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Duration.class,Duration::parse );
        converters.put(ZoneId.class, ZoneId::of);
    }
    public boolean containsProperty(String key){
        return properties.containsKey(key);
    }
    public String getProperty(String key){
        PropertyExpr keyExpr = parsePropertyExpr(key);
        String property;
        if (keyExpr != null)
            property = getRquiredProperty(keyExpr.key(),keyExpr.defaultValue());
        else
            property = getRquiredProperty(key);
        String value = properties.get(key);
        if (value != null)
            return parseValue(value);
        return value;
    }
    public <T> T getProperty(String key, Class<T> type){

    }
    public String getRquiredProperty(String key,String defaultValue){
        String value = properties.get(key);
        return value != null ? value : defaultValue;
    }
    public String getRquiredProperty(String key){
        return properties.get(key);
    }

    PropertyExpr parsePropertyExpr(String key){
        if (key.startsWith("${") && key.endsWith("}")) {
            int n = key.indexOf(':');
            if (n == (-1)) {
                String k = key.substring(2, key.length() - 1);
                return new PropertyExpr(k,null);
            } else {
                String k = key.substring(2, n);
                return new PropertyExpr(k,key.substring(n+1, key.length() - 1));
            }
        }
        throw new IllegalArgumentException("Invalid arguments: " + key);
    }

    String parseValue(String value){
        PropertyExpr expr = parsePropertyExpr(value);
        if (expr == null ){
            return value;
        }
        if(expr.defaultValue() != null){
            return getRquiredProperty(expr.key(), expr.defaultValue());
        } else {
            return getRquiredProperty(expr.key());
        }
    }


}
