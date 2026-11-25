# Documentação de domínio

## Antes de explorar o código

- Leia `GLOSSARY.md` na raiz, quando existir.
- Leia os ADRs em `docs/adr/` relacionados à área em que vai trabalhar.

Esses arquivos são criados conforme os termos e as decisões forem definidos. A ausência deles não impede a exploração.

## Estrutura

Este repositório usa um único contexto de domínio:

```text
/
├── GLOSSARY.md
└── docs/
    └── adr/
        └── <numero>-<decisao>.md
```

## Vocabulário

Ao nomear conceitos em tarefas, propostas e testes, use os termos definidos em `GLOSSARY.md`. Se faltar um termo, registre a lacuna para o skill `/domain-modeling`.

## Conflitos com ADRs

Se uma proposta contrariar um ADR existente, identifique o ADR e explique o motivo para reconsiderar a decisão.
