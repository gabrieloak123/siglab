package br.ufrn.dimap.siglab.dto;

public record GooglePreviewResponse(boolean existingUser, String name, String email,
                                    String institutionalDomain) {}
