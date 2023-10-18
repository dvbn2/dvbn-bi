package com.dvbn.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;


/**
 * @author dvbn
 * @title: MyMessageConsumer
 * @createDate 2023/10/18 18:27
 */
@Component
@Slf4j
public class MyMessageConsumer {


    /**
     * 消费者
     *
     * @param message 发送的消息
     * @param channel 与rabbitmq通信，需要使用channel来手动确认消息
     * @param deliveryTag 指定拒绝或接收那一条消息
     */
    @RabbitListener(queues = {"code_queue"}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("message received: {}", message);
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
