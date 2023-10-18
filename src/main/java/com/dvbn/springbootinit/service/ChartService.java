package com.dvbn.springbootinit.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dvbn.springbootinit.common.ErrorCode;
import com.dvbn.springbootinit.model.entity.Chart;

/**
 * @author dvbn
 * @description 针对表【chart(图表信息表)】的数据库操作Service
 * @createDate 2023-10-08 20:43:35
 */
public interface ChartService extends IService<Chart> {

    /**
     * 图表更新失败
     *
     * @param code
     * @param chart
     * @param errorMessage
     */
    void handlerChartUpdateError(ErrorCode code, Chart chart, String errorMessage);
}
