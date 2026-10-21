# Relatório de mudanças — Sprint 3

## 1. Objetivo

Este documento registra as mudanças implementadas no SIGLab para a Sprint 3, incluindo autenticação, integração com Google Scholar, recomendações vetoriais, relacionamentos de seguidores, feed de posts, classificação de conteúdo, banco de dados, interface e testes.

A regra funcional mais recente solicitada após a implementação inicial também está contemplada: o `author_id` do Google Scholar passou a ser opcional; a exigência para criar uma conta é informar exatamente três áreas de interesse distintas.

## 2. Resumo funcional

Foram adicionadas as seguintes capacidades:

- cadastro e login com sessão HTTP;
- entrada e vinculação de conta pelo Google Identity Services;
- consulta opcional de áreas públicas do Google Scholar via SerpApi;
- validação de exatamente três áreas de interesse por usuário e três linhas de pesquisa por grupo;
- geração de embeddings com Cloudflare Workers AI e o modelo `@cf/baai/bge-m3`;
- armazenamento e consulta vetorial com PostgreSQL e pgvector;
- recomendação de pesquisadores, grupos e posts por similaridade;
- seguir e deixar de seguir pesquisadores e grupos;
- feed geral e feed restrito a entidades seguidas;
- diferenciação entre posts dos tipos `ARTIGO` e `OUTROS`;
- uso das palavras-chave fornecidas pelo autor em artigos;
- geração de três palavras-chave pelo Gemini em posts do tipo `OUTROS`;
- indexação retroativa de usuários, grupos e posts antigos;
- telas e componentes para autenticação, perfil, sugestões e feed;
- tratamento padronizado de erros de serviços externos e regras de negócio;
- testes unitários das regras implementadas.

## 3. Backend

### 3.1. Autenticação e sessão

Foram criados:

- `AuthController`, com endpoints de cadastro, login, login Google, vinculação Google, logout, sessão atual e consulta Scholar;
- `AuthService`, responsável pelas regras de cadastro, credenciais, identidade Google, associação de perfil e áreas de interesse;
- `SessionUser`, responsável por recuperar e validar o usuário da sessão;
- DTOs `LoginRequest`, `RegistrationRequest` e `GoogleLoginRequest`.

Endpoints adicionados:

| Método | Endpoint | Função |
|---|---|---|
| `POST` | `/api/auth/register` | Cadastra e inicia a sessão |
| `POST` | `/api/auth/login` | Autentica com e-mail e senha |
| `POST` | `/api/auth/google` | Autentica ou cadastra com credencial Google |
| `POST` | `/api/auth/google/link` | Vincula uma conta Google à conta autenticada |
| `POST` | `/api/auth/logout` | Encerra a sessão |
| `GET` | `/api/auth/me` | Retorna o usuário autenticado |
| `GET` | `/api/auth/scholar/{authorId}` | Consulta áreas públicas no Scholar |

A sessão guarda o ID interno do usuário no servidor. Uma nova autenticação invalida a sessão anterior antes de criar outra.

No vínculo de uma conta existente, o e-mail verificado pelo Google deve coincidir com o e-mail cadastrado. O campo `sub` da credencial Google é armazenado como identificador estável da conta.

### 3.2. Regra atual de áreas e Google Scholar

Inicialmente, o fluxo exigia `author_id`. Após a alteração solicitada, a regra final ficou assim:

- `author_id` é opcional no cadastro comum, no primeiro acesso Google e na edição do perfil;
- exatamente três áreas distintas e não vazias são obrigatórias;
- quando `author_id` é informado, as áreas públicas retornadas pelo Scholar são usadas primeiro e podem ser completadas manualmente até chegar a três;
- quando `author_id` não é informado, as três áreas são fornecidas manualmente;
- o `author_id`, quando presente, continua único no banco;
- usuários já vinculados ao Google entram sem preencher os dados novamente.

O `InterestService` centraliza a limpeza, deduplicação e validação das áreas, além de solicitar a indexação vetorial.

### 3.3. Integrações externas desacopladas

As chamadas externas foram separadas da camada de serviços de negócio:

- `ScholarClient`: consulta perfis públicos na SerpApi usando o mecanismo `google_scholar_author`;
- `GoogleIdentityClient`: valida JWT, assinatura, expiração, emissor, audiência e e-mail verificado;
- `EmbeddingClient`: chama o modelo `@cf/baai/bge-m3` da Cloudflare e valida vetores de 1024 dimensões;
- `KeywordClient`: solicita ao Gemini exatamente três palavras-chave para posts do tipo `OUTROS`.

Foi criada `ExternalServiceException` para representar indisponibilidade, configuração ausente ou resposta inválida dessas integrações. O `GlobalExceptionHandler` passou a tratar essa exceção e outros erros de regra e segurança com respostas JSON consistentes.

