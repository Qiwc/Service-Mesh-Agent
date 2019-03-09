package com.alibaba.dubbo.performance.demo.agent.server;

import com.alibaba.dubbo.performance.demo.agent.proto.Agent;
import com.alibaba.dubbo.performance.demo.agent.rpc.ProviderRpcClient;
import com.alibaba.dubbo.performance.demo.agent.rpc.model.JsonUtils;
import com.alibaba.dubbo.performance.demo.agent.rpc.model.Request;
import com.alibaba.dubbo.performance.demo.agent.rpc.model.RpcInvocation;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-23
 * Time: 下午3:38
 */
public class ProviderAgentServerHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);

    private ProviderRpcClient client;

    private Channel targetChannel;

    private int channelId;

    public ProviderAgentServerHandler(ProviderRpcClient client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Agent.AgentRequest agentRequest = (Agent.AgentRequest) msg;

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(agentRequest.getMethodName());
        invocation.setAttachment("path", agentRequest.getInterfaceName());
        invocation.setParameterTypes(agentRequest.getParameterTypesString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(agentRequest.getParameter(), writer);
        invocation.setArguments(out.toByteArray());

        Request request = new Request(agentRequest.getId() * 10 + channelId);
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);
//        logger.info("Provider send request : " + channelId);
        targetChannel.writeAndFlush(request);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelId = IdGenerator.getInstance().getChannelId();
        ProviderAgentServer.channels.put(channelId, ctx.channel());
        targetChannel = client.getChannel(ctx.channel().eventLoop());
    }
}
