package org.harenoasa.summerframework.util;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YamlUtils {
    public static Map<String, Object> loadYaml(String path){
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        Representer representer = new Representer(dumperOptions);
        NoImplicitResolver resolver = new NoImplicitResolver();
        Yaml yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
        return ClassPathUtils.readInputStream(path, yaml::load);
    }
    public static Map<String ,Object> loadYamlAsPlainMap(String path){
        Map<String, Object> data = loadYaml(path);
        LinkedHashMap<String, Object> plain = new LinkedHashMap<>();
        convertTo(data,"",plain);
        return plain;
    }

    public static void convertTo(Map<String,Object> source,String prefix,Map<String,Object> plain){
        source.keySet().forEach(k -> {
            Object value = source.get(k);
            if(value instanceof Map){
                Map<String,Object> subMap = Map.class.cast(value);
                convertTo(subMap,prefix + k + ".",plain);
            } else if(value instanceof List){
                plain.put(prefix + k, value);
            } else {
                plain.put(prefix + k, value.toString());
            }
        });
    }


}
class NoImplicitResolver extends Resolver {
    public NoImplicitResolver(){
        super();
        yamlImplicitResolvers.clear();
    }
}