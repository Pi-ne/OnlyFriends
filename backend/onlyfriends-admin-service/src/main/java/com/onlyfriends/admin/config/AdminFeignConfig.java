package com.onlyfriends.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.Result;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class AdminFeignConfig {
    @Bean
    public ErrorDecoder adminFeignErrorDecoder(ObjectMapper objectMapper) {
        return new AdminFeignErrorDecoder(objectMapper);
    }

    private static final class AdminFeignErrorDecoder implements ErrorDecoder {
        private final ObjectMapper objectMapper;
        private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

        private AdminFeignErrorDecoder(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Exception decode(String methodKey, Response response) {
            Result<?> result = readResult(response);
            if (result != null && result.getCode() != null) {
                return new BizException(result.getCode(), resolveMessage(result));
            }
            return defaultDecoder.decode(methodKey, response);
        }

        private Result<?> readResult(Response response) {
            if (response.body() == null) {
                return null;
            }
            try (InputStream inputStream = response.body().asInputStream()) {
                return objectMapper.readValue(inputStream, Result.class);
            } catch (IOException ex) {
                return null;
            }
        }

        private String resolveMessage(Result<?> result) {
            if (StringUtils.hasText(result.getMessage())) {
                return result.getMessage();
            }
            return "下游服务调用失败";
        }
    }
}
