package com.interview.api.observability;

import com.interview.application.port.LlmGateway;
import com.interview.common.util.LlmProviderNormalizer;
import com.interview.domain.model.UserLlmSettings;
import com.interview.domain.repository.UserLlmSettingsRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
@Primary
public class ObservedLlmGateway implements LlmGateway {
    private static final Logger log = LoggerFactory.getLogger(ObservedLlmGateway.class);
    private static final long SLOW_CALL_THRESHOLD_MS = 3_000L;

    private final LlmGateway delegate;
    private final UserLlmSettingsRepository userLlmSettingsRepository;
    private final MeterRegistry meterRegistry;
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final String defaultProvider;
    private final String defaultModel;

    public ObservedLlmGateway(
            @Qualifier("multiVendorLlmGateway") LlmGateway delegate,
            UserLlmSettingsRepository userLlmSettingsRepository,
            MeterRegistry meterRegistry,
            ObservationRegistry observationRegistry,
            Tracer tracer,
            @Value("${app.llm.provider:anthropic}") String defaultProvider,
            @Value("${app.llm.model:claude-3-5-sonnet-latest}") String defaultModel
    ) {
        this.delegate = delegate;
        this.userLlmSettingsRepository = userLlmSettingsRepository;
        this.meterRegistry = meterRegistry;
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.defaultProvider = LlmProviderNormalizer.normalize(defaultProvider);
        this.defaultModel = normalizeModel(defaultModel);
    }

    @Override
    public String chat(Long userId, String prompt) {
        LlmRoute route = resolveRoute(userId);
        Observation observation = Observation.start("llm.call", observationRegistry)
                .lowCardinalityKeyValue("provider", route.provider())
                .lowCardinalityKeyValue("model", route.model())
                .highCardinalityKeyValue("user_id_masked", maskUserId(userId))
                .highCardinalityKeyValue("call_type", "chat");

        long startNanos = System.nanoTime();
        String status = "success";
        String errorType = "none";

        try (Observation.Scope scope = observation.openScope()) {
            return delegate.chat(userId, prompt);
        } catch (RuntimeException ex) {
            status = "failure";
            errorType = ex.getClass().getSimpleName();
            observation.error(ex);
            throw ex;
        } finally {
            recordMetricsAndLog(route, userId, "chat", startNanos, status, errorType);
            observation.lowCardinalityKeyValue("status", status);
            observation.stop();
        }
    }

    @Override
    public void chatStream(Long userId, String prompt, Consumer<String> tokenConsumer) {
        LlmRoute route = resolveRoute(userId);
        Observation observation = Observation.start("llm.call", observationRegistry)
                .lowCardinalityKeyValue("provider", route.provider())
                .lowCardinalityKeyValue("model", route.model())
                .highCardinalityKeyValue("user_id_masked", maskUserId(userId))
                .highCardinalityKeyValue("call_type", "stream");

        long startNanos = System.nanoTime();
        String status = "success";
        String errorType = "none";

        try (Observation.Scope scope = observation.openScope()) {
            delegate.chatStream(userId, prompt, tokenConsumer);
        } catch (RuntimeException ex) {
            status = "failure";
            errorType = ex.getClass().getSimpleName();
            observation.error(ex);
            throw ex;
        } finally {
            recordMetricsAndLog(route, userId, "stream", startNanos, status, errorType);
            observation.lowCardinalityKeyValue("status", status);
            observation.stop();
        }
    }

    private LlmRoute resolveRoute(Long userId) {
        Optional<UserLlmSettings> settings = Optional.ofNullable(userId)
                .flatMap(userLlmSettingsRepository::findByUserId)
                .filter(item -> item.enabled() == null || item.enabled() == 1);

        String provider = settings.map(UserLlmSettings::providerName)
                .map(LlmProviderNormalizer::normalize)
                .orElse(defaultProvider);
        String model = settings.map(UserLlmSettings::modelName)
                .map(this::normalizeModel)
                .orElse(defaultModel);
        return new LlmRoute(sanitizeTag(provider), sanitizeTag(model));
    }

    private void recordMetricsAndLog(
            LlmRoute route,
            Long userId,
            String callType,
            long startNanos,
            String status,
            String errorType
    ) {
        long durationNanos = System.nanoTime() - startNanos;
        long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);

        meterRegistry.counter(
                "llm_call_total",
                "provider", route.provider(),
                "model", route.model(),
                "status", status,
                "call_type", callType
        ).increment();

        Timer.builder("llm_call_latency")
                .tag("provider", route.provider())
                .tag("model", route.model())
                .tag("status", status)
                .tag("call_type", callType)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);

        if ("failure".equals(status)) {
            meterRegistry.counter(
                    "llm_call_error_total",
                    "provider", route.provider(),
                    "model", route.model(),
                    "error_type", sanitizeTag(errorType),
                    "call_type", callType
            ).increment();
        }

        String traceId = currentTraceId();
        if ("failure".equals(status) || durationMs >= SLOW_CALL_THRESHOLD_MS) {
            log.warn(
                    "llm_call callType={} provider={} model={} status={} durationMs={} user={} errorType={} traceId={}",
                    callType,
                    route.provider(),
                    route.model(),
                    status,
                    durationMs,
                    maskUserId(userId),
                    errorType,
                    traceId
            );
            return;
        }
        log.info(
                "llm_call callType={} provider={} model={} status={} durationMs={} user={} traceId={}",
                callType,
                route.provider(),
                route.model(),
                status,
                durationMs,
                maskUserId(userId),
                traceId
        );
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        if (span == null || span.context() == null || span.context().traceId() == null) {
            return "no-trace";
        }
        return span.context().traceId();
    }

    private String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "unknown";
        }
        return model.trim();
    }

    private String sanitizeTag(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._:-]", "_");
        if (normalized.length() > 64) {
            return normalized.substring(0, 64);
        }
        return normalized;
    }

    private String maskUserId(Long userId) {
        if (userId == null) {
            return "anonymous";
        }
        String raw = String.valueOf(userId);
        if (raw.length() <= 2) {
            return "u**" + raw;
        }
        return "u**" + raw.substring(raw.length() - 2);
    }

    private record LlmRoute(String provider, String model) {
    }
}