### 3.4. Recomendações e seguidores

Foram criados:

- `SocialController`;
- `SocialService`;
- `FollowRepository`;
- `VectorRepository`;
- `IndexBackfillService`.

Endpoints adicionados:

| Método | Endpoint | Função |
|---|---|---|
| `GET` | `/api/social/suggestions/researchers` | Sugere pesquisadores não seguidos |
| `GET` | `/api/social/suggestions/groups` | Sugere grupos não seguidos |
| `GET` | `/api/social/feed` | Retorna posts ordenados por similaridade e data |
| `GET` | `/api/social/following` | Lista pesquisadores e grupos seguidos |
| `POST/DELETE` | `/api/social/researchers/{id}/follow` | Segue ou deixa de seguir pesquisador |
| `POST/DELETE` | `/api/social/groups/{id}/follow` | Segue ou deixa de seguir grupo |

Regras aplicadas:

- não é permitido seguir a si mesmo;
- pesquisadores e grupos precisam existir;
- as operações de seguir são idempotentes;
- sugestões excluem pesquisadores e grupos já seguidos;
- sugestões de grupos também excluem grupos dos quais o usuário já é membro;
- apenas os três candidatos mais similares, considerando pesquisadores e grupos em conjunto, são retornados;
- o feed é ordenado por distância vetorial, depois por data e ID decrescentes;
- o parâmetro `followedOnly=true` restringe o feed a posts de pesquisadores ou grupos seguidos.

O cálculo usa distância cosseno do pgvector por meio do operador `<=>`.

### 3.5. Indexação vetorial e retrocompatibilidade

O `VectorRepository` grava vetores com operação de inserção ou atualização (`upsert`) e rejeita:

- vetores com dimensão diferente de 1024;
- valores `NaN` ou infinitos.

O `IndexBackfillService` detecta entidades antigas sem vetor e cria índices sob demanda quando o usuário acessa sugestões ou o feed. Posts antigos sem palavras-chave são tratados como `OUTROS`, classificados pelo Gemini e indexados.

Usuários são reindexados após mudanças nas áreas de interesse. Grupos são reindexados após criação ou mudança nas linhas de pesquisa. Posts são reindexados após criação ou edição.

### 3.6. Posts

O modelo `Post` e os DTOs de criação e atualização passaram a incluir:

- `postType`;
- `keywords`;
- identificador do usuário publicador.

Regras implementadas no `PostService`:

- o tipo precisa ser `ARTIGO` ou `OUTROS`;
- artigos precisam ter ao menos uma palavra-chave válida fornecida pelo autor;
- posts `OUTROS` recebem exatamente três palavras-chave geradas pelo Gemini;
- as palavras-chave são convertidas em embedding após criação e edição;
- apenas líderes do grupo podem criar, editar ou excluir posts;
- a leitura exige usuário conhecido;
- o post consultado precisa pertencer ao grupo indicado na rota.

O `PostController` foi simplificado para delegar regras ao serviço, mantendo a arquitetura em camadas.

### 3.7. Usuários e grupos

O modelo `User` passou a possuir:

- `authorId`, único e opcional;
- `googleSubject`, único e oculto das respostas JSON.

O `UserRepository` recebeu consultas por `authorId` e `googleSubject`.

O `UserService` passou a:

- permitir cadastro sem Scholar;
- exigir três áreas distintas;
- validar unicidade do `author_id` apenas quando informado;
- reindexar o usuário após criação ou atualização;
- preservar e criptografar senhas corretamente em atualizações.

O `ResearchGroupService` passou a exigir exatamente três linhas de pesquisa e indexar o grupo após criação ou atualização.

Os controllers de usuários e grupos foram ajustados para a sessão e as novas regras de negócio.

## 4. Banco de dados

Foi adicionado `schema.sql` com:

- ativação da extensão `vector`;
- colunas `author_id` e `google_subject` em `users`;
- índices únicos para os identificadores opcionais;
- colunas `post_type` e `keywords` em `posts`;
- tabela `researcher_follows`;
- tabela `group_follows`;
- tabela `user_vectors` com `vector(1024)`;
- tabela `group_vectors` com `vector(1024)`;
- tabela `post_vectors` com `vector(1024)`;
- chaves estrangeiras e exclusão em cascata;
- restrição que impede um pesquisador de seguir a si mesmo.

A imagem do PostgreSQL no Compose foi alterada para `pgvector/pgvector:pg16`.

Foi corrigida uma falha encontrada no primeiro teste com banco vazio: o `schema.sql` era executado antes de o Hibernate criar as tabelas de entidade. A propriedade `spring.jpa.defer-datasource-initialization` foi alterada para `true`, fazendo a migração complementar ocorrer depois da inicialização JPA.

## 5. Frontend

### 5.1. Cliente de API e autenticação

