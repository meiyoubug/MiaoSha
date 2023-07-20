package com.zc.threadPool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @program: Netty
 * @description:
 * @author: ZC
 * @create: 2023-05-21 14:39
 **/
public class ThreadRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
    * @Description: 饱和策略一：调用者线程执行策略
    * @Param: 在该策略下,在调用者中执行被拒绝任务的run方法。除非线程池showdown，否则直接丢弃线程
    * @return:
    * @Author: ZC
    * @Date: 2023/5/21
    */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {}


    /**
     * 饱和策略一：调用者线程执行策略
     * 在该策略下，在调用者中执行被拒绝任务的run方法。除非线程池showdown，否则直接丢弃线程
     */
    public static class CallerRunsPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //判断线程池是否在正常运行，如果线程池在正常运行则由调用者线程执行被拒绝的任务。如果线程池停止运行，则直接丢弃该任务
            if (!executor.isShutdown()){
                r.run();
            }
        }
    }


    /**
     * 饱和策略二：终止策略
     * 在该策略下，丢弃被拒绝的任务，并抛出拒绝执行异常
     */
    public static class AbortPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            throw new RejectedExecutionException("请求任务：" + r.toString() + "，线程池负载过高执行饱和终止策略！");
        }
    }


    /**
     * 饱和策略三：丢弃策略
     * 在该策略下，什么都不做直接丢弃被拒绝的任务
     */
    public static class DiscardPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

        }
    }


    /**
     * 饱和策略四：弃老策略
     * 在该策略下，丢弃最早放入阻塞队列中的线程，并尝试将拒绝任务加入阻塞队列
     */
    public static class DiscardOldestPolicy extends ThreadRejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //判断线程池是否正常运行，如果线程池正常运行则弹出（或丢弃）最早放入阻塞队列中的任务，并尝试将拒绝任务加入阻塞队列。如果线程池停止运行，则直接丢弃该任务
            if (!executor.isShutdown()){
                executor.getQueue().poll();
                executor.execute(r);
            }
        }
    }

}
