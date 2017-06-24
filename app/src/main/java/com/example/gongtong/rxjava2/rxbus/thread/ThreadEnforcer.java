package com.example.gongtong.rxjava2.rxbus.thread;

import android.os.Looper;

import com.example.gongtong.rxjava2.rxbus.Bus;

/***********************************************
 * <P> dec: 对在线程总线上的方法执行线程限制
 * <P> Author: gongtong
 * <P> Date: 17-6-24.
 * <P> Copyright  2008 二维火科技
 ***********************************************/

public interface ThreadEnforcer {
    /**
     * 为指定的bus执行一个有效的线程     这可能抛出一个运行时异常
     */
    void enforce(Bus bus);

   ThreadEnforcer  ANY=new ThreadEnforcer() {
       @Override
       public void enforce(Bus bus) {
            //允许所有的线程
       }

   };
   ThreadEnforcer MAIN=new ThreadEnforcer() {
       @Override
       public void enforce(Bus bus) {
           if (Looper.myLooper() != Looper.getMainLooper()) {
               throw new IllegalArgumentException("Event bus" + bus + "在非主线程中访问" + Looper.myLooper());
           }
       }
   };
}
