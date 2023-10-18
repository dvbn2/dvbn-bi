package com.dvbn.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author dvbn
 * @title: Main
 * @createDate 2023/10/18 18:36
 */
public class Main {
    public static void main(String[] args) {

        try {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setHost("localhost");
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            // 创建交换机
            String EXCHANGE_NAME = "code_exchange";
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // 创建队列
            String queueName = "code_queue";

            channel.queueDeclare(queueName, true, false, false, null);

            // 绑定交换机和队列
            channel.queueBind(queueName, EXCHANGE_NAME, "my_routingKey");
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
