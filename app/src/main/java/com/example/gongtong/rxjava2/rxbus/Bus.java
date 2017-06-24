package com.example.gongtong.rxjava2.rxbus;


import com.example.gongtong.rxjava2.rxbus.annotation.Produce;
import com.example.gongtong.rxjava2.rxbus.annotation.Subscribe;
import com.example.gongtong.rxjava2.rxbus.annotation.Tag;
import com.example.gongtong.rxjava2.rxbus.finder.Finder;
import com.example.gongtong.rxjava2.rxbus.thread.ThreadEnforcer;
import com.example.gongtong.rxjava2.rxbus.vo.DeadEvent;
import com.example.gongtong.rxjava2.rxbus.vo.EventType;
import com.example.gongtong.rxjava2.rxbus.vo.ProducerEvent;
import com.example.gongtong.rxjava2.rxbus.vo.SubscriberEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

/***********************************************
 * <P> desc:  事件调度器 ,给监听者提供的注册方法
 *            允许组件间使用 订阅者----发布者 方式 不需要明确的注册（彼此了解） 专为替换而设计
 *            传统的Android进程间通信使用显示注册和监听，它不是一个通用的发布订阅系统，也不能用于进程间通信
 *            接收事件：要接收事件，一个对象应该使用一个公开的方法（事件订阅者），且必须含有一个接收事件的参数
 *            使用注解：通过它去找 对应注册的一个方法
 * <P> Author: gongtong
 * <P> Date: 2017-06-24 上午11:38
 * <P> Copyright  2008 二维火科技
 ***********************************************/

public class Bus {
    private static final String DEFAULT_IDENTIFIER = "default";
    /**
     * 所有注册的观察者   按照事件类型索引
     */
    private final ConcurrentMap<EventType, Set<SubscriberEvent>>
            subscribersByType = new ConcurrentHashMap<>();
    /**
     * 所有注册的producer  按照事件类型索引
     */
    private final ConcurrentMap<EventType, ProducerEvent>
            producersByType = new ConcurrentHashMap<>();
    /**
     * 用于区分事件总线的标示符
     */
    private final String identifier;
    /**
     * 对事件进行  发送  注解 和解注册
     */
    private final ThreadEnforcer enforcer;
    /**
     * 用于查找订阅者的注册和解注册
     */
    private final Finder finder;

    private final ConcurrentMap<Class<?>, Set<Class<?>>> flattenHierarchyCache
            = new ConcurrentHashMap<>();

    /**
     * 创建一个default 总线，执行主线程上的操作
     */
    public Bus() {
        this(DEFAULT_IDENTIFIER);
    }

    /**
     * 使用给定的标示符创建事件总线，强制对主线程执行操作
     *
     * @param identifier 总线的别名，用于调试
     */
    public Bus(String identifier) {
        this(ThreadEnforcer.MAIN, identifier);
    }

    /**
     * 创建一个新的default总线 @Code enforcer
     *
     * @param enforcer 注册，解注册和发送事件调节器
     */
    public Bus(ThreadEnforcer enforcer) {
        this(enforcer, DEFAULT_IDENTIFIER);
    }

    /**
     * 创建一个标示符@code  identifier的总线
     *
     * @param enforcer   注册，解注册和发送事件调节器
     * @param identifier 总线的别名，用于调试
     */
    public Bus(ThreadEnforcer enforcer, String identifier) {
        this(enforcer, identifier, Finder.ANNOTATED);
    }

    /**
     * 构造函数   允许替换默认的finder
     *
     * @param enforcer   注册，解注册和发送事件调节器
     * @param identifier 总线的别名，用于调试
     * @param finder     用于在注册和解注册当中发现订阅者和发布者
     */
    public Bus(ThreadEnforcer enforcer, String identifier, Finder finder) {
        this.enforcer = enforcer;
        this.identifier = identifier;
        this.finder = finder;
    }

    @Override
    public String toString() {
        return "[Bus \"" + identifier + "\"]";
    }

    public void register(Object object) {
        if (object == null) {
            throw new NullPointerException("object to register must not be null");
        }
        enforcer.enforce(this);
        Map<EventType, ProducerEvent> foundProducers = finder.findAllProducers(object);
        for (EventType type : foundProducers.keySet()) {
            final ProducerEvent producer = foundProducers.get(type);
            //putIfAbsent  通过key查找该值存不存在，如果存在则返回value值，如果不存在返回null
            ProducerEvent previousProducer = producersByType.putIfAbsent(type, producer);
            //判断以前的Produce 存不存在
            if (previousProducer != null) {
                throw new IllegalArgumentException("Produce method for type" + type +
                        "该类型的Produce已经通过" + producer.getTarget().getClass() + "注册");
            }
            Set<SubscriberEvent> subscribers = subscribersByType.get(type);
            if (subscribers != null && subscribers.isEmpty()) {
                for (SubscriberEvent subscriberEvent : subscribers) {
                    disPatchProducerResult(subscriberEvent, producer);
                }
            }
        }

        Map<EventType, Set<SubscriberEvent>> foundSubscribesMap = finder.findAllSubscribers(object);
        for (EventType eventType : foundSubscribesMap.keySet()) {
            Set<SubscriberEvent> subscribes = subscribersByType.get(eventType);
            if (subscribes == null) {
                //线程安全
                Set<SubscriberEvent> subscribersCreation = new CopyOnWriteArraySet<>();
                //putIfAbsent  通过key查找该值存不存在，如果存在则返回value值，如果不存在返回null
                subscribes = subscribersByType.putIfAbsent(eventType, subscribersCreation);
                if (subscribes == null) {
                    subscribes = subscribersCreation;
                }
            }
            final Set<SubscriberEvent> foundSubscribers = foundSubscribesMap.get(eventType);
            if (!subscribes.addAll(foundSubscribers)) {
                throw new IllegalArgumentException("this is already  register");
            }
        }

        for (Map.Entry<EventType, Set<SubscriberEvent>> entry : foundSubscribesMap.entrySet()) {
            EventType type = entry.getKey();
            ProducerEvent producer = producersByType.get(type);
            if (producer != null && producer.isValid()) {
                Set<SubscriberEvent> subscriberEvents = entry.getValue();
                for (SubscriberEvent subscriberEvent : subscriberEvents) {
                    if (!producer.isValid()) {
                        break;
                    }
                    if (subscriberEvent.isValid()) {
                        disPatchProducerResult(subscriberEvent, producer);
                    }
                }
            }
        }
    }

