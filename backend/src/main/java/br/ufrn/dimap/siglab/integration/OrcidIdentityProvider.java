package br.ufrn.dimap.siglab.integration;

public interface OrcidIdentityProvider {
    record Identity(String orcid, String name) {}
    boolean configured();
    String authorizationUrl(String state);
    Identity exchange(String code);
}
