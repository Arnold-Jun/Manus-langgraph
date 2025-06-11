package com.zhouruojun.manus.infrastructure.tools.imp.search;

import java.util.function.Function;

/**
 * 天气服务工具
 * 基于Spring AI的Function<请求,响应>模式
 */
public class WeatherTool implements Function<WeatherTool.WeatherRequest, WeatherTool.WeatherResponse> {

    @Override
    public WeatherResponse apply(WeatherRequest request) {
        // 这里是示例实现，实际中可能需要调用外部API
        return new WeatherResponse(0.0, request.unit());
    }

    /**
     * 天气请求记录类
     */
    public record WeatherRequest(String location, Unit unit) {}

    /**
     * 天气响应记录类
     */
    public record WeatherResponse(double temperature, Unit unit) {}

    /**
     * 温度单位枚举
     */
    public enum Unit { C, F }
}
