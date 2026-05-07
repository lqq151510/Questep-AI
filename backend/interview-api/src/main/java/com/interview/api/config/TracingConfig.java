package com.interview.api.config;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ServerHttpObservationFilter;

@Configuration
public class TracingConfig {
    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    @Bean
    public TraceIdLogger traceIdLogger(Tracer tracer) {
        return new TraceIdLogger(tracer);
    }

    public static class TraceIdLogger {
        private final Tracer tracer;

        public TraceIdLogger(Tracer tracer) {
            this.tracer = tracer;
        }

        public String getTraceId() {
            if (tracer == null || tracer.currentSpan() == null) {
                return "no-trace";
            }
            return tracer.currentSpan().context().traceId();
        }

        public String getSpanId() {
            if (tracer == null || tracer.currentSpan() == null) {
                return "no-span";
            }
            return tracer.currentSpan().context().spanId();
        }
    }
}

