package com.example.gongtong.rxjava2.rxbus.finder;

import com.example.gongtong.rxjava2.rxbus.annotation.Produce;
import com.example.gongtong.rxjava2.rxbus.annotation.Subscribe;
import com.example.gongtong.rxjava2.rxbus.annotation.Tag;
import com.example.gongtong.rxjava2.rxbus.thread.EventThread;
import com.example.gongtong.rxjava2.rxbus.vo.ProducerEvent;
import com.example.gongtong.rxjava2.rxbus.vo.EventType;
import com.example.gongtong.rxjava2.rxbus.vo.SubscriberEvent;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/***********************************************
 * <P> 注解查找类
 * <P> Author: gongtong
 * <P> Date: 2017-06-23 下午5:11
 * <P> Copyright  2008 二维火科技
 ***********************************************/

public class AnnotatedFinder {
    /**
     * 每个类缓存  发布事件总线
     */
    private static final ConcurrentMap<Class<?>,
            Map<EventType, SourceMethod>> PRODUCERS_CHANGE =
            new ConcurrentHashMap<>();
    /**
     * 每个类  订阅事件总线
     */
    private static final ConcurrentMap<Class<?>,
            Map<EventType, Set<SourceMethod>>> SUBSCRIBERS_CHANGE =
            new ConcurrentHashMap<>();

    /**
     * 设置外部不可创建该类
     */
    private AnnotatedFinder(){

    }
    /**
     * 查找所有Produce的方法
     *
     * @param listener
     * @return
     */
    static Map<EventType, ProducerEvent> findAllProducers(Object listener) {
        final Class<?> listenerClass = listener.getClass();
        //存放所有Producer的集合
        Map<EventType, ProducerEvent> producersInMethod = new HashMap<>();

        Map<EventType, SourceMethod> methodMap = PRODUCERS_CHANGE.get(listenerClass);
        if (null == methodMap) {
            methodMap = new HashMap<>();
            loadAnnotatedProducerMethods(listenerClass, methodMap);
        }
        if (!methodMap.isEmpty()) {
            for (Map.Entry<EventType, SourceMethod> entry : methodMap.entrySet()) {
                ProducerEvent producer = new ProducerEvent(listener, entry.getValue().method, entry.getValue().thread);
                producersInMethod.put(entry.getKey(), producer);
            }
        }
        return producersInMethod;
    }


    /**
     * 查找观察者方法
     *
     * @param listener
     * @return
     */
    static Map<EventType, Set<SubscriberEvent>> finAllSubscribers(Object listener) {
        Class<?> listenerClass = listener.getClass();
        Map<EventType, Set<SubscriberEvent>> subscribersInMethod = new HashMap<>();
        Map<EventType, Set<SourceMethod>> subscriberMap = SUBSCRIBERS_CHANGE.get(listenerClass);
        if (null == subscriberMap) {
            subscriberMap = new HashMap<>();
            loadAnnotatedSubscribers(listenerClass, subscriberMap);
        }
        if (!subscriberMap.isEmpty()) {
            for (Map.Entry<EventType, Set<SourceMethod>> entry : subscriberMap.entrySet()) {
                Set<SubscriberEvent> subscribers = new HashSet<>();
                for (SourceMethod sourceMethod : entry.getValue()) {

                    subscribers.add(new SubscriberEvent(listener, sourceMethod.method, sourceMethod.thread));
                }
                subscribersInMethod.put(entry.getKey(), subscribers);
            }
        }
        return subscribersInMethod;
    }

    /**
     * 加载生产者的方法
     *
     * @param listenerClass
     * @param producesMethods
     */
    private static void loadAnnotatedProducerMethods(Class<?> listenerClass, Map<EventType, SourceMethod> producesMethods) {
        Map<EventType, Set<SourceMethod>> subscriberMethods = new HashMap<>();
        loadAnnotatedMethods(listenerClass, producesMethods, subscriberMethods);
    }

    /**
     * 加载观察者
     *
     * @param listener
     * @param subscribersMethod
     */
    private static void loadAnnotatedSubscribers(Class<?> listener, Map<EventType, Set<SourceMethod>> subscribersMethod) {
        Map<EventType, SourceMethod> producersMethod = new HashMap<>();
        loadAnnotatedMethods(listener, producersMethod, subscribersMethod);
    }

