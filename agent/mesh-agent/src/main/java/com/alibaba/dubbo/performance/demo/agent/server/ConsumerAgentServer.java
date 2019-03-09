package com.alibaba.dubbo.performance.demo.agent.server;

import com.alibaba.dubbo.performance.demo.agent.registry.EtcdRegistry;
import com.alibaba.dubbo.performance.demo.agent.registry.IRegistry;
import com.alibaba.dubbo.performance.demo.agent.rpc.ConsumerRpcClient;
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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.LongObjectHashMap;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: qiwenchao
 * Date: 2018-05-23
 * Time: 下午2:46
 */
public class ConsumerAgentServer implements AgentServer {

    public static EventLoopGroup worker = new EpollEventLoopGroup(8);

    public static IntObjectHashMap<Channel> channels = new IntObjectHashMap<>();

    private ServerBootstrap bootstrap;

    private IRegistry registry;

    private int port;

    public ConsumerAgentServer(int port) {
        init();
        this.port = port;
    }

    private void init() {
        registry = new EtcdRegistry(AgentConstant.ETCD_URL);
        bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new EpollEventLoopGroup(1);
        ConsumerRpcClient client = new ConsumerRpcClient(registry);

        bootstrap.group(boss, worker)
                .channel(EpollServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new HttpServerCodec(),
                                new HttpObjectAggregator(65536),
                                new ConsumerAgentServerHandler(client)
                        );
                    }
                });
    }

    @Override
    public void run() {
        try {
            ChannelFuture future = bootstrap.bind(port);
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bootstrap.config().group().shutdownGracefully();
            bootstrap.config().childGroup().shutdownGracefully();
        }
    }

}
