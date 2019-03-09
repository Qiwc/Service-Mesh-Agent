package com.alibaba.dubbo.performance.demo.agent.server;

import com.alibaba.dubbo.performance.demo.agent.proto.Agent;
import com.alibaba.dubbo.performance.demo.agent.rpc.ConsumerRpcClient;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: qiwenchao
 * Date: 2018-05-23
 * Time: 下午3:49
 */
public class ConsumerAgentServerHandler extends ChannelInboundHandlerAdapter{

    private Logger logger = LoggerFactory.getLogger(ConsumerAgentServerHandler.class);

    private ConsumerRpcClient client;

    private int channelId;

    private Channel targetChannel;

    public ConsumerAgentServerHandler(ConsumerRpcClient client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            Map<String, String> pMap = parse((FullHttpRequest) msg);

            long id = IdGenerator.getInstance().getRequestId() + (((long) channelId) << 20);

            Agent.AgentRequest request = Agent.AgentRequest.newBuilder()
                    .setId(id)
                    .setMethodName(pMap.get("method"))
                    .setInterfaceName(pMap.get("interface"))
                    .setParameterTypesString(pMap.get("parameterTypesString"))
                    .setParameter(pMap.get("parameter")).build();

            targetChannel.writeAndFlush(request);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelId = IdGenerator.getInstance().getChannelId();
        ConsumerAgentServer.channels.put(channelId, ctx.channel());
        targetChannel = client.getChannel(ctx.channel().eventLoop());
    }

    private Map<String, String> parse(FullHttpRequest fullReq) throws IOException {
        HttpMethod method = fullReq.method();

        Map<String, String> parmMap = new HashMap<>();

        if (HttpMethod.GET == method) {
            logger.info("Http Get Request");
        } else if (HttpMethod.POST == method) {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(fullReq);
            decoder.offer(fullReq);

            List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();

            for (InterfaceHttpData parm : parmList) {

                Attribute data = (Attribute) parm;
                parmMap.put(data.getName(), data.getValue());
            }
        } else {
            logger.info("Unsupported method : ", method);
        }
        fullReq.release();
        return parmMap;
    }

}
