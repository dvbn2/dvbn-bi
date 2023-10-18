package com.dvbn.springbootinit.bizmq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.dvbn.springbootinit.common.ErrorCode;
import com.dvbn.springbootinit.constant.CommonConstant;
import com.dvbn.springbootinit.exception.BusinessException;
import com.dvbn.springbootinit.manager.AiManager;
import com.dvbn.springbootinit.model.entity.Chart;
import com.dvbn.springbootinit.service.ChartService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;


/**
 * @author dvbn
 * @title: MyMessageConsumer
 * @createDate 2023/10/18 18:27
 */
@Component
@Slf4j
public class BiMessageConsumer {


    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    /**
     * 消费者
     *
     * @param message     发送的消息
     * @param channel     与rabbitmq通信，需要使用channel来手动确认消息
     * @param deliveryTag 指定拒绝或接收那一条消息
     */
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        if (StrUtil.isBlank(message)) {
            // 拒绝消息
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        Integer chartId = Integer.valueOf(message);

        Chart chart = chartService.getById(chartId);

        if (BeanUtil.isEmpty(chart)) {
            // 拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表为空");
        }

        // 将图表状态改为running
        chart.setStatus("running");
        boolean beforeResult = chartService.updateById(chart);
        if (!beforeResult) {
            // 拒绝消息
            channel.basicNack(deliveryTag, false, false);
            chartService.handlerChartUpdateError(ErrorCode.OPERATION_ERROR, chart, "更新图表失败");
        }

        // AI调用
        String data = aiManager.doChat(CommonConstant.BI_MODEL_ID, getChartData(chart));
        String[] split = data.split("-----");

        if (split.length < 3) {
            // 拒绝消息
            channel.basicNack(deliveryTag, false, false);
            chartService.handlerChartUpdateError(ErrorCode.SYSTEM_ERROR, chart, "AI 生成错误");
        }

        String genChart = split[1].trim();
        String genResult = split[2].trim();

        // 添加AI生成的数据
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setChartData(data);
        // 修改图表状态为succeed
        chart.setStatus("succeed");

        chart.setExecMessage("执行成功");
        boolean afterResult = chartService.updateById(chart);
        if (!afterResult) {
            // 拒绝消息
            channel.basicNack(deliveryTag, false, false);
            chartService.handlerChartUpdateError(ErrorCode.OPERATION_ERROR, chart, "更新图表失败");
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    private String getChartData(Chart chart) {

        // 读取上传的excel文件，进行处理
        StringBuilder userInput = new StringBuilder();

        userInput.append("分析需求:").append("\n");

        String userGoal = chart.getGoal();
        if (StrUtil.isNotBlank(chart.getChartType())) {
            userGoal += ",请使用" + chart.getChartType();
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");

        userInput.append(chart.getChartData()).append("\n");

        userInput.append("按照设定模板回答，使用{-----}分割代码与分析结论").append("\n");
        return String.valueOf(userInput);
    }
}
