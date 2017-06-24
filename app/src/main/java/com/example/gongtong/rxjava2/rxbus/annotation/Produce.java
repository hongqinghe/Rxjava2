package com.example.gongtong.rxjava2.rxbus.annotation;

import com.example.gongtong.rxjava2.rxbus.thread.EventThread;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/***********************************************
 * <P> dec:
 * <P> Author: gongtong
 * <P> Date: 17-6-24.
 * <P> Copyright  2008 二维火科技
 ***********************************************/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface Produce {
    Tag[] tags() default {};

    EventThread thread() default EventThread.MAIN_THREAD;
}
