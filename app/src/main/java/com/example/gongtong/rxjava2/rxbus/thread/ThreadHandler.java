package com.example.gongtong.rxjava2.rxbus.thread;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Created by gongtong on 17-6-23.
 */

public interface ThreadHandler {
    Executor getExecutor();

    Handler getHandler();

    static ThreadHandler DEFAUL = new ThreadHandler() {
        private Executor executor;
        private Handler handler;

        @Override
        public Executor getExecutor() {
            if (executor == null) {
                //创建一个可缓存的线程池
                executor = Executors.newCachedThreadPool();
            }
            return executor;
        }

        @Override
        public Handler getHandler() {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            return handler;
        }
    };
}
