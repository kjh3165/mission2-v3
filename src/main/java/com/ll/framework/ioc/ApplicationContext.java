package com.ll.framework.ioc;

import com.ll.framework.ioc.annotations.Repository;
import com.ll.framework.ioc.annotations.Service;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.*;

public class ApplicationContext {
    private Map<Class<?>, Object> beans = new HashMap<>();
    private Map<String, Class<?>> beanNames = new HashMap<>();
    private Reflections reflections;
    private Set<Class<?>> repositories;
    private Set<Class<?>> services;

    public ApplicationContext(String basePackage) {
        reflections = new Reflections(basePackage);
        repositories = reflections.getTypesAnnotatedWith(Repository.class);
        services = reflections.getTypesAnnotatedWith(Service.class);
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
        if (repositories.contains(type) || services.contains(type)) {
            return createBean(type);
        }

        throw new RuntimeException("No bean for type: " + type);
    }

    // 빈 이름(String)으로 빈 클래스 얻기
    private String getBeanName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
