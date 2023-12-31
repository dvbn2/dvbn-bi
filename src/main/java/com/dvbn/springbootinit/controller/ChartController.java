package com.dvbn.springbootinit.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dvbn.springbootinit.annotation.AuthCheck;
import com.dvbn.springbootinit.bizmq.BiMessageProducer;
import com.dvbn.springbootinit.common.BaseResponse;
import com.dvbn.springbootinit.common.DeleteRequest;
import com.dvbn.springbootinit.common.ErrorCode;
import com.dvbn.springbootinit.common.ResultUtils;
import com.dvbn.springbootinit.constant.CommonConstant;
import com.dvbn.springbootinit.constant.UserConstant;
import com.dvbn.springbootinit.exception.BusinessException;
import com.dvbn.springbootinit.exception.ThrowUtils;
import com.dvbn.springbootinit.manager.AiManager;
import com.dvbn.springbootinit.manager.RedisLimiterManager;
import com.dvbn.springbootinit.model.dto.chart.*;
import com.dvbn.springbootinit.model.entity.Chart;
import com.dvbn.springbootinit.model.entity.User;
import com.dvbn.springbootinit.model.vo.BiResponse;
import com.dvbn.springbootinit.service.ChartService;
import com.dvbn.springbootinit.service.UserService;
import com.dvbn.springbootinit.utils.ExcelUtils;
import com.dvbn.springbootinit.utils.SqlUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 帖子接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    private final static Gson GSON = new Gson();
    @Resource
    private ChartService chartService;
    @Resource
    private UserService userService;
    @Resource
    private AiManager aiManager;
    @Resource
    private RedisLimiterManager redisLimiterManager;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = BeanUtil.copyProperties(chartAddRequest, Chart.class);

        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = BeanUtil.copyProperties(chartUpdateRequest, Chart.class);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = BeanUtil.copyProperties(chartEditRequest, Chart.class);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }


    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StrUtil.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StrUtil.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        long fileSize = multipartFile.getSize(); // 文件大小
        String filename = multipartFile.getOriginalFilename(); // 文件名

        final long ONE_MB = 1024 * 1024L; // 限定文件大小为1MB
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件不可大于1MB");

        String suffix = FileUtil.getSuffix(filename);
        final Set<String> availableSuffixSet = Set.of("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!availableSuffixSet.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀不规范");


        User user = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByBI_" + user.getId());

        String s = """
                你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:
                分析需求:
                {数据分析的需求或者目标}
                原始数据:
                {csv格式的原始数据，用,作为分隔符}
                请根据这两部分内容，按照以下指定格式生成内容(此外不要输出任何多余的开头、结尾、注释)
                -----
                {前端 Echarts V5 的option 配置对象json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}
                -----
                {明确的数据分析结论、越详细越好，不要生成多余的注释}
                                
                分析网站用户增长情况，数据如下：
                1号：用户数10
                2号：用户数20
                3号：用户数60
                4号：用户数20
                5号：用户数100
                6号：用户数40
                """;

        // 分析需求
        /*final String prompt = """
                你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:
                分析需求:
                {数据分析的需求或者目标}
                原始数据:
                {csv格式的原始数据，用,作为分隔符}
                请根据这两部分内容，按照以下指定格式生成内容(此外不要输出任何多余的开头、结尾、注释)
                【【【【【
                {前端 Echarts V5 的 option配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}
                【【【【【
                {明确的数据分析结论、越详细越好，不要生成多余的注释}
                """;*/
        // 读取上传的excel文件，进行处理
        StringBuilder userInput = new StringBuilder();

        userInput.append("分析需求:").append("\n");

        String userGoal = goal;
        if (StrUtil.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");

        // 数据压缩，将excel转为csv
        String result = ExcelUtils.excelToCsv(multipartFile);

        userInput.append(result).append("\n");

        userInput.append("按照设定模板回答，使用{-----}分割代码与分析结论").append("\n");

        long biModelId = 1713120252013187073L;
//        long biModelId = 1651468516836098050L;


        // 插入到数据库
        Chart chart = Chart.builder()
                .name(name)
                .goal(goal)
                .chartData(result)
                .chartType(chartType)
                .status("wait")
                .userId(user.getId())
                .build();

        boolean saveResult = chartService.save(chart);

        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        Chart updateChart = Chart.builder().build();
        updateChart.setId(chart.getId());

        // 向线程池添加任务
        CompletableFuture.runAsync(() -> {
            // 将图表状态改为running
            updateChart.setStatus("running");
            boolean beforeResult = chartService.updateById(updateChart);
            if (!beforeResult) {
                chartService.handlerChartUpdateError(ErrorCode.OPERATION_ERROR, updateChart, "更新图表失败");
            }


            // AI调用
            String data = aiManager.doChat(biModelId, userInput.toString());
            String[] split = data.split("-----");

            if (split.length < 3) {
                chartService.handlerChartUpdateError(ErrorCode.SYSTEM_ERROR, updateChart, "AI 生成错误");
            }

            String genChart = split[1].trim();
            String genResult = split[2].trim();

            // 添加AI生成的数据
            updateChart.setGenChart(genChart);
            updateChart.setGenResult(genResult);
            updateChart.setChartData(data);
            // 修改图表状态为succeed
            updateChart.setStatus("succeed");

            updateChart.setExecMessage("执行成功");
            boolean afterResult = chartService.updateById(updateChart);
            if (!afterResult) {
                chartService.handlerChartUpdateError(ErrorCode.OPERATION_ERROR, updateChart, "更新图表失败");
            }


        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(updateChart.getGenChart());
        biResponse.setGenResult(updateChart.getGenResult());
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }


    /**
     * 智能分析（异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<String> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StrUtil.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StrUtil.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        long fileSize = multipartFile.getSize(); // 文件大小
        String filename = multipartFile.getOriginalFilename(); // 文件名

        final long ONE_MB = 1024 * 1024L; // 限定文件大小为1MB
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件不可大于1MB");

        String suffix = FileUtil.getSuffix(filename);
        final Set<String> availableSuffixSet = Set.of("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!availableSuffixSet.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀不规范");


        User user = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByBI_" + user.getId());

        String s = """
                你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:
                分析需求:
                {数据分析的需求或者目标}
                原始数据:
                {csv格式的原始数据，用,作为分隔符}
                请根据这两部分内容，按照以下指定格式生成内容(此外不要输出任何多余的开头、结尾、注释)
                -----
                {前端 Echarts V5 的option 配置对象json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}
                -----
                {明确的数据分析结论、越详细越好，不要生成多余的注释}
                                
                分析网站用户增长情况，数据如下：
                1号：用户数10
                2号：用户数20
                3号：用户数60
                4号：用户数20
                5号：用户数100
                6号：用户数40
                """;

        // 数据压缩，将excel转为csv
        String result = ExcelUtils.excelToCsv(multipartFile);

        // 插入到数据库
        Chart chart = Chart.builder()
                .name(name)
                .goal(goal)
                .chartData(result)
                .chartType(chartType)
                .status("wait")
                .userId(user.getId())
                .build();

        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");


        // 发送消息
        biMessageProducer.sendMessage(String.valueOf(chart.getId()));

        return ResultUtils.success("操作成功");
    }

    /**
     * 智能分析（同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/s")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();

        // 校验
        ThrowUtils.throwIf(StrUtil.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StrUtil.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 校验文件
        long fileSize = multipartFile.getSize(); // 文件大小
        String filename = multipartFile.getOriginalFilename(); // 文件名

        final long ONE_MB = 1024 * 1024L; // 限定文件大小为1MB
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件不可大于1MB");

        String suffix = FileUtil.getSuffix(filename);
        final Set<String> availableSuffixSet = Set.of("xlsx", "xls", "csv");
        ThrowUtils.throwIf(!availableSuffixSet.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀不规范");


        User user = userService.getLoginUser(request);

        // 限流
        redisLimiterManager.doRateLimit("genChartByBI_" + user.getId());

        String s = """
                你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:
                分析需求:
                {数据分析的需求或者目标}
                原始数据:
                {csv格式的原始数据，用,作为分隔符}
                请根据这两部分内容，按照以下指定格式生成内容(此外不要输出任何多余的开头、结尾、注释)
                -----
                {前端 Echarts V5 的option 配置对象json代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}
                -----
                {明确的数据分析结论、越详细越好，不要生成多余的注释}
                                
                分析网站用户增长情况，数据如下：
                1号：用户数10
                2号：用户数20
                3号：用户数60
                4号：用户数20
                5号：用户数100
                6号：用户数40
                """;

        // 分析需求
        /*final String prompt = """
                你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容:
                分析需求:
                {数据分析的需求或者目标}
                原始数据:
                {csv格式的原始数据，用,作为分隔符}
                请根据这两部分内容，按照以下指定格式生成内容(此外不要输出任何多余的开头、结尾、注释)
                【【【【【
                {前端 Echarts V5 的 option配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}
                【【【【【
                {明确的数据分析结论、越详细越好，不要生成多余的注释}
                """;*/
        // 读取上传的excel文件，进行处理
        StringBuilder userInput = new StringBuilder();

        userInput.append("分析需求:").append("\n");

        String userGoal = goal;
        if (StrUtil.isNotBlank(chartType)) {
            userGoal += ",请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据:").append("\n");

        // 数据压缩，将excel转为csv
        String result = ExcelUtils.excelToCsv(multipartFile);

        userInput.append(result).append("\n");

        userInput.append("按照设定模板回答，使用{-----}分割代码与分析结论").append("\n");

        long biModelId = 1713120252013187073L;
//        long biModelId = 1651468516836098050L;

        // AI调用
        String data = aiManager.doChat(biModelId, userInput.toString());
        String[] split = data.split("-----");

        // 生成不和法，抛出异常
        ThrowUtils.throwIf(split.length < 3, ErrorCode.SYSTEM_ERROR, "AI 生成错误");

        String genChart = split[1].trim();
        String genResult = split[2].trim();


        // 插入到数据库
        Chart chart = Chart.builder()
                .name(name)
                .goal(goal)
                .chartData(data)
                .chartType(chartType)
                .genChart(genChart)
                .genResult(genResult)
                .status("wait")
                .execMessage("执行成功")
                .userId(user.getId())
                .build();


        boolean saveResult = chartService.save(chart);

        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(chart.getGenChart());
        biResponse.setGenResult(chart.getGenResult());
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.eq(StrUtil.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StrUtil.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);


        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

}
