package com.dvbn.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * @author dvbn
 * @title: GenChartByAiRequest
 * @createDate 2023/10/13 21:55
 */
@Data
public class GenChartByAiRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 图表名称
     */
    private String name;
    /**
     * 图表名称
     */
    private String goal;
    /**
     * 图表类型
     */
    private String chartType;
}
