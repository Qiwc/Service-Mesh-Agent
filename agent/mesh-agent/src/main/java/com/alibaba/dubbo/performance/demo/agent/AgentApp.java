package com.alibaba.dubbo.performance.demo.agent;

import com.alibaba.dubbo.performance.demo.agent.server.AgentConstant;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentApp {

    public static void main(String[] args) {
        AgentConstant.AGENT_SERVER.run();
    }
}
