package com.example.gongtong.rxjava2.rxbus.vo;


/***********************************************
 * <P> 事件类型类  内部需要重写equals和hashcode方法  （比较e1.equals(e2)）返回为true
 * <P> Author: gongtong
 * <P> Date: 2017-06-23 下午5:56
 * <P> Copyright  2008 二维火科技
 ***********************************************/
public class EventType {

    private final String tag;
    private final Class<?> clazz;
    private final int hashCode;
    public EventType(String tag, Class<?> clazz) {
        if (tag==null){
            throw new NullPointerException("EventType  tag  can  not be null");
        }
        if (clazz==null){
            throw new NullPointerException("EventType tag can  not be null");

        }
        this.tag = tag;
        this.clazz = clazz;
        /**
         * 开始的hashcode值
         * 计算hashcode进行返回
         */
         final int prime=31;
        this.hashCode = (prime+tag.hashCode())*prime+clazz.hashCode();
    }

    /**
     * 重写hashcode
     * @return
     */
    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this==obj){
            return true;
        }
        if (obj==null) {
            return false;
        }
        if (getClass()!=obj.getClass()){
            return false;
        }
        final EventType other= (EventType) obj;
        return tag.equals(other.tag)&&clazz==other.clazz;
    }
}
