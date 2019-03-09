package com.alibaba.dubbo.performance.demo.agent.rpc.model;

import com.alibaba.dubbo.performance.demo.agent.proto.Agent;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.DefaultPromise;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-06-11
 * Time: 下午1:08
 */
public class AgentPromise extends DefaultPromise<Agent.AgentResponse> {

    public AgentPromise(ChannelHandlerContext ctx) {
        super(ctx.executor());
        this.addListener(future -> callback(ctx.channel(), (Agent.AgentResponse) future.get()));
    }

    private void callback(Channel sourceChannel, Agent.AgentResponse response) {
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(response.getValue().getBytes()));
        resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
        resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        sourceChannel.writeAndFlush(resp);
    }
}
