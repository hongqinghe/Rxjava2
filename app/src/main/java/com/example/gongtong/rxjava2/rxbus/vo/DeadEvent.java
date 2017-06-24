package com.example.gongtong.rxjava2.rxbus.vo;
/**
 * Created by gongtong on 17-6-23.
 */

/***********************************************
 * <P> dec:没有订阅者  无法进行消息发送
 *       可用于调试
 * <P> Author: gongtong$
 * <P> Date: 17-6-23.
 * <P> Copyright  2008 二维火科技
 ***********************************************/

public class DeadEvent {
    public final Object source;
    public final Object event;

    /**
     * 一个无法发送的事件
     * @param source
     * @param event
     */
    public DeadEvent(Object source, Object event) {
        this.source = source;
        this.event = event;
    }
}
