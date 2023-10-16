package com.dvbn.springbootinit.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * @author dvbn
 * @title: AiManagerTest
 * @createDate 2023/10/14 12:33
 */
@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;

    @Test
    void doChat() {
        String result = aiManager.doChat(1659920671007834113L, """
                分析需求：
                分析网站用户的增长情况
                原始数据：
                日期,用户数
                1号,10
                2号,20
                3号,30
                """);
        System.out.println(result);
    }
}