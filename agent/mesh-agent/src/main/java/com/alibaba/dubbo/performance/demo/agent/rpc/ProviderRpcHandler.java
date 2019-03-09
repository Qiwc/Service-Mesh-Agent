package com.alibaba.dubbo.performance.demo.agent.rpc;

import com.alibaba.dubbo.performance.demo.agent.proto.Agent;
import com.alibaba.dubbo.performance.demo.agent.rpc.model.RpcResponse;
import com.alibaba.dubbo.performance.demo.agent.server.ProviderAgentServer;
import com.google.protobuf.ByteString;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-24
 * Time: 下午10:00
 */
public class ProviderRpcHandler extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(ProviderRpcHandler.class);

    private Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RpcResponse response = (RpcResponse) msg;

        int channelId = (int) (response.getRequestId() % 10);
        Agent.AgentResponse agentResponse = Agent.AgentResponse.newBuilder()
                .setId(response.getRequestId() / 10)
                .setValueBytes(ByteString.copyFrom(response.getBytes()))
                .build();

        if (isLegal(agentResponse.getValue())) {
//            logger.info("Provider Response : " + channelId);
            Channel sourceChannel = ProviderAgentServer.channels.get(channelId);
            sourceChannel.writeAndFlush(agentResponse);
        } else {
//            logger.info("Not legal : " + agentResponse.getValue());
        }
    }

    private boolean isLegal(String str) {
        return pattern.matcher(str).matches();
    }
}
