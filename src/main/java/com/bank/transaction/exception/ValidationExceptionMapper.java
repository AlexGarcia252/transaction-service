package com.bank.transaction.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    private static final Logger LOG = Logger.getLogger(ValidationExceptionMapper.class);

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Extraemos los errores de validación exactos
        List<String> errors = exception.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.toList());

        // Log estructurado automático con el correlationId incluido
        LOG.warnv("Petición inválida detectada. Errores: {0}", errors);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(
                        "error", "Fallo de validación de datos",
                        "correlationId", MDC.get("correlationId"),
                        "details", errors
                )).build();
    }
}