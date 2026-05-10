package com.interview.application.dto;

public record CaptchaResponse(
        String captchaId,
        String captchaCode
) {
}
