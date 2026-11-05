package br.com.fiap.postech.mspedidos.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Populates the logging MDC with the correlation identifiers used by the platform.
 */
@Component
public class MdcRequestFilter extends OncePerRequestFilter {

    static final String CORRELATION_ID = "correlation_id";
    static final String SERVICE_ORDER_ID = "so_id";
    static final String BRANCH_ID = "branch_id";
    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String BRANCH_ATTRIBUTE = "branchId";

    private static final Pattern SERVICE_ORDER_PATH = Pattern.compile("/service-orders/(\\d+)");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String correlationId = request.getHeader(CORRELATION_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(CORRELATION_ID, correlationId);
            response.setHeader(CORRELATION_HEADER, correlationId);

            Matcher matcher = SERVICE_ORDER_PATH.matcher(request.getRequestURI());
            if (matcher.find()) {
                MDC.put(SERVICE_ORDER_ID, matcher.group(1));
            }

            Object branchId = request.getAttribute(BRANCH_ATTRIBUTE);
            if (branchId != null) {
                MDC.put(BRANCH_ID, String.valueOf(branchId));
            }

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CORRELATION_ID);
            MDC.remove(SERVICE_ORDER_ID);
            MDC.remove(BRANCH_ID);
        }
    }
}
