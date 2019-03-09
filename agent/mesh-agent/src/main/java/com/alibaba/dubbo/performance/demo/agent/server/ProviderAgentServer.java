package com.alibaba.dubbo.performance.demo.agent.server;

import com.alibaba.dubbo.performance.demo.agent.proto.Agent;
import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.rpc.ProviderRpcClient;
import com.alibaba.dubbo.performance.demo.agent.server.api.AgentServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.util.collection.IntObjectHashMap;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: chenyifan
 * Date: 2018-05-23
 * Time: 下午2:34
 */
public class ProviderAgentServer implements AgentServer {

    public static EventLoopGroup worker = new EpollEventLoopGroup(1);

    public static IntObjectHashMap<Channel> channels = new IntObjectHashMap<>();

    private ServerBootstrap bootstrap;
    private IRegistry registry;
    private int port;

    public ProviderAgentServer(int port) {
        init();
        this.port = port;
    }

    private void init() {
        bootstrap = new ServerBootstrap();
        registry = new EtcdRegistry(AgentConstant.ETCD_URL);
        EventLoopGroup boss = new EpollEventLoopGroup(1);
        ProviderRpcClient client = new ProviderRpcClient();

        bootstrap.group(boss, worker)
                .channel(EpollServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                // decoded
                                new ProtobufVarint32FrameDecoder(),
                                new ProtobufDecoder(Agent.AgentRequest.getDefaultInstance()),
                                // encoded
                                new ProtobufVarint32LengthFieldPrepender(),
                                new ProtobufEncoder(),
                                new ProviderAgentServerHandler(client));
                    }
                });
    }

    @Override
    public void run() {
        try {
            ChannelFuture future = bootstrap.bind(port);
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bootstrap.config().group().shutdownGracefully();
            bootstrap.config().childGroup().shutdownGracefully();
        }
    }
}
