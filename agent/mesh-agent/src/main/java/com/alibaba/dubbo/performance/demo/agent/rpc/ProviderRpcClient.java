package com.alibaba.dubbo.performance.demo.agent.rpc;

import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.server.AgentConstant;
import com.alibaba.dubbo.performance.demo.agent.server.ProviderAgentServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-23
 * Time: 下午3:20
 */
public class ProviderRpcClient {

    private Logger logger = LoggerFactory.getLogger(ProviderRpcClient.class);

    private Endpoint endpoint;

    public static Map<EventLoop, Channel> channelMap = new HashMap<>();

    public ProviderRpcClient() {
        this.endpoint = new Endpoint("127.0.0.1", AgentConstant.DUBBO_PORT, 0);
        init();
    }

    private void init() {
        ProviderAgentServer.worker.forEach(
                eventExecutor -> {
                    Bootstrap bootstrap = createBootstrap((EventLoop) eventExecutor);
                    try {
                        Channel channel = bootstrap.connect().sync().channel();
                        logger.info("Connection!!!");
                        channelMap.put((EventLoop) eventExecutor, channel);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private Bootstrap createBootstrap(EventLoop eventLoop) {
        return new Bootstrap()
                .group(eventLoop)
                .remoteAddress(endpoint.getHost(), endpoint.getPort())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .channel(EpollSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new DubboRpcDecoder(),
                                new DubboRpcEncoder(),
                                new ProviderRpcHandler()
                        );
                    }
                });
    }

    public Channel getChannel(EventLoop eventLoop) {
        Channel channel = channelMap.get(eventLoop);
        if (channel == null) {
            logger.info("Channel is null!");
            Bootstrap bootstrap = createBootstrap(eventLoop);
            channel = bootstrap.connect().channel();
            channelMap.put(eventLoop, channel);
        }
        return channel;
    }

}
