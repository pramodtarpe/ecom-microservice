package com.ecommerce.platform.common.logging;

import com.ecommerce.platform.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Logs operation metadata without serializing method arguments or return values.
 */
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("@within(org.springframework.web.bind.annotation.RestController) || "
            + "@within(org.springframework.stereotype.Service)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startedAt = System.nanoTime();
        InvocationContext context = currentContext();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String operation = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        log.info("operation.started operation={} httpMethod={} path={} principal={} actor={} authorities={} correlationId={}",
                operation,
                context.httpMethod(),
                context.path(),
                context.principal(),
                context.actor(),
                context.authorities(),
                context.correlationId());
        try {
            Object result = joinPoint.proceed();
            log.info("operation.completed operation={} durationMs={} principal={} actor={} correlationId={}",
                    operation,
                    elapsedMilliseconds(startedAt),
                    context.principal(),
                    context.actor(),
                    context.correlationId());
            return result;
        } catch (Throwable failure) {
            log.warn("operation.failed operation={} durationMs={} exceptionType={} principal={} actor={} correlationId={}",
                    operation,
                    elapsedMilliseconds(startedAt),
                    failure.getClass().getName(),
                    context.principal(),
                    context.actor(),
                    context.correlationId());
            throw failure;
        }
    }

    private static long elapsedMilliseconds(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static InvocationContext currentContext() {
        HttpServletRequest request = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            request = attributes.getRequest();
        }

        String principal = "anonymous";
        String actor = "anonymous";
        String authorities = "-";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            principal = authentication.getName();
            actor = principal;
            String roleAuthorities = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .sorted()
                    .collect(Collectors.joining(","));
            authorities = roleAuthorities.isBlank() ? "-" : roleAuthorities;
            if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
                Object actorClaim = jwtAuthentication.getToken().getClaims().get("actorSub");
                if (actorClaim instanceof String delegatedActor && !delegatedActor.isBlank()) {
                    actor = delegatedActor;
                }
            }
        }

        return new InvocationContext(
                request == null ? "-" : request.getMethod(),
                request == null ? "-" : request.getRequestURI(),
                principal,
                actor,
                authorities,
                request == null ? null : CorrelationIdFilter.currentCorrelationId(request));
    }

    private record InvocationContext(
            String httpMethod,
            String path,
            String principal,
            String actor,
            String authorities,
            String correlationId) {
    }
}
