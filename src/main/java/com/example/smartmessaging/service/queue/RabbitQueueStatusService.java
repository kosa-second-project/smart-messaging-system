package com.example.smartmessaging.service.queue;

import com.example.smartmessaging.config.RabbitMQConfig;
import com.example.smartmessaging.dto.response.DashboardQueueStatusResponse;
import com.example.smartmessaging.dto.response.DashboardQueueStatusResponse.QueueItem;
import com.example.smartmessaging.service.repository.DashboardMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class RabbitQueueStatusService {
    private final RestTemplate restTemplate;
    private final DashboardMapper dashboardMapper;
    private final String managementBaseUrl;
    private final String vhost;

    public RabbitQueueStatusService(
            RestTemplateBuilder restTemplateBuilder,
            DashboardMapper dashboardMapper,
            @Value("${rabbitmq.management.base-url:http://localhost:15672}") String managementBaseUrl,
            @Value("${rabbitmq.management.vhost:/}") String vhost,
            @Value("${rabbitmq.management.username:guest}") String username,
            @Value("${rabbitmq.management.password:guest}") String password
    ) {
        this.restTemplate = restTemplateBuilder.basicAuthentication(username, password).build();
        this.dashboardMapper = dashboardMapper;
        this.managementBaseUrl = trimTrailingSlash(managementBaseUrl);
        this.vhost = vhost;
    }

    public DashboardQueueStatusResponse getDashboardQueueStatus() {
        long activeSendBacklog = countActiveSendBacklog();
        List<QueueItem> queues = List.of(
                queueItem(RabbitMQConfig.CAMP_COMMAND_QUEUE, "\uCEA0\uD398\uC778 \uD050", "#8B5CF6", "violet"),
                queueItem(RabbitMQConfig.MAIN_QUEUE, "\uBA54\uC2DC\uC9C0 \uBC1C\uC1A1 \uD050", "#3B82F6", "blue", activeSendBacklog),
                queueItem(RabbitMQConfig.DLQ_QUEUE, "\uC2E4\uD328 \uD050", "#EF4444", "red")
        );

        long totalReadyCount = queues.stream().mapToLong(QueueItem::getReadyCount).sum();
        long totalQueueCount = queues.stream().mapToLong(QueueItem::getTotalCount).sum();
        long totalDatabaseBacklogCount = queues.stream().mapToLong(QueueItem::getDatabaseBacklogCount).sum();
        int totalConsumerCount = queues.stream().mapToInt(QueueItem::getConsumerCount).sum();
        long deadCount = queues.stream()
                .filter(queue -> RabbitMQConfig.DLQ_QUEUE.equals(queue.getQueueName()))
                .mapToLong(QueueItem::getTotalCount)
                .sum();
        boolean hasUnavailableQueue = queues.stream().anyMatch(queue -> !queue.isAvailable());

        String status = resolveStatus(totalQueueCount + totalDatabaseBacklogCount, deadCount, hasUnavailableQueue);
        return DashboardQueueStatusResponse.builder()
                .status(status)
                .statusLabel(statusLabel(status))
                .statusMessage(statusMessage(status))
                .refreshedAt(LocalDateTime.now())
                .totalReadyCount(totalReadyCount)
                .totalConsumerCount(totalConsumerCount)
                .queues(queues)
                .build();
    }

    private QueueItem queueItem(String queueName, String label, String color, String badge) {
        return queueItem(queueName, label, color, badge, 0);
    }

    private QueueItem queueItem(String queueName, String label, String color, String badge, long databaseBacklogCount) {
        Map<String, Object> queue = fetchQueue(queueName);
        boolean available = queue != null;
        long readyCount = longValue(queue, "messages_ready");
        long unackedCount = longValue(queue, "messages_unacknowledged");
        long totalCount = longValue(queue, "messages");
        int consumerCount = (int) longValue(queue, "consumers");

        return QueueItem.builder()
                .queueName(queueName)
                .label(label)
                .readyCount(readyCount)
                .unackedCount(unackedCount)
                .totalCount(totalCount)
                .databaseBacklogCount(databaseBacklogCount)
                .consumerCount(consumerCount)
                .color(color)
                .badge(badge)
                .available(available)
                .build();
    }

    private long countActiveSendBacklog() {
        try {
            return Math.max(dashboardMapper.countActiveSendBacklog(), 0);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchQueue(String queueName) {
        try {
            String url = managementBaseUrl + "/api/queues/" + encode(vhost) + "/" + encode(queueName);
            ResponseEntity<Map> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, HttpEntity.EMPTY, Map.class);
            return response.getBody();
        } catch (RestClientException exception) {
            return null;
        }
    }

    private String resolveStatus(long totalQueueCount, long deadCount, boolean hasUnavailableQueue) {
        if (hasUnavailableQueue) {
            return "NEEDS_ATTENTION";
        }
        if (deadCount > 0) {
            return "NEEDS_ATTENTION";
        }
        if (totalQueueCount > 0) {
            return "PROCESSING";
        }
        return "IDLE";
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "NEEDS_ATTENTION" -> "\uD655\uC778 \uD544\uC694";
            case "PROCESSING" -> "\uBC1C\uC1A1 \uCC98\uB9AC \uC911";
            default -> "\uD604\uC7AC \uB300\uAE30 \uC911";
        };
    }

    private String statusMessage(String status) {
        return switch (status) {
            case "NEEDS_ATTENTION" -> "\uC2E4\uD328 \uD050\uC5D0 \uD655\uC778\uD560 \uBA54\uC2DC\uC9C0\uAC00 \uC788\uC2B5\uB2C8\uB2E4.";
            case "PROCESSING" -> "\uB300\uB7C9 \uBA54\uC2DC\uC9C0\uB97C \uC21C\uCC28 \uCC98\uB9AC\uD558\uACE0 \uC788\uC2B5\uB2C8\uB2E4.";
            default -> "\uCC98\uB9AC\uD560 \uBA54\uC2DC\uC9C0\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4.";
        };
    }

    private long longValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:15672";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
