package com.zc.threadPool;

import java.util.concurrent.ThreadFactory;

/**
 * @program: Netty
 * @description:
 * @author: ZC
 * @create: 2023-05-21 14:38
 **/
public class MyThreadFactory  implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        Thread newThread=new Thread(r);
        return  newThread;
    }
}
