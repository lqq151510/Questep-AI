package com.interview.common.constant;

public final class TaskConstants {
    private TaskConstants() {}

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    public static final String TYPE_MATERIAL_PARSE = "MATERIAL_PARSE";
    public static final String BIZ_TYPE_MATERIAL_PARSE = "MATERIAL_PARSE";
    public static final String BIZ_TYPE_QUIZ_GEN = "QUIZ_GEN";
    public static final String TASK_NO_PREFIX = "PARSE-";

    public static final String SOURCE_TYPE_AI = "AI";
    public static final String GATEWAY_NOOP = "noop-gateway";

    public static final String ROLE_USER = "ROLE_USER";

    public static final String QUESTION_REVIEW_STATUS_APPROVED = "APPROVED";
    public static final String QUESTION_REVIEW_STATUS_PENDING = "PENDING_REVIEW";
}
