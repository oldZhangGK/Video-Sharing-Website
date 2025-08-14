package com.example.bilibili.service.util;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQUtil {
    //同步，阻塞，保证可靠，比如订单
    public static void syncSendMsg(DefaultMQProducer producer, Message msg) throws Exception {
        SendResult result = producer.send(msg);
        System.out.println(result);
    }

    //异步，不阻塞，性能高，适合大量发送，需要快速返回调用的，比如日志系统
    public static void asyncSendMsg(DefaultMQProducer producer, Message msg) throws Exception {
        producer.send(msg, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                Logger logger = LoggerFactory.getLogger(RocketMQUtil.class);
                logger.info("Asynchroized messsage sent successfully，message id：" + sendResult.getMsgId());
            }
            @Override
            public void onException(Throwable e) {
                e.printStackTrace();
            }
        });
    }
}
