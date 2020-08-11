package com.willi.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: sakura
 * @description:
 * @author: Hoodie_Willi
 * @create: 2020-04-28 16:17
 **/
@Slf4j
public class NettyClient {
    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static ExecutorService getExecutor() {
        return executor;
    }


    private static NettyClientHandler client;
    private AtomicInteger count = new AtomicInteger(0);

    /**
     * 获取一个代理对象
     * @param serviceClass 代理的接口，provider和consumer端都实现了这个接口
     * @param providerName 协议头
     * @return 返回代理对象
     */
    public Object getBean(final Class<?> serviceClass, final String providerName, String hostName, int port){
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{serviceClass}, (proxy, method, args)->{
            // 客户端每调用一次hello，就进入到该代码

            log.info("被调用" + count.getAndIncrement());
            if (client == null){
                // 初始化一个client传入host和port，创建channel
                initClient(hostName, port);
            }
            // 设置要发给服务器端的信息
                    // providerName协议头， arg[0]就是客户端调用api hello的参数
                    client.setPara(providerName + args[0]);
                    Future submit = executor.submit(client);
                    return submit.get();
                });
    }

    // 初始化客户端
    private static void initClient(String hostName, int port){
         client = new NettyClientHandler();

        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(
                        new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                // 一个channel包含了一个channelPipeline，而ChannelPipeLine中有维护了一个由ChannelHandlerConetext
                                ChannelPipeline pipeline = ch.pipeline();
                                // 加入解码器
                                pipeline.addLast(new StringDecoder());
                                // 加入编码器
                                pipeline.addLast(new StringEncoder());
                                // 加入自己的业务处理handler
                                pipeline.addLast(client);
                            }
                        }
                );
        try {
            bootstrap.connect(hostName, port).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
