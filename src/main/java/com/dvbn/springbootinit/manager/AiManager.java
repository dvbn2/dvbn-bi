package com.dvbn.springbootinit.manager;

import com.dvbn.springbootinit.common.ErrorCode;
import com.dvbn.springbootinit.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author dvbn
 * @title: AiManager
 * @createDate 2023/10/14 12:17
 */
@Service
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    public static void main(String[] args) {

    }

    /**
     * AI 对话
     *
     * @param message
     * @return
     */
    public String doChat(Long modelId, String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        // 模型id
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);

        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);

        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI响应错误");
        }

        return response.getData().getContent();
    }
}
