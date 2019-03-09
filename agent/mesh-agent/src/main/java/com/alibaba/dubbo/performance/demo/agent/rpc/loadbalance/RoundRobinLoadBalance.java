package com.alibaba.dubbo.performance.demo.agent.rpc.loadbalance;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-29
 * Time: 下午2:22
 */
public class RoundRobinLoadBalance implements LoadBalance {

    private Logger logger = LoggerFactory.getLogger(RoundRobinLoadBalance.class);

    private AtomicInteger count = new AtomicInteger();

    private List<Endpoint> index = new ArrayList<>();

    private int totalWeight;

    public RoundRobinLoadBalance(List<Endpoint> endpoints) {
        totalWeight = endpoints.stream().mapToInt(Endpoint::getWeight).sum();

        for (int i = 0; i < endpoints.size(); i++) {
            for (int j = 0; j < endpoints.get(i).getWeight(); j++) {
                index.add(endpoints.get(i));
            }
        }
    }

    @Override
    public Endpoint select() {

        int order = count.getAndIncrement() % totalWeight;

        return index.get(order);
    }

}
