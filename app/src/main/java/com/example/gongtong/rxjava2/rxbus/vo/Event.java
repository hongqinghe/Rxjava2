package com.example.gongtong.rxjava2.rxbus.vo;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by gongtong on 17-6-23.
 */

abstract class Event {
    /**
     *
     * InvocationTargetException异常由Method.invoke(obj, args...)方法抛出。
     * 当被调用的方法的内部抛出了异常而没有被捕获时，将由此异常接收
     * @param msg
     * @param e
     */
    public void throwRuntimeException(String msg, InvocationTargetException e) {
        throwRuntimeException(msg, e.getCause());
    }

    /**
     * @param msg
     * @param e
     */
    public void throwRuntimeException(String msg, Throwable e) {
        Throwable casue = e.getCause();
        if (casue != null) {
            throw new RuntimeException(msg + ":" + casue.getMessage(), casue);
        } else {
            throw new RuntimeException(msg + ":" + e.getMessage(), e);
        }
    }

}
