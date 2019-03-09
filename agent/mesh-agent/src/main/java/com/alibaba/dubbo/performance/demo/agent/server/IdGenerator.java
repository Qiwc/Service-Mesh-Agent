package com.alibaba.dubbo.performance.demo.agent.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-06-06
 * Time: 下午5:25
 */
public class IdGenerator {
    private AtomicInteger channelId;
    private AtomicLong requestId;


    private static IdGenerator instance = new IdGenerator();

    public static IdGenerator getInstance() {
        return instance;
    }

    public int getChannelId() {
        return channelId.incrementAndGet();
    }

    public long getRequestId() {
        return requestId.incrementAndGet();
    }

    private IdGenerator() {
        channelId = new AtomicInteger();
        requestId = new AtomicLong();
    }
}
