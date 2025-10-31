package com.smarthealthdog.backend.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealthdog.backend.domain.PetSpecies;
import com.smarthealthdog.backend.dto.diagnosis.celery.CeleryMessage;
import com.smarthealthdog.backend.dto.diagnosis.celery.DeliveryInfo;
import com.smarthealthdog.backend.dto.diagnosis.celery.MessageHeaders;
import com.smarthealthdog.backend.dto.diagnosis.celery.MessageProperties;
import com.smarthealthdog.backend.dto.diagnosis.create.RequestDiagnosisData;

@Component
@Profile("dev")
public class CeleryTaskSender implements DiagnosisTaskRequestClient {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Define constants for your task and queue
    private static final String DOG_DIAGNOSIS_TASK_NAME = "smart_health_dog_disease_detection.predict_dog_disease";
    private static final String CAT_DIAGNOSIS_TASK_NAME = "smart_health_dog_disease_detection.predict_cat_disease";
    private static final String CELERY_QUEUE_NAME = "celery"; 

    @Autowired
    public CeleryTaskSender(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // /**
    //  * (비동기) 주어진 이미지 경로와 반려동물 ID, 업데이트 토큰으로 Celery 작업을 Redis에 비동기적으로 전송합니다.
    //  * @param imagePath CloudFront 이미지 경로
    //  * @param petId 반려동물 ID
    //  * @param species 반려동물 종류
    //  * @param updateToken 업데이트 토큰
    //  */
    // @Override
    // public void sendDiagnosisTask(String imagePath, Long submissionId, PetSpecies species, String updateToken) {
    //     try {
    //         String taskId = UUID.randomUUID().toString();
    //         String payloadBase64 = generateCeleryBody(imagePath, submissionId, updateToken);
    //         String taskName = species == PetSpecies.DOG ? DOG_DIAGNOSIS_TASK_NAME : CAT_DIAGNOSIS_TASK_NAME;

    //         // 1. Create nested objects
    //         DeliveryInfo deliveryInfo = DeliveryInfo.builder()
    //             .exchange("") // Default exchange
    //             .routingKey(CELERY_QUEUE_NAME)
    //             .build();

    //         MessageProperties properties = MessageProperties.builder()
    //             .correlationId(taskId)
    //             .deliveryTag(taskName)
    //             .deliveryInfo(deliveryInfo)
    //             .deliveryMode(2)
    //             .bodyEncoding("base64")
    //             .priority(0)
    //             .build();
            
    //         // The imagePath needs to be formatted for 'argsrepr' (e.g., "('./input/cataract1.jpg',)")
    //         String argsRepr = String.format("('%s','%s','%s')", imagePath, submissionId.toString(), updateToken);

    //         MessageHeaders headers = MessageHeaders.builder()
    //             .lang("py")
    //             .task(taskName)
    //             .id(taskId)
    //             .rootId(taskId)
    //             .argsRepr(argsRepr)
    //             .kwargsRepr("{}")
    //             .ignoreResult(false)
    //             .retries(0)
    //             .timeLimit(new Object[]{null, null})
    //             .parentId(null)
    //             .build();
            
    //         // 2. Create the Final Message Dictionary
    //         CeleryMessage finalMessage = CeleryMessage.builder()
    //             .body(payloadBase64)
    //             .contentEncoding("utf-8")
    //             .contentType("application/json")
    //             .headers(headers)
    //             .properties(properties)
    //             .build();

    //         // 3. Serialize the final dictionary to the string that goes into Redis
    //         String finalMessageStr = objectMapper.writeValueAsString(finalMessage);

    //         // 4. Connect to Redis and push the message (RPUSH)
    //         // LTRIM/ping check is done by Spring's connection health by default
            
    //         // redisTemplate.opsForList().rightPush(key, value)
    //         stringRedisTemplate.opsForList().rightPush(CELERY_QUEUE_NAME, finalMessageStr);
    //     } catch (Exception e) {
    //         // TODO: Connect to Sentry or logging system and log the exception
    //         e.printStackTrace();
    //     }
    // }

    /**
     * (비동기) 여러 개의 진단 작업을 한 번에 Redis에 전송합니다.
     * @param requestDataList 진단 작업 데이터 리스트
     */
    @Override
    public void sendDiagnosisTaskInBatch(List<RequestDiagnosisData> requestDataList) {
        try {
            List<String> celeryMessages = requestDataList.stream()
                .map(data -> {
                    try {
                        return generateCeleryMessage(
                            data.imageUrl(),
                            data.submissionId(),
                            data.species()
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

            // Push all messages to Redis in a single operation
            stringRedisTemplate.opsForList().rightPushAll(CELERY_QUEUE_NAME, celeryMessages);
        } catch (Exception e) {
            // TODO: Connect to Sentry or logging system and log the exception
            throw e;
        }
    }

    /**
     * 파이썬 Celery 작업 메시지를 생성합니다.
     * @param imagePath CloudFront 이미지 경로
     * @param submissionId 서브미션 ID
     * @param species 반려동물 종류
     * @param updateToken 업데이트 토큰
     * @return 직렬화된 Celery 메시지 문자열
     * @throws JsonProcessingException
     */
    private String generateCeleryMessage(
        String imagePath, Long submissionId, PetSpecies species
    ) throws JsonProcessingException {
        try {
            String taskId = UUID.randomUUID().toString();
            String payloadBase64 = generateCeleryBody(imagePath, submissionId);
            String taskName = species == PetSpecies.DOG ? DOG_DIAGNOSIS_TASK_NAME : CAT_DIAGNOSIS_TASK_NAME;

            // 1. Create nested objects
            DeliveryInfo deliveryInfo = DeliveryInfo.builder()
                .exchange("") // Default exchange
                .routingKey(CELERY_QUEUE_NAME)
                .build();

            MessageProperties properties = MessageProperties.builder()
                .correlationId(taskId)
                .deliveryTag(taskName)
                .deliveryInfo(deliveryInfo)
                .deliveryMode(2)
                .bodyEncoding("base64")
                .priority(0)
                .build();
            
            // The imagePath needs to be formatted for 'argsrepr' (e.g., "('./input/cataract1.jpg',)")
            String argsRepr = String.format("('%s','%s')", imagePath, submissionId.toString());

            MessageHeaders headers = MessageHeaders.builder()
                .lang("py")
                .task(taskName)
                .id(taskId)
                .rootId(taskId)
                .argsRepr(argsRepr)
                .kwargsRepr("{}")
                .ignoreResult(false)
                .retries(0)
                .timeLimit(new Object[]{null, null})
                .parentId(null)
                .build();
            
            // 2. Create the Final Message Dictionary
            CeleryMessage finalMessage = CeleryMessage.builder()
                .body(payloadBase64)
                .contentEncoding("utf-8")
                .contentType("application/json")
                .headers(headers)
                .properties(properties)
                .build();

            // 3. Serialize the final dictionary to the string that goes into Redis
            return objectMapper.writeValueAsString(finalMessage);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 파이썬 Celery 작업의 바디를 생성합니다.
     * @param imagePath CloudFront 이미지 경로
     * @param submissionId 서브미션 ID
     * @param updateToken 업데이트 토큰
     * @return
     * @throws JsonProcessingException
     */
    private String generateCeleryBody(
        String imagePath, Long submissionId
    ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        // 1. Construct the Decoded Task Body (Payload)
        // Structure: [args, kwargs, embed_options]
        
        // Positional args: [imagePath]
        Object[] args = new Object[]{imagePath, submissionId.toString()};
        
        // Keyword args: {}
        Map<String, Object> kwargs = new HashMap<>();

        // Task options (Celery 4.x/5.x format)
        Map<String, Object> options = new HashMap<>();
        options.put("callbacks", null);
        options.put("errbacks", null);
        options.put("chain", null);
        options.put("chord", null);

        // The full payload array
        Object[] taskPayload = new Object[]{args, kwargs, options};

        // 2. Serialize and Base64-encode the body
        String payloadJson = objectMapper.writeValueAsString(taskPayload);
        
        // Encode the JSON string to a byte array using UTF-8, then Base64 encode it
        String payloadBase64 = Base64.getEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return payloadBase64;
    }
}
