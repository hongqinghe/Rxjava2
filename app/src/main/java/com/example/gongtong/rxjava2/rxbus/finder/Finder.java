package com.example.gongtong.rxjava2.rxbus.finder;

import com.example.gongtong.rxjava2.rxbus.vo.EventType;
import com.example.gongtong.rxjava2.rxbus.vo.ProducerEvent;
import com.example.gongtong.rxjava2.rxbus.vo.SubscriberEvent;

import java.util.Map;
import java.util.Set;

/***********************************************
 * <P> dec:
 * <P> Author: gongtong
 * <P> Date: 17-6-23.
 * <P> Copyright  2008 二维火科技
 ***********************************************/

public interface Finder {
    /**
     * 所有的发布者
     * @param listener
     * @return
     */
    Map<EventType, ProducerEvent> findAllProducers(Object listener);

    /**
     * 所有的订阅者者
     *
     * @param listener
     * @return
     */
    Map<EventType, Set<SubscriberEvent>> findAllSubscribers(Object listener);
    Finder ANNOTATED=new Finder() {
        @Override
        public Map<EventType, ProducerEvent> findAllProducers(Object listener) {
            return AnnotatedFinder.findAllProducers(listener);
        }

        @Override
        public Map<EventType, Set<SubscriberEvent>> findAllSubscribers(Object listener) {
            return AnnotatedFinder.finAllSubscribers(listener);
        }
    };

}
