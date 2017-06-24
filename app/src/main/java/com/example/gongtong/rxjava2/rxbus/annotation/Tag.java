package com.example.gongtong.rxjava2.rxbus.annotation;

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
public @interface Tag {
    static final String DEFAULT = "default_tag";

    String value() default DEFAULT;
}
