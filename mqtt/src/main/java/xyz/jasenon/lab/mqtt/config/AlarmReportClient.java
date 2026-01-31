package xyz.jasenon.lab.mqtt.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import xyz.jasenon.lab.common.entity.log.AlarmLog;

/**
 * 向 service 模块 POST 报警日志到 /log/alarm，由 service 异步入库 alarm_log。
 */
@Slf4j
@Component
public class AlarmReportClient {

    private final RestTemplate restTemplate;
    private final String serviceBaseUrl;

    public AlarmReportClient(RestTemplate restTemplate,
                             @Value("${lab.service.base-url:http://localhost:8088}") String serviceBaseUrl) {
        this.restTemplate = restTemplate;
        this.serviceBaseUrl = serviceBaseUrl.replaceAll("/$", "");
    }

    /**
     * 上报一条报警日志。失败仅打日志，不抛异常，不影响 MQTT 主流程。
     */
    public void reportAlarm(AlarmLog alarmLog) {
        String url = serviceBaseUrl + "/log/alarm";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AlarmLog> request = new HttpEntity<>(alarmLog, headers);
            // 使用 String 接收响应，避免 service 返回 R<Void> 的 JSON 时反序列化到 Void 报错
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.warn("上报报警日志失败: url={}, category={}, alarmType={}, device={}, error={}",
                    url, alarmLog.getCategory(), alarmLog.getAlarmType(), alarmLog.getDevice(), e.getMessage());
        }
    }
}
