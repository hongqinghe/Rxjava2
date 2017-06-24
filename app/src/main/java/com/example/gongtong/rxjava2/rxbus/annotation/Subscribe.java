package com.example.gongtong.rxjava2.rxbus.annotation;


import com.example.gongtong.rxjava2.rxbus.thread.EventThread;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/***********************************************
 * <P> dec:标记一个方法作为一个事件的订阅者，被Bus和AnnotatedFinder
 *            该方法的第一个参数和标记定义事件的类型
 *            如果该消息无法通过Bus进行分发，这将造成运行时异常
 * <P> Author: gongtong
 * <P> Date: 17-6-24.
 * <P> Copyright  2008 二维火科技
 ***********************************************/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    Tag[] tags() default {};

    EventThread thread() default EventThread.MAIN_THREAD;
}
