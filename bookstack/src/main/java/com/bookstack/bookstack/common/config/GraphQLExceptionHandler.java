package com.bookstack.bookstack.common.config;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;

import com.bookstack.bookstack.common.exception.BaseException;

import java.util.stream.Collectors;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(@NonNull Throwable ex, @NonNull DataFetchingEnvironment env) {

        System.out.println("GraphQLExceptionHandler: " + ex.getClass().getName() + " - " + ex.getMessage());
        
        // Handle validation errors (Case 1)
        if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException violationException = (ConstraintViolationException) ex;
            
            String violations = violationException.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
                
            return GraphqlErrorBuilder.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message("Validation failed: " + violations)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }
        
        // Handle business logic errors (Case 2)
        if (ex instanceof BaseException) {
            BaseException baseEx = (BaseException) ex;
            ErrorType errorType = mapHttpStatusToErrorType(baseEx.getStatus());
            
            return GraphqlErrorBuilder.newError()
                .errorType(errorType)
                .message(baseEx.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
        }
        
        return null; // Let other handlers deal with it
    }
    
    private ErrorType mapHttpStatusToErrorType(org.springframework.http.HttpStatus status) {
        return switch (status.value()) {
            case 400 -> ErrorType.BAD_REQUEST;
            case 401 -> ErrorType.UNAUTHORIZED;
            case 403 -> ErrorType.FORBIDDEN;
            case 404 -> ErrorType.NOT_FOUND;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }
}