    /**
     * 加载所有的方法  到他们自己的缓存类中
     *
     * @param listenerClass
     * @param producesMethods
     * @param subscriberMethods
     */
    private static void loadAnnotatedMethods(Class<?> listenerClass, Map<EventType, SourceMethod> producesMethods, Map<EventType, Set<SourceMethod>> subscriberMethods) {
        for (Method method : listenerClass.getDeclaredMethods()) {
            if (method.isBridge()) {
                continue;
            }
            //判断当前类是不是该类的注解
            if (method.isAnnotationPresent(Subscribe.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != -1) {
                    throw new IllegalArgumentException("Method" + method + "可设置注解 " + parameterTypes.length + "    但是方法必须设置一个参数");

                }
                Class<?> parameterClazz = parameterTypes[0];
                //判断是不是接口类
                if (parameterClazz.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + "注解" + parameterClazz + "必须是一个具体的类型，不能是一个接口类");
                }
                //获取该方法的修饰符
                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method" + method + "注解" + parameterClazz + "不是public");
                }
                Subscribe annotation = method.getAnnotation(Subscribe.class);
                EventThread eventThread = annotation.thread();
                Tag[] tags = annotation.tags();
                int tagLength = (tags == null ? 0 : tags.length);
                do {
                    String tag = Tag.DEFAULT;
                    if (tagLength > 0) {
                        tag = tags[tagLength - 1].value();
                    }
                    EventType eventType = new EventType(tag, parameterClazz);
                    Set<SourceMethod> methods = subscriberMethods.get(eventType);
                    if (methods == null) {
                        methods = new HashSet<>();
                        subscriberMethods.put(eventType, methods);
                    }
                    methods.add(new SourceMethod(eventThread, method));
                    tagLength--;
                } while (tagLength > 0);

            } else if (method.isAnnotationPresent(Produce.class)) {

                Class<?>[] parameterTypes = method.getParameterTypes();
                //判断参数
                if (parameterTypes.length != 0) {
                    throw new IllegalArgumentException("Method" + method + "可设置注解" + parameterTypes.length + "注解参数要求为空");
                }
                //判断返回值
                if (method.getReturnType() == Void.class) {
                    throw new IllegalArgumentException("Method" + method + "注解必修返回为空");
                }
                Class<?> parameterClazz = method.getReturnType();
                //方法类型
                if (parameterClazz.isInterface()) {
                    throw new IllegalArgumentException("Method " + method + "注解" + parameterClazz + "必须是一个具体的类型，不能是一个接口类");
                }
                if (parameterClazz.equals(Void.TYPE)) {
                    throw new IllegalArgumentException("Method" + method + "有注解，但不能有返回值");
                }
                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method" + method + "注解" + parameterClazz + "不是public");
                }
                Produce annoatation = method.getAnnotation(Produce.class);
                EventThread eventThread = annoatation.thread();
                Tag[] tags = annoatation.tags();
                int tagLength = (tags == null ? 0 : tags.length);
                do {
                    String  tag=Tag.DEFAULT;
                    if (tagLength > 0) {
                        tag = tags[tagLength - 1].value();
                    }
                    EventType eventType = new EventType(tag, parameterClazz);
                    //判断是不是当前集合中有该produce  （通过key）
                    if (producesMethods.containsKey(eventType)) {
                        throw new IllegalArgumentException(eventType + "该事件已经被注册过了");
                    }
                    producesMethods.put(eventType, new SourceMethod(eventThread, method));
                    tagLength--;
                }while (tagLength>0);
            }
        }
        //将Produce  和subscriber存贮到map中
        PRODUCERS_CHANGE.put(listenerClass, producesMethods);
        SUBSCRIBERS_CHANGE.put(listenerClass, subscriberMethods);
    }

    private static class SourceMethod {
        private EventThread thread;
        private Method method;

        private SourceMethod(EventThread thread, Method method) {
            this.thread = thread;
            this.method = method;
        }
    }
}
