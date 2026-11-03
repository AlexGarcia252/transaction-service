package com.bank.transaction.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.MDC;
import java.util.UUID;

@Provider
public class CorrelationIdFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext context) {
        // Leemos el Correlation-ID si viene en la cabecera, si no, generamos uno
        String correlationId = context.getHeaderString("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Lo inyectamos en el contexto de los logs
        MDC.put("correlationId", correlationId);

        // Lo dejamos disponible en las cabeceras para que el RestClient lo pueda propagar
        context.getHeaders().add("X-Correlation-ID", correlationId);
    }
}