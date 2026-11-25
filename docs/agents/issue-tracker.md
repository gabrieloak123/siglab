# Issue tracker: Local Markdown

As tarefas e especificações deste repositório ficam em arquivos Markdown sob `.scratch/`.

## Convenções

- Cada funcionalidade tem um diretório `.scratch/<funcionalidade>/`.
- A especificação fica em `.scratch/<funcionalidade>/spec.md`.
- Cada tarefa de implementação fica em `.scratch/<funcionalidade>/issues/<NN>-<nome>.md`, numerada a partir de `01`.
- O estado de triagem fica em uma linha `Status:` perto do início da tarefa. Os valores estão em `docs/agents/triage-labels.md`.
- Comentários e histórico de conversa são acrescentados ao fim do arquivo, sob `## Comments`.

## Quando um skill pedir para publicar no rastreador

Crie um arquivo em `.scratch/<funcionalidade>/`, criando o diretório quando necessário.

## Quando um skill pedir para buscar uma tarefa

Leia o arquivo no caminho indicado. O usuário normalmente informará o caminho ou o número da tarefa.

## Operações de wayfinding

O `/wayfinder` usa um arquivo de mapa e um arquivo por tarefa filha.

- **Mapa:** `.scratch/<trabalho>/map.md`, com Notes, Decisions-so-far e Fog.
- **Tarefa filha:** `.scratch/<trabalho>/issues/<NN>-<nome>.md`, numerada a partir de `01`. Uma linha `Type:` registra `research`, `prototype`, `grilling` ou `task`; uma linha `Status:` registra `claimed` ou `resolved`.
- **Bloqueio:** uma linha `Blocked by: NN, NN` perto do início. A tarefa fica desbloqueada quando todos os arquivos citados estão com `Status: resolved`.
- **Próxima tarefa:** procure em `.scratch/<trabalho>/issues/` a primeira tarefa, por número, que esteja aberta, desbloqueada e não assumida.
- **Assumir:** defina `Status: claimed` e salve antes de iniciar o trabalho.
- **Resolver:** acrescente a resposta sob `## Answer`, defina `Status: resolved` e acrescente às Decisions-so-far do mapa um resumo com o link para a tarefa.
