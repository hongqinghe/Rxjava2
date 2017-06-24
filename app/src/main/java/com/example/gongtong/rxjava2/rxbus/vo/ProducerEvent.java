package com.example.gongtong.rxjava2.rxbus.vo;

import com.example.gongtong.rxjava2.rxbus.thread.EventThread;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.annotations.NonNull;


/***********************************************
 * <P> 被观察者类型
 * <P> Author: gongtong
 * <P> Date: 2017-06-23 下午7:56
 * <P> Copyright  2008 二维火科技
 ***********************************************/

public class ProducerEvent extends Event {
    /**
     * produce
     */
    private final Object target;
    /**
     * produce 的方法
     */
    private final Method method;
    /**
     * produce 的线程
     */
    private final EventThread thread;
    /**
    *  是否能进行发送事件
    */
    private boolean valid=true;

    private final int hashcode;
    public ProducerEvent(Object target, Method method, EventThread thread) {
        if (target==null){
            throw new NullPointerException("ProducerEvent target  can not be null");
        }
        if (method == null) {
            throw new NullPointerException("ProducerEvent method can not be null");
        }
        this.target = target;
        this.method = method;
        this.thread = thread;
        //设置为提高java发射速度
        method.setAccessible(true);
        /**
         * 开始的hashcode值
         * 计算hashcode进行返回
         */
        final int prime = 31;
        this.hashcode = (prime + method.hashCode()) * prime + target.hashCode();
    }

    /**
     * 生成一个Observable  这里设置支持背压  背压的模式是（Buffer）
     * @return
     */
    public Flowable produce(){
        return Flowable.create(new FlowableOnSubscribe() {
            @Override
            public void subscribe(@NonNull FlowableEmitter flowableEmitter) throws Exception {
                try {

                    flowableEmitter.onNext(produceEvent());
                }catch (InvocationTargetException e){
                    throwRuntimeException("Produce "+ProducerEvent.this+"throw  a exception ",e);
                }
            }
        }, BackpressureStrategy.BUFFER);
    }

    private Object produceEvent() throws InvocationTargetException {
        if (!valid){
            throw new IllegalStateException(toString() + "已经失效，无法发送事件");
        }
        try {
            return method.invoke(target);
        }catch (IllegalAccessException  e){
            throw new AssertionError(e);
        }catch (InvocationTargetException e){
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
            throw  e;
        }
    }

    /**
     *  状态
     * @return
     */
    public boolean isValid(){
        return valid;
    }
    /**
    *不能发送事件  当没有注册者时应该调用该方法
    */
    public void invalidate(){
        valid = false;
    }

    @Override
    public int hashCode() {

        return hashcode;
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
        final ProducerEvent other = (ProducerEvent) obj;
        return method.equals(other.method) && target == other.target;
    }
    public Object getTarget(){
        return target;
    }
}
