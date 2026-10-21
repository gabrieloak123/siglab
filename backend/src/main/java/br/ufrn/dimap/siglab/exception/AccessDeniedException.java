package br.ufrn.dimap.siglab.exception;

/** Indica que o usuário está autenticado, mas não pode executar a operação. */
public class AccessDeniedException extends SecurityException {
    public AccessDeniedException(String message) { super(message); }
}
