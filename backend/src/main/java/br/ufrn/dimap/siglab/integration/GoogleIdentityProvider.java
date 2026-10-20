package br.ufrn.dimap.siglab.integration;

public interface GoogleIdentityProvider {
    record Identity(String subject, String email, String name, String hostedDomain) {}
    Identity verify(String credential);
}
