package com.dvbn.springbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dvbn.springbootinit.model.entity.Chart;

import java.util.List;
import java.util.Map;

/**
 * @author dvbn
 * @description 针对表【chart(图表信息表)】的数据库操作Mapper
 * @createDate 2023-10-08 20:43:35
 * @Entity com.yupi.springbootinit.model.entity.Chart
 */
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String, Object>> queryChartData(String chartId);
}