O cliente em `frontend/lib/api.ts` foi reorganizado e recebeu:

- classe `ApiError` com status e mensagem do backend;
- leitura padronizada das mensagens JSON de erro;
- envio de cookies de sessão com `credentials: "include"`;
- APIs de autenticação, Scholar, seguidores, sugestões e feed;
- tipos de payload para posts e demais operações.

O hook `useAuth` passou a:

- restaurar e validar a sessão ao abrir a aplicação;
- cadastrar, autenticar e sair pelo backend;
- autenticar com Google;
- atualizar os dados armazenados após alteração de perfil;
- limpar o armazenamento local quando a sessão não é mais válida.

### 5.2. Modal de autenticação

O `AuthModal` passou a oferecer:

- login por e-mail e senha;
- cadastro com nome, e-mail, senha e três áreas;
- entrada via Google Identity Services;
- campo opcional de `author_id`;
- busca opcional das áreas no Scholar;
- preenchimento manual das áreas ausentes;
- validação no cliente de três áreas distintas no cadastro;
- mensagens específicas de validação e falhas de integração.

O texto da interface foi atualizado para deixar claro que o Scholar é opcional e que as três áreas são obrigatórias apenas no primeiro cadastro/acesso.

### 5.3. Perfil

O `ProfileModal` recebeu:

- edição opcional do `author_id`;
- edição das três áreas de interesse;
- botão para vincular uma conta Google;
- exigência de correspondência de e-mail no vínculo.

O alerta da página inicial agora é exibido somente quando faltam as três áreas, não quando falta `author_id`.

### 5.4. Sugestões, seguidores e feed

Foi criado `SocialPanel`, integrado à área autenticada, para:

- exibir até três recomendações combinadas de pesquisadores e grupos;
- seguir e deixar de seguir entidades;
- atualizar o estado visual após cada operação.

Foi criada a rota `frontend/app/posts/page.tsx`, com:

- feed geral de posts;
- ordenação recebida do algoritmo vetorial;
- opção de visualizar somente posts de entidades seguidas;
- acesso pela navegação da aplicação.

As telas de grupos, configurações e detalhes de posts foram adaptadas aos novos campos e contratos.

### 5.5. Editor de posts

O `PostEditor` passou a exigir a escolha entre:

- `ARTIGO`: exibe e envia palavras-chave fornecidas pelo autor;
- `OUTROS`: oculta a entrada manual e delega a geração das três palavras-chave ao Gemini.

Os tipos TypeScript foram atualizados para incluir identidade Scholar, tipo de post, palavras-chave e usuário publicador.

## 6. Infraestrutura e configuração

O `docker-compose.yml` foi atualizado para:

- utilizar PostgreSQL com pgvector;
- aguardar o healthcheck do banco antes de iniciar o backend;
- expor as integrações externas ao backend;
- expor o ID público do cliente Google ao frontend;
- configurar o endereço interno do backend para o proxy do Next.js.

Variáveis adicionadas:

```text
SERPAPI_KEY
CLOUDFLARE_ACCOUNT_ID
CLOUDFLARE_API_TOKEN
GEMINI_API_KEY
GOOGLE_CLIENT_ID
```

O `README.md` foi ampliado com instruções da Sprint 3, serviços utilizados, execução com Docker, comportamento da indexação e limitação do vínculo entre Google Identity e Google Scholar.

## 7. Tratamento de erros e validações

Foram contempladas mensagens específicas para:

- sessão ausente ou inválida;
- credenciais inválidas;
- tentativa de vincular e-mail Google diferente;
- conta Google já vinculada;
- usuário, grupo ou post inexistente;
- tentativa de seguir o próprio usuário;
- quantidade incorreta ou duplicidade de áreas;
- `author_id` duplicado;
- tipo de post inválido;
- artigo sem palavras-chave;
- usuário sem permissão de líder;
- chaves externas ausentes;
- falhas ou respostas inválidas de SerpApi, Cloudflare e Gemini;
- vetor com dimensão ou valores inválidos.

## 8. Testes adicionados e validações executadas

Foram criados testes para:

- serialização e validação de vetores;
- combinação de áreas Scholar e áreas manuais;
- três áreas manuais sem `author_id`;
- cadastro sem `author_id`;
- login com senha incorreta;
- segurança do vínculo Google por e-mail;
- vínculo do identificador Google;
- indexação retroativa;
- classificação e validação de posts;
- validação das três linhas de pesquisa de grupos;
- regras de seguir pesquisadores;
- recomendações sociais.

Comandos executados ao final:

```bash
docker compose exec -T backend mvn test
docker compose exec -T frontend npm run lint
```

Resultado registrado:

