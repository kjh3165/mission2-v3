package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Bean;
import com.ll.framework.ioc.annotations.Configuration;
import com.ll.framework.ioc.annotations.Repository;
import com.ll.framework.ioc.annotations.Service;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ApplicationContext {
    private Map<Class<?>, Object> beans = new HashMap<>();
    private Map<String, Class<?>> beanNames = new HashMap<>();
    private Reflections reflections;
    private Set<Class<?>> repositories;
    private Set<Class<?>> services;
    private Set<Class<?>> configs;

    public ApplicationContext(String basePackage) {
        reflections = new Reflections(basePackage);
        repositories = reflections.getTypesAnnotatedWith(Repository.class);
        services = reflections.getTypesAnnotatedWith(Service.class);
        configs = reflections.getTypesAnnotatedWith(Configuration.class);
    }

    public void init() {
        // Repository 어노테이션을 가진 클래스들의 빈 생성
        for (Class<?> clazz : repositories) {
            createBean(clazz);
            beanNames.put(getBeanName(clazz), clazz);
        }
        // Service 어노테이션을 가진 클래스들의 빈 생성
        for (Class<?> clazz : services) {
            createBean(clazz);
            beanNames.put(getBeanName(clazz), clazz);
        }
        // Configuration 어노테이션을 가진 클래스들의 빈 생성, 그 후 내부에 @Bean 어노테이션을 가진 메서드들로 빈 생성
        for (Class<?> clazz : configs) {
            createBean(clazz);
            beanNames.put(getBeanName(clazz), clazz);
            createConfigBean(clazz);
        }
    }

    // 빈 얻어오기
    public <T> T genBean(String beanName) {
        Class<?> clazz = beanNames.get(beanName);

        if (clazz == null) {
            throw new RuntimeException("No bean found: " + beanName);
        }

        return (T) beans.get(clazz);
    }

    // 빈 생성하기
    public Object createBean(Class<?> clazz) {
        // 이미 있으면 반환
        if (beans.containsKey(clazz)) {
            return beans.get(clazz);
        }
        // 생성자를 찾고 필요한 매개변수를의 빈을 이용해 생성
        try {
            Constructor<?> constructor = findConstructor(clazz);
            Class<?>[] paramTypes = constructor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = getBeanByType(paramTypes[i]);
            }

            Object instance = constructor.newInstance(params);
            beans.put(clazz, instance);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 생성자 찾기
    private Constructor<?> findConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        // 생성자가 1개일 경우 바로 반환
        if (constructors.length == 1) {
            return constructors[0];
        }
        // 생성자가 여러 개면 가장 매개변수가 많은 생성자를 선택
        return Arrays.stream(constructors)
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
    }

    // 매개변수 타입으로 빈 찾기(없으면 생성)
    private Object getBeanByType(Class<?> type) {
        // 있으면 반환
        if (beans.containsKey(type)) {
            return beans.get(type);
        }
        // 없으면 생성
//        if (repositories.contains(type) || services.contains(type)) {
//            return createBean(type);
//        }
//
//        throw new RuntimeException("No bean for type: " + type);
        return createBean(type);
    }

    // 빈 이름(String)으로 빈 클래스 얻기
    private String getBeanName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private void createConfigBean(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            // @Bean 어노테이션을 가진 메서드들로 빈 생성
            if (method.isAnnotationPresent(Bean.class)) {
                createBeanByMethod(method, clazz);
                String methodName = method.getName();
                beanNames.put(methodName, method.getReturnType());
            }
        }
    }

    // 메서드를 통해 빈 생성
    private Object createBeanByMethod(Method method, Class<?> clazz) {
        // 이미 있으면 반환
        if (beans.containsKey(method.getReturnType())) {
            return beans.get(method.getReturnType());
        }
        // 메서드 실행 후 반환값을 빈으로 등록
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = getBeanByType(paramTypes[i]);
            }

            Object instance = method.invoke(getBeanByType(clazz), params);
            beans.put(instance.getClass(), instance);
            return instance;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
