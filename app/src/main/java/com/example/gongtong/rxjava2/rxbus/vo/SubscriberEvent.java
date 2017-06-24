package com.example.gongtong.rxjava2.rxbus.vo;

import com.example.gongtong.rxjava2.rxbus.thread.EventThread;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


/***********************************************
 * <P>   订阅者事件
 * <P> Author: gongtong
 * <P> Date: 2017-06-23 下午7:03
 * <P> Copyright  2008 二维火科技
 ***********************************************/
public class SubscriberEvent extends Event {

    private final Object target;
    /**
     * 订阅者方法
     */
    private final Method method;
    /**
     * 订阅者的线程
     */
    private final EventThread thread;
    /**
     * RxJava {@link Subject}
     */
    private Subject subject;
    /**
     * 订阅者是否会接受到事件
     */
    private boolean valid = true;

    /**
     * object hashCode
     */
    private final int hashCode;

    public SubscriberEvent(Object target, Method method, EventThread thread) {
        if (target == null) {
            throw new NullPointerException("SubscriberEvent target  can not null");
        }
        if (method == null) {
            throw new NullPointerException("SubscriberEvent method can not null");
        }
        if (thread == null) {
            throw new NullPointerException("SubscriberEvent thread  can not null");
        }
        this.target = target;
        this.method = method;
        this.thread = thread;
        //提高java的反射速度
        this.method.setAccessible(true);
        initObservable();
        /**
         * 开始的hashcode值
         * 计算hashcode进行返回
         */
        final int prime=31;
        hashCode = (prime + method.hashCode()) * prime + target.hashCode();
    }

    /**
     * 初始化Observable
     */
    private void initObservable() {
        subject = PublishSubject.create();
        //指定观察者的线程
        subject.observeOn(EventThread.getSchedler(this.thread))
                .subscribe(new Consumer() {
                    @Override
                    public void accept(@NonNull Object object) throws Exception {
                        try {
                            if (valid) {
                                handlerEvent(object);
                            }
                        } catch (InvocationTargetException e) {
                            throwRuntimeException("Count not send event" + object.getClass() + "to  Subscriber" + SubscriberEvent.this, e);
                        }
                    }
                });
    }

    /**
     * 进行订阅的处理
     */
    private void handlerEvent(Object event) throws InvocationTargetException {
        if (!valid) {
            throw new IllegalStateException(toString() + "消息失效,无法进行处理");
        }
        try {
            method.invoke(target, event);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw e;
        }
    }

    /**
     * 是否有效
     * @return
     */
     public  boolean isValid(){
         return valid;
     }
    /**
     * 如果无效则就不在进行处理事件（由外部调用）
     */
    public void  invalidate(){
        valid=false;
    }

    /**
     *
     * @return
     */
    public Subject getSubject(){
        return subject;
    }
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj){
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SubscriberEvent other = (SubscriberEvent) obj;

        return method.equals(other.method) && target == other.target;
    }

    /**
     * 将消息发送 出去
     * @param event
     */
    public void handle(Object event) {
        subject.onNext(event);

    }
}
