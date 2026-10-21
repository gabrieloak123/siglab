package br.ufrn.dimap.siglab.exception;

public class AuthenticationRequiredException extends SecurityException {
    public AuthenticationRequiredException(String message) { super(message); }
}
