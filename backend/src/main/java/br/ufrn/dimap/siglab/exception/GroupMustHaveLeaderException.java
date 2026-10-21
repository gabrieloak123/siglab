package br.ufrn.dimap.siglab.exception;

/** Impede uma operação que deixaria um grupo de pesquisa sem líder. */
public class GroupMustHaveLeaderException extends IllegalStateException {
    public GroupMustHaveLeaderException(String message) { super(message); }
}
