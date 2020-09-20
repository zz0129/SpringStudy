package com.ly.framework;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LyApplicationContext {

    private Class configClass;
    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();
    // 单例池里面可以有两个名字相同但是类型不同的bean， 所以需要找的时候先根据Type寻找
    private Map<String, Object> singletonObjectMap = new ConcurrentHashMap<String, Object>();

    public LyApplicationContext(Class configClass) {
        this.configClass = configClass;
        //扫描 --> BeanDefinition
        scan(configClass);
        //创建bean 单例非懒加载
        instanceNonLazySingletonBean();
    }

    //应该先按照type去找类
    public Object getBean(String beanName){
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if("singleton".equals(beanDefinition.getScope())) {
            if(singletonObjectMap.containsKey(beanName)) {
                return singletonObjectMap.get(beanName);
            } else {
                return addSingleton(beanName, beanDefinition);
            }
        } else {
            return createBean(beanName, beanDefinition);
        }
    }

    private void instanceNonLazySingletonBean() {
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(!beanDefinition.isLazy() && "singleton".equals(beanDefinition.getScope())) {
                addSingleton(beanName, beanDefinition);
            }
        }
    }

    private void scan(Class configClass) {
        if(configClass.isAnnotationPresent(ComponentScan.class)){
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScanAnnotation.value();
            path = path.replace(".", "/");
            ClassLoader classLoader = LyApplicationContext.class.getClassLoader();
            // file:/Users/zzly/IDEA/SpringStudy/target/classes/com/ly/service
            URL resource = classLoader.getResource(path);
            // /Users/zzly/IDEA/SpringStudy/target/classes/com/ly/service
            File file = new File(resource.getFile());
            for(File f : file.listFiles()) {
                System.out.println("file =" + f);
                String fileName = f.getAbsolutePath();
                if(fileName.endsWith(".class")) {
                    String s = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    s = s.replace("/", ".");

                    try {
                        Class<?> clazz = classLoader.loadClass(s);
                        System.out.println("class = " + clazz);
                        if(clazz.isAnnotationPresent(Component.class)) {
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setBeanClass(clazz);

                            Component component = clazz.getAnnotation(Component.class);
                            String beanName = component.value();

                            if(clazz.isAnnotationPresent(Lazy.class)) {
                                beanDefinition.setLazy(true);
                            }

                            if(clazz.isAnnotationPresent(Scope.class)) {
                                beanDefinition.setScope(clazz.getAnnotation(Scope.class).value());
                            } else {
                                beanDefinition.setScope("singleton");
                            }

                            beanDefinitionMap.put(beanName, beanDefinition);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private Object addSingleton(String beanName, BeanDefinition beanDefinition) {
            Object bean = createBean(beanName, beanDefinition);
            singletonObjectMap.put(beanName, bean);
            return bean;
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class beanClass = beanDefinition.getBeanClass();
        try {
            //反射创建类
            Object instance = beanClass.getDeclaredConstructor().newInstance();
            //依赖注入 bean的生命周期
            for(Field field : beanClass.getDeclaredFields()) {
                if(field.isAnnotationPresent(Autowired.class)) {
                    String fieldName = field.getName();
                    System.out.println("filedName = " + fieldName);
                    Object bean = getBean(fieldName);
                    field.setAccessible(true);
                    field.set(instance, bean);
                }
            }

            //回调函数 回调把beanName赋值给属性,
            //使用场景：Dubbo向注册中心注册时服务名称的获取
            if(instance instanceof BeanNameAware) {
                ((BeanNameAware)instance).setBeanName(beanName);
            }

            //bean注入完成后操作
            if(instance instanceof InitializingBean) {
                ((InitializingBean)instance).afterPropertiesSet();
            }
            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Map建的选择和获取
}
