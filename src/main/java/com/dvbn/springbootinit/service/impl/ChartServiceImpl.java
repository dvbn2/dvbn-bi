package com.dvbn.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dvbn.springbootinit.common.ErrorCode;
import com.dvbn.springbootinit.exception.BusinessException;
import com.dvbn.springbootinit.mapper.ChartMapper;
import com.dvbn.springbootinit.model.entity.Chart;
import com.dvbn.springbootinit.service.ChartService;
import org.springframework.stereotype.Service;

/**
 * @author dvbn
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2023-10-08 20:43:35
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    /**
     * 图表更新失败
     *
     * @param code
     * @param chart
     * @param errorMessage
     */
    public void handlerChartUpdateError(ErrorCode code, Chart chart, String errorMessage) {
        chart.setStatus("failed");
        chart.setExecMessage(errorMessage);
        updateById(chart);
        throw new BusinessException(code, errorMessage);
    }
}




