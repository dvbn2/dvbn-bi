package com.dvbn.springbootinit.bizmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author dvbn
 * @title: MyMessageProducer
 * @createDate 2023/10/18 18:24
 */
@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(BiMqConstant.BI_QUEUE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }
}
