package com.alibaba.dubbo.performance.demo.agent.server;

import com.alibaba.dubbo.performance.demo.agent.server.api.AgentServer;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-28
 * Time: 上午12:42
 */
public class AgentConstant {
    public static final String AGENT_TYPE = System.getProperty("type");

    public static final String WEIGHT = System.getProperty("weight", "1");

    public static final int SERVER_PORT = Integer.parseInt(System.getProperty("server.port"));

    public static final int DUBBO_PORT = Integer.valueOf(System.getProperty("dubbo.protocol.port", "80"));

    public static final String ETCD_URL = System.getProperty("etcd.url");

    public static final boolean IS_PROVIDER = "provider".equals(AGENT_TYPE);

    public static final boolean IS_CONSUMER = "consumer".equals(AGENT_TYPE);

    public static final AgentServer AGENT_SERVER =
            IS_PROVIDER ? new ProviderAgentServer(SERVER_PORT) : new ConsumerAgentServer(SERVER_PORT);

}
