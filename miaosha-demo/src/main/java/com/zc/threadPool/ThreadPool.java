package com.zc.threadPool;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

/**
 * @program: Netty
 * @description:
 * @author: ZC
 * @create: 2023-05-09 14:21
 **/
@Component
public class ThreadPool{
    /**
     * 系统可用计算资源
     */
    private static final  int CPU_COUNT=Runtime.getRuntime().availableProcessors();


    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE=Math.max(2,Math.min(CPU_COUNT-1,4));

    /**
     * 最大线程数
     */
    private static final int MAXIMUM_POOL_SIZE=CPU_COUNT*2+1;

    /**
     * 线程最大空闲存活时间
     */
    private static final int KEEP_ALIVE_SECONDS = 30;

    /**
     * 工作队列
     */
    private static final BlockingQueue<Runnable> POOL_WORK_QUEUE = new LinkedBlockingQueue<>(2);


    /**
     * 工厂模式
     */
    private static final MyThreadFactory MY_THREAD_FACTORY = new MyThreadFactory();


    /**
     * 饱和策略
     */
    private static final ThreadRejectedExecutionHandler THREAD_REJECTED_EXECUTION_HANDLER = new ThreadRejectedExecutionHandler.CallerRunsPolicy();


    /**
     * 线程池对象
     */
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    /**
     * 声明式定义线程池工具类对象静态变量，在所有线程中同步
     */
    private static volatile ThreadPool threadPool = null;


    /**
     * 初始化线程池静态代码块
     */
    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                //核心线程数
                CORE_POOL_SIZE,
                //最大线程数
                MAXIMUM_POOL_SIZE,
                //空闲线程执行时间
                KEEP_ALIVE_SECONDS,
                //空闲线程执行时间单位
                TimeUnit.SECONDS,
                //工作队列（或阻塞队列）
                POOL_WORK_QUEUE,
                //工厂模式
                MY_THREAD_FACTORY,
                //饱和策略
                THREAD_REJECTED_EXECUTION_HANDLER
        );
    }

    /**
     * 线程池工具类空参构造方法
     */
    public ThreadPool() {}



    /**
     * 获取线程池工具类实例
     */
    @Bean
    public ThreadPool getNewInstance(){
        return new ThreadPool();
    }

    /**
     * 获得当前活动线程数
     *
     * @return int
     */
    public int getCorePoolSize(){
        return THREAD_POOL_EXECUTOR.getActiveCount();
    }


    /**
     * 获得全当前任务总数
     *
     * @return int
     */
    public int getTaskNum(){
        return THREAD_POOL_EXECUTOR.getQueue().size();
    }

    /**
     * 执行任务线程
     */
    public void execut(Runnable runnable) {
        THREAD_POOL_EXECUTOR.execute(runnable);
    }

    public <T> Future<T> submit(Callable<T> callable){
        return THREAD_POOL_EXECUTOR.submit(callable);
    }


    /**
     * 获取线程池状态
     * @return 返回线程池状态
     */
    public boolean isShutDown(){
        return THREAD_POOL_EXECUTOR.isShutdown();
    }

    /**
     * 停止正在执行的线程任务
     * @return 返回等待执行的任务列表
     */
    public List<Runnable> shutDownNow(){
        return THREAD_POOL_EXECUTOR.shutdownNow();
    }

    /**
     * 关闭线程池
     */
    public void shutDown(){
        THREAD_POOL_EXECUTOR.shutdown();
    }


    /**
     * 关闭线程池后判断所有任务是否都已完成
     * @return
     */
    public boolean isTerminated(){
        return THREAD_POOL_EXECUTOR.isTerminated();
    }
}
