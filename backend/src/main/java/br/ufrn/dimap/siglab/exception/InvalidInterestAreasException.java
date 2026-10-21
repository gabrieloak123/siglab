package br.ufrn.dimap.siglab.exception;

/** Indica que as áreas de interesse não atendem às regras do domínio. */
public class InvalidInterestAreasException extends IllegalArgumentException {
    public InvalidInterestAreasException(String message) { super(message); }
}
