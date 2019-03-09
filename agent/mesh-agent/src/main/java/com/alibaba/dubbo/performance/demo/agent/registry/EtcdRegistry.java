package com.alibaba.dubbo.performance.demo.agent.registry;

import com.alibaba.dubbo.performance.demo.agent.server.AgentConstant;
import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class EtcdRegistry implements IRegistry {
    private Logger logger = LoggerFactory.getLogger(EtcdRegistry.class);
    // 该EtcdRegistry没有使用etcd的Watch机制来监听etcd的事件
    // 添加watch，在本地内存缓存地址列表，可减少网络调用的次数
    // 使用的是简单的随机负载均衡，如果provider性能不一致，随机策略会影响性能

    private final String rootPath = "dubbomesh";
    private Lease lease;
    private KV kv;
    private long leaseId;

    public EtcdRegistry(String registryAddress) {
        Client client = Client.builder().endpoints(registryAddress).build();
        this.lease   = client.getLeaseClient();
        this.kv      = client.getKVClient();
        logger.info(AgentConstant.AGENT_TYPE + " Resigtry");
        try {
            this.leaseId = lease.grant(30).get().getID();
            logger.info("LeaseId : " + leaseId);
        } catch (Exception e) {
            logger.error("Fail to get leaseId :" + e.getMessage());
        }

        keepAlive();
        String type = System.getProperty("type");   // 获取type参数
        if ("provider".equals(type)){
            // 如果是provider，去etcd注册服务
            try {
                int port = Integer.valueOf(System.getProperty("server.port"));
                register("com.alibaba.dubbo.performance.demo.provider.IHelloService", port);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    // 向ETCD中注册服务
    public void register(String serviceName,int port) throws Exception {
        String weight = AgentConstant.WEIGHT;
        String strKey = MessageFormat.format("/{0}/{1}/{2}:{3}",rootPath,serviceName, IpHelper.getHostIp(),String.valueOf(port));
        ByteSequence key = ByteSequence.fromString(strKey);
        ByteSequence val = ByteSequence.fromString(weight);
        kv.put(key,val, PutOption.newBuilder().withLeaseId(leaseId).build()).get();
        logger.info("Register a new service at:" + strKey + "  weight : " + weight);
    }

    // 发送心跳到ETCD,表明该host是活着的
    public void keepAlive(){
        Executors.newSingleThreadExecutor(new DefaultThreadFactory("heart-beat")).submit(
                () -> {
                    try {
                        Lease.KeepAliveListener listener = lease.keepAlive(leaseId);
                        listener.listen();
                        logger.info("KeepAlive lease:" + leaseId + "; Hex format:" + Long.toHexString(leaseId));
                    } catch (Exception e) {  logger.error("",e); }
                }
        );
    }

    @Override
    public List<Endpoint> find(String serviceName) throws Exception {
        String strKey = MessageFormat.format("/{0}/{1}",rootPath,serviceName);
        ByteSequence key  = ByteSequence.fromString(strKey);
        GetResponse response = kv.get(key, GetOption.newBuilder().withPrefix(key).build()).get();

        List<Endpoint> endpoints = new ArrayList<>();

        for (com.coreos.jetcd.data.KeyValue kv : response.getKvs()){
            String s = kv.getKey().toStringUtf8();
            int index = s.lastIndexOf("/");
            String endpointStr = s.substring(index + 1,s.length());
            String host = endpointStr.split(":")[0];
            int port = Integer.valueOf(endpointStr.split(":")[1]);
            int cores = Integer.valueOf(kv.getValue().toStringUtf8());
            endpoints.add(new Endpoint(host, port, cores));
        }
        return endpoints;
    }


}
