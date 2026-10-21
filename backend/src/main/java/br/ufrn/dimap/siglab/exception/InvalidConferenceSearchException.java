package br.ufrn.dimap.siglab.exception;

/** Indica filtros inválidos em uma busca de conferências. */
public class InvalidConferenceSearchException extends IllegalArgumentException {
    public InvalidConferenceSearchException(String message) { super(message); }
}
