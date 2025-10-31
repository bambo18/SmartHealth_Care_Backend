package com.smarthealthdog.backend.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImgUtils {
    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontUrl;

    /**
     * AWS CloudFront URL 생성
     * @param key 이미지 키
     * @return 이미지 URL
     */
    public String getImgUrl(String key) {
        return cloudFrontUrl + "/" + key;
    }
}
