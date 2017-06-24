package com.example.gongtong.rxjava2.rxbus;


import com.example.gongtong.rxjava2.rxbus.thread.ThreadEnforcer;


/**
 * Created by gongtong on 17-6-23.
 */

public class RxBus {
    private static Bus busInstance;

    public RxBus() {

    }

    public static synchronized Bus getInstance() {
        if (busInstance == null) {
            busInstance = new Bus(ThreadEnforcer.ANY);
        }
        return busInstance;
    }
}
