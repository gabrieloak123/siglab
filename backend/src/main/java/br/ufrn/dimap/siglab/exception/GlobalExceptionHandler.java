package br.ufrn.dimap.siglab.exception;

import br.ufrn.dimap.siglab.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manipulador global de exceções para a API REST do SIGLab.
 * <p>
 * Captura exceções não tratadas nos controladores e retorna respostas HTTP estruturadas
 * utilizando o DTO {@link ErrorResponseDTO}.
 * </p>
 *
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceções de recursos não encontrados (HTTP 404 Not Found).
     *
     * @param ex a exceção {@link ResourceNotFoundException}
     * @param request a requisição HTTP original
     * @return {@link ResponseEntity} com status 404 e detalhes do erro
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Trata exceções de segurança e autorização (HTTP 403 Forbidden).
     *
     * @param ex a exceção {@link SecurityException}
     * @param request a requisição HTTP original
     * @return {@link ResponseEntity} com status 403 e detalhes do erro
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponseDTO> handleSecurityException(SecurityException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Trata exceções de argumentos ou regras de negócio inválidas (HTTP 400 Bad Request).
     *
     * @param ex a exceção {@link IllegalArgumentException} ou {@link IllegalStateException}
     * @param request a requisição HTTP original
     * @return {@link ResponseEntity} com status 400 e detalhes do erro
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(RuntimeException ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Trata erros de validação de DTOs anotados com {@code @Valid} (HTTP 400 Bad Request).
     *
     * @param ex a exceção {@link MethodArgumentNotValidException}
     * @param request a requisição HTTP original
     * @return {@link ResponseEntity} com status 400 e mapa de campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Erro de Validação")
                .message("Um ou mais campos da requisição possuem valores inválidos.")
                .path(request.getRequestURI())
                .validationErrors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Trata exceções inesperadas do sistema (HTTP 500 Internal Server Error).
     *
     * @param ex a exceção genérica {@link Exception}
     * @param request a requisição HTTP original
     * @return {@link ResponseEntity} com status 500 e mensagem genérica
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Ocorreu um erro interno inesperado no servidor: " + ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