- backend: 18 testes executados, sem falhas ou erros;
- frontend: ESLint concluído sem erros;
- backend, banco e frontend iniciados com Docker;
- comunicação frontend → proxy Next.js → backend validada;
- variáveis de ambiente confirmadas dentro do contêiner sem imprimir seus valores;
- endpoint de cadastro validado sem `author_id`, retornando apenas a regra de exatamente três áreas quando foram enviadas duas.

## 9. Limitações conhecidas

- Google Identity Services não retorna o `author_id` do Google Scholar.
- Um perfil Scholar informado não possui prova criptográfica de titularidade pela credencial Google.
- O vínculo Scholar permanece opcional e baseado em informação pública fornecida pelo usuário.
- A disponibilidade de cadastro, indexação e publicação depende das APIs externas configuradas.
- A indexação retroativa pode consumir cota das APIs na primeira abertura das sugestões ou do feed.

## 10. Alerta de segurança

O arquivo `.env-example` foi encontrado com valores preenchidos que aparentam ser credenciais reais. Arquivos de exemplo devem conter apenas campos vazios ou valores fictícios.

Ações obrigatórias recomendadas:

1. revogar e recriar as chaves da SerpApi, Cloudflare e Gemini;
2. revisar ou recriar o cliente OAuth do Google, se necessário;
3. manter os novos valores exclusivamente no `.env`, que não deve ser versionado;
4. substituir os valores do `.env-example` por placeholders vazios antes de qualquer commit;
5. verificar o histórico Git e remover segredos caso esse arquivo já tenha sido enviado a um repositório remoto.

Nenhum valor de credencial é reproduzido neste relatório.

## 11. Arquivos principais criados

### Backend

- `controller/AuthController.java`
- `controller/SocialController.java`
- `dto/GoogleLoginRequest.java`
- `dto/LoginRequest.java`
- `dto/RegistrationRequest.java`
- `exception/ExternalServiceException.java`
- `integration/EmbeddingClient.java`
- `integration/GoogleIdentityClient.java`
- `integration/KeywordClient.java`
- `integration/ScholarClient.java`
- `repository/FollowRepository.java`
- `repository/VectorRepository.java`
- `service/AuthService.java`
- `service/IndexBackfillService.java`
- `service/InterestService.java`
- `service/SessionUser.java`
- `service/SocialService.java`
- `resources/schema.sql`

### Frontend

- `app/posts/page.tsx`
- `components/groups/SocialPanel.tsx`

### Testes

- `repository/VectorRepositoryTest.java`
- `service/AuthServiceTest.java`
- `service/IndexBackfillServiceTest.java`
- `service/InterestServiceTest.java`
- `service/PostServiceTest.java`
- `service/ResearchGroupServiceTest.java`
- `service/SocialServiceTest.java`

## 12. Arquivos existentes modificados

- `.env-example`
- `README.md`
- `docker-compose.yml`
- `backend/src/main/resources/application.properties`
- controllers de posts, grupos e usuários;
- DTOs de criação e atualização de posts;
- tratamento global de exceções;
- modelos `Post` e `User`;
- repositório de usuários;
- serviços de posts, grupos e usuários;
- página inicial do frontend;
- páginas de detalhes de posts e configurações de grupos;
- componentes de criação de grupo, autenticação, perfil e edição de posts;
- hooks de autenticação e grupos;
- cliente de API e tipos TypeScript.

## 13. Evolução posterior — descoberta Scholar e verificação de identidade

O login Google foi simplificado e reforçado após este relatório inicial:

- a credencial Google é validada antes da descoberta de perfis;
- nome e domínio institucional confiável (`hd`) são extraídos do ID token;
- a SerpApi pesquisa até cinco candidatos Scholar pelo nome;
- o usuário confirma um candidato e apenas revisa três áreas pré-preenchidas;
- quando nenhum candidato é encontrado, permanece disponível o preenchimento manual;
- o vínculo Scholar continua separado do nível de verificação;
- foram adicionados os níveis `NAO_VERIFICADO`, `INSTITUCIONAL` e `ORCID`;
- o nível institucional depende do claim `hd` assinado pelo Google, nunca apenas do sufixo do e-mail;
- quando existe Scholar, o domínio institucional precisa coincidir com o domínio verificado exibido no perfil;
- foi implementado vínculo ORCID por OAuth 2.0/OpenID Connect, com `state` de uso único contra CSRF;
- o ORCID autenticado é único no banco e não pode ser vinculado a duas contas;
- a tela de perfil mostra o nível de verificação e oferece o vínculo ORCID quando configurado;
- foram adicionadas colunas para ORCID, nível de verificação e domínio institucional;
- foram adicionadas variáveis de ambiente opcionais para ORCID e URL pública da aplicação.

Durante esta evolução, credenciais reais encontradas no `.env-example` foram removidas e substituídas por campos vazios. As credenciais expostas anteriormente ainda devem ser revogadas nos respectivos provedores.