    public void unregister(Object object) {
        if (object == null) {
            throw new NullPointerException("object to unregister must not be null");
        }
        enforcer.enforce(this);
        Map<EventType, ProducerEvent> producerInListener = finder.findAllProducers(object);
        for (Map.Entry<EventType, ProducerEvent> entry : producerInListener.entrySet()) {
            final EventType eventType = entry.getKey();
            //查找当前有没有的producer
            ProducerEvent producer = producersByType.get(eventType);
            ProducerEvent value = entry.getValue();
            if (value == null || !value.equals(producer)) {
                throw new IllegalArgumentException("当前的对象已经解除注册或者为注册");
            }
            //在当前的集合中删除Produce
            producersByType.remove(eventType).invalidate();
        }
        Map<EventType, Set<SubscriberEvent>> subscriberInListener = finder.findAllSubscribers(object);
        for (Map.Entry<EventType, Set<SubscriberEvent>> entry : subscriberInListener.entrySet()) {
            //得到当前的观察者
            Set<SubscriberEvent> currentSubscribers = subscribersByType.get(entry.getKey());
            Collection<SubscriberEvent> eventMethodInListener = entry.getValue();
            if (currentSubscribers == null || !currentSubscribers.containsAll(eventMethodInListener)) {
                throw new IllegalArgumentException("当前的观察者为空或者为注册观察者");
            }
            for (SubscriberEvent subscriberEvent : currentSubscribers) {
                if (eventMethodInListener.contains(subscriberEvent)) {
                    subscriberEvent.invalidate();
                }
            }
            currentSubscribers.removeAll(eventMethodInListener);
        }
    }

    /**
     * 向所有观察者发送消息  事件发布后，将会返回订阅者的任何信息（异常信息）
     * 如果事件不能正常发布 将会保存在DeadEvent(前提是该事件不在DeadEvent中)
     *
     * @param event
     */
    public void post(Object event) {
        post(Tag.DEFAULT, event);
    }

    /**
     * 向所有观察者发送消息  事件发布后，将会返回订阅者的任何信息（异常信息）
     * 如果事件不能正常发布 将会保存在DeadEvent(前提是该事件不在DeadEvent中)
     *
     * @param tag
     * @param event
     */
    private void post(String tag, Object event) {
        if (event == null) {
            throw new NullPointerException("Event must not be null");
        }
        enforcer.enforce(this);
        Set<Class<?>> dispatchClasses = flattenHierarchy(event.getClass());

        boolean dispatched = false;
        for (Class<?> clazz : dispatchClasses) {
            Set<SubscriberEvent> wrappers = subscribersByType.get(new EventType(tag, clazz));
            if (wrappers != null && !wrappers.isEmpty()) {
                dispatched = true;
                for (SubscriberEvent wrapper : wrappers) {
                    disPatch(event, wrapper);
                }
            }
        }
        if (!dispatched && !(event instanceof DeadEvent)) {
            post(new DeadEvent(this, event));
        }

    }

    /**
     * 将类的层次结构映射为一组class对象，set集合将包含这些类的所有接口
     * @param concreteClass  对象类结构
     * @return
     */
    private Set<Class<?>> flattenHierarchy(Class<?> concreteClass) {
        Set<Class<?>> classes = flattenHierarchyCache.get(concreteClass);
        if (classes == null) {
            Set<Class<?>> classesCreation = getClassFor(concreteClass);
            //判断当前有缓存信息
            classes = flattenHierarchyCache.putIfAbsent(concreteClass, classesCreation);
            if (classes == null) {
                classes = classesCreation;
            }
        }
        return classes;
    }

    private Set<Class<?>> getClassFor(Class<?> concreteClass) {

        List<Class<?>> parents = new LinkedList<>();
        Set<Class<?>> classes = new HashSet<>();
        parents.add(concreteClass);
        while (!parents.isEmpty()) {
            Class<?> clazz = parents.remove(0);
            classes.add(clazz);
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                parents.add(parent);
            }

        }
        return classes;
    }

    /**
     * 创建RxJava 系列观察事件
     *
     * @param subscriberEvent
     * @param producer
     */
    private void disPatchProducerResult(final SubscriberEvent subscriberEvent, ProducerEvent producer) {
        producer.produce().subscribe(new Consumer() {
            @Override
            public void accept(@NonNull Object event) throws Exception {
                if (event != null) {
                    disPatch(event, subscriberEvent);
                }
            }
        });
    }

    /**
     * 事件分发给订阅者   这个方法是通过异步的形式发送
     *
     * @param event           事件发送
     * @param subscriberEvent 包装器  将调用handler
     */
    private void disPatch(Object event, SubscriberEvent subscriberEvent) {
        if (subscriberEvent.isValid()) {
            subscriberEvent.handle(event);//发送消息
        }
    }
}
