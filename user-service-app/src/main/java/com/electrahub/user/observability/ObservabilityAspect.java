package com.electrahub.user.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

@Aspect
@Component
public class ObservabilityAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityAspect.class);
    private static final int MAX_LOG_VALUE_LENGTH = 300;
    private static final long SLOW_CALL_THRESHOLD_MS = 1_000;
    private static final Set<String> SENSITIVE_PARAM_NAMES = Set.of(
            "password", "token", "secret", "authorization", "apiKey", "newPassword", "oldPassword"
    );

    private final MeterRegistry meterRegistry;

    /**
     * Executes observability aspect for `ObservabilityAspect`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.observability`.
     * @param meterRegistry input consumed by ObservabilityAspect.
     */
    public ObservabilityAspect(MeterRegistry meterRegistry) {
        LOGGER.info("Observability aspect initialized for Spring beans and Micrometer metrics");
        this.meterRegistry = meterRegistry;
    }

    /**
     * Executes observe for `ObservabilityAspect`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.observability`.
     * @param joinPoint input consumed by observe.
     * @return result produced by observe.
     */
    @Around("execution(public * com.electrahub..*.*(..)) && " +
            "(within(@org.springframework.web.bind.annotation.RestController *) || " +
            "within(@org.springframework.stereotype.Controller *) || " +
            "within(@org.springframework.stereotype.Service *) || " +
            "within(@org.springframework.stereotype.Repository *) || " +
            "within(@org.springframework.stereotype.Component *)) && " +
            "!within(com.electrahub..observability..*)")
    public Object observe(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String component = resolveComponent(signature);
        long startedAt = System.nanoTime();

        Counter.builder("electrahub.method.invocations")
                .description("Total method invocations for Spring-managed application beans")
                .tag("component", component)
                .tag("class", className)
                .tag("method", methodName)
                .register(meterRegistry)
                .increment();

        LOGGER.info("{} call started: {}.{}", component, className, methodName);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} call input: {}.{} {}", component, className, methodName, formatArgs(joinPoint));
        }

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            Object result = joinPoint.proceed();
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            sample.stop(Timer.builder("electrahub.method.duration")
                    .description("Method execution duration for Spring-managed application beans")
                    .tag("component", component)
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("outcome", "success")
                    .register(meterRegistry));

            LOGGER.info("{} call succeeded: {}.{} in {} ms", component, className, methodName, durationMs);
            if (durationMs >= SLOW_CALL_THRESHOLD_MS) {
                LOGGER.warn("Slow {} call: {}.{} took {} ms", component.toLowerCase(Locale.ROOT), className, methodName, durationMs);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{} call output: {}.{} -> {}", component, className, methodName, abbreviate(result));
            }
            return result;
        } catch (Throwable ex) {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

            sample.stop(Timer.builder("electrahub.method.duration")
                    .description("Method execution duration for Spring-managed application beans")
                    .tag("component", component)
                    .tag("class", className)
                    .tag("method", methodName)
                    .tag("outcome", "failure")
                    .register(meterRegistry));

            Counter.builder("electrahub.method.failures")
                    .description("Total failed method invocations for Spring-managed application beans")
                    .tag("component", component)
                    .tag("class", className)
                    .tag("method", methodName)
                    .register(meterRegistry)
                    .increment();

            LOGGER.warn("{} call failed: {}.{} in {} ms due to {}", component, className, methodName, durationMs, ex.toString());
            LOGGER.debug("Failure stack trace for {}.{}", className, methodName, ex);
            throw ex;
        }
    }

    /**
     * Executes format args for `ObservabilityAspect`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.observability`.
     * @param joinPoint input consumed by formatArgs.
     * @return result produced by formatArgs.
     */
    private String formatArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "[]";
        }
        String[] parameterNames = ((CodeSignature) joinPoint.getSignature()).getParameterNames();
        return IntStream.range(0, args.length)
                .mapToObj(index -> sanitizeArg(parameterNames, args[index], index))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String sanitizeArg(String[] parameterNames, Object value, int index) {
        String name = (parameterNames != null && index >= 0 && index < parameterNames.length)
                ? parameterNames[index]
                : "arg" + Math.max(index, 0);
        if (isSensitive(name)) {
            return name + "=<redacted>";
        }
        return name + "=" + abbreviate(value);
    }


    private boolean isSensitive(String parameterName) {
        String normalized = parameterName == null ? "" : parameterName.toLowerCase(Locale.ROOT);
        return SENSITIVE_PARAM_NAMES.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }

    private String resolveComponent(MethodSignature signature) {
        Class<?> declaringType = signature.getDeclaringType();
        if (declaringType.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class)
                || declaringType.isAnnotationPresent(org.springframework.stereotype.Controller.class)) {
            return "HTTP";
        }
        if (declaringType.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
            return "SERVICE";
        }
        if (declaringType.isAnnotationPresent(org.springframework.stereotype.Repository.class)) {
            return "REPOSITORY";
        }
        return "COMPONENT";
    }

    /**
     * Executes abbreviate for `ObservabilityAspect`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.user.observability`.
     * @param value input consumed by abbreviate.
     * @return result produced by abbreviate.
     */
    private String abbreviate(Object value) {
        if (value == null) {
            return "null";
        }
        String text;
        try {
            text = value.toString();
        } catch (Exception ex) {
            return value.getClass().getSimpleName();
        }
        if (text.length() <= MAX_LOG_VALUE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LOG_VALUE_LENGTH) + "...";
    }
}
