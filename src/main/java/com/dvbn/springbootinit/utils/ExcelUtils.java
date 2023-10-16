package com.dvbn.springbootinit.utils;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dvbn
 * @title: Excel相关工具类
 * @createDate 2023/10/13 22:18
 */
@Slf4j
public class ExcelUtils {

    /**
     * excel转csv
     *
     * @param multipartFil
     * @return
     */
    public static String excelToCsv(MultipartFile multipartFil) {

        // 读取excel数据
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFil.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误", e);
        }

        if (CollUtil.isEmpty(list)) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();


        // 转为csv
        // 读取表头
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap<Integer, String>) list.get(0);

        // 过滤为null的数据
        List<String> headerList = headerMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());

        // 使用,拼接列表数据
        stringBuilder.append(StrUtil.join(",", headerList)).append("\n");

        // 读取每一行数据
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap<Integer, String>) list.get(i);
            // 过滤为null的数据
            List<String> dataList = dataMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());

            // 使用,拼接列表数据
            stringBuilder.append(StrUtil.join(",", dataList)).append("\n");
        }
        System.out.println(list);
        return stringBuilder.toString();
    }

    public static void main(String[] args) {
        excelToCsv(null);
    }
}
