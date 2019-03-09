package com.alibaba.dubbo.performance.demo.agent.rpc;

import com.alibaba.dubbo.performance.demo.agent.proto.Agent;
import com.alibaba.dubbo.performance.demo.agent.registry.Endpoint;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.rpc.loadbalance.LoadBalance;
import com.alibaba.dubbo.performance.demo.agent.rpc.loadbalance.RoundRobinLoadBalance;
import com.alibaba.dubbo.performance.demo.agent.server.ConsumerAgentServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.concurrent.FastThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static javax.swing.UIManager.put;


/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: qiwenchao
 * Date: 2018-05-23
 * Time: 下午3:52
 */
public class ConsumerRpcClient{

    private IRegistry registry;

    private Logger logger = LoggerFactory.getLogger(ConsumerRpcHandler.class);


    private LoadBalance loadBalance = null;

    private Map<EventLoop, Channel> channelMap = new HashMap<>();

    private final Object lock = new Object();

    public ConsumerRpcClient(IRegistry registry) {
        this.registry = registry;
        try {
            List<Endpoint> endpoints = registry.find("com.alibaba.dubbo.performance.demo.provider.IHelloService");
            loadBalance = new RoundRobinLoadBalance(endpoints);
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        ConsumerAgentServer.worker.forEach(
                eventExecutor -> {
                    Bootstrap bootstrap = createBootstrap((EventLoop) eventExecutor, loadBalance.select());
                    try {
                        Channel channel = bootstrap.connect().sync().channel();
                        channelMap.put((EventLoop) eventExecutor, channel);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private Bootstrap createBootstrap(EventLoop eventLoop, Endpoint endpoint) {
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
                                // decoded
                                new ProtobufVarint32FrameDecoder(),
                                new ProtobufDecoder(Agent.AgentResponse.getDefaultInstance()),
                                // encoded
                                new ProtobufVarint32LengthFieldPrepender(),
                                new ProtobufEncoder(),
                                new ConsumerRpcHandler());
                    }
                });
    }

    public Channel getChannel(EventLoop eventLoop) throws Exception {
        return channelMap.get(eventLoop);
    }
}
