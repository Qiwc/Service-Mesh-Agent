package com.alibaba.dubbo.performance.demo.agent.rpc.loadbalance;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-29
 * Time: 下午2:06
 */
public interface LoadBalance {

    Endpoint select();

}
