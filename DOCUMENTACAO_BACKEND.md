# Documentação conceitual do backend do SIGLab

Este documento descreve o backend existente em `backend/`, suas responsabilidades e os principais fluxos entre componentes. Ele retrata o código atual: não é uma especificação de funcionalidades futuras nem uma garantia de segurança para produção.

## 1. Visão geral

O backend oferece uma API HTTP para contas de pesquisadores, grupos de pesquisa, quadro Kanban, publicações, relacionamentos de acompanhamento e recomendações. Foi construído com Java 25, Spring Boot 4.1.1, Maven, PostgreSQL e a extensão pgvector. O frontend consome a API e mantém a sessão por cookie.

```text
Cliente HTTP
    ↓
Controllers → validação de entrada e identificação da sessão
    ↓
Services → regras de negócio e transações
    ├── Repositories → PostgreSQL e pgvector
    └── Interfaces de integração → provedores externos
                                  Google, ORCID, SerpApi, Cloudflare e Gemini

Dados alterados → fila persistida → processamento agendado → vetores
```

Essa separação é uma organização predominante, não absoluta: por exemplo, alguns controllers consultam diretamente repositórios, e `InterestService` ainda possui métodos de indexação direta. Entidades JPA também são retornadas por várias rotas, não apenas DTOs.

### Componentes de infraestrutura

| Arquivo | Papel |
| --- | --- |
| `pom.xml` | Declara dependências, versão Java, Lombok, plugins e execução dos testes. |
| `mvnw`, `mvnw.cmd` | Maven Wrapper para executar o Maven quando seus arquivos de suporte estiverem disponíveis. |
| `SiglabApplication` | Ponto de entrada Spring Boot; habilita os agendamentos com `@EnableScheduling`. |
| `application.properties` | Configura nome da aplicação e inicialização SQL, entre outras propriedades. |
| `schema.sql` | Complementa o esquema gerenciado pelo Hibernate com pgvector, índices, relacionamentos sociais, vetores e fila. |

O `docker-compose.yml`, na raiz, fornece backend, frontend e PostgreSQL com pgvector. No Compose, o Hibernate está configurado para `update` e o SQL de inicialização roda depois da inicialização JPA. Isso ajuda no desenvolvimento, mas não substitui um processo versionado de migrações.

## 2. Configuração (`config`)

- `SecurityConfig`: registra o codificador BCrypt e a cadeia de filtros do Spring Security. No estado atual, desabilita CSRF, permite todas as requisições na configuração global e habilita HTTP Basic e formulário. A autenticação da aplicação é feita separadamente por sessão e verificações explícitas nos controllers/services; não se deve interpretar `permitAll` como proteção automática das rotas. Há credenciais padrão de desenvolvimento em `application.properties`, inadequadas para produção.
- `ExternalClientConfig`: cria um `RestClient` compartilhado com limites configuráveis de conexão e leitura. É usado pelos clientes HTTP externos que o recebem por injeção; a validação JWT do Google usa seu próprio mecanismo.
- `OpenApiConfig`: configura os metadados da documentação OpenAPI. O Springdoc expõe as rotas de documentação, incluindo `/v3/api-docs` e a interface Swagger.

## 3. Modelo de domínio (`model`)

Uma entidade JPA representa dados persistidos e seu mapeamento para tabelas. Nem toda classe nesta pasta é uma entidade: algumas são estruturas de resposta ou chaves compostas.

| Classe | Conceito e dados principais |
| --- | --- |
| `User` | Pesquisador: id, nome, e-mail, senha codificada, áreas de interesse, identificadores Google/Scholar/ORCID, nível de verificação e domínio institucional. A senha é protegida contra serialização de saída por anotação Jackson. |
| `ResearchGroup` | Grupo identificado por `group_id`, com nome, linhas de pesquisa e relações de membros. |
| `User_ResearchGroup` | Associação entre usuário e grupo; registra se o membro é líder. Não é a mesma coisa que seguir um grupo. |
| `User_ResearchGroupId` | Chave composta da associação: e-mail do usuário e identificador do grupo. A escolha do e-mail como parte da chave afeta alterações de e-mail e integridade referencial. |
| `GroupInvite` | Convite com token, grupo, criador, datas de criação e expiração e indicador de uso. |
| `Post` | Publicação vinculada a grupo e publicador, com título, descrição, conteúdo, tags, tipo, palavras-chave, caminho de arquivo e data. |
| `Coluna` | Coluna persistida do Kanban, vinculada a um grupo e com uma ordem. A lista de tarefas exibida é montada pelo serviço. |
| `Tarefa` | Tarefa de um grupo, com criador, possível responsável, coluna, título, descrição e tags. |
| `Estado` | Representação do quadro de um grupo e suas colunas; é montada para resposta, não é entidade JPA persistida. |

`NAO_VERIFICADO`, `INSTITUCIONAL` e `ORCID` são valores usados pelo código para indicar diferentes evidências de identidade. Um `authorId` do Scholar declarado ou pesquisado não prova, por si só, que a conta pertence ao titular do perfil.

## 4. Contratos HTTP (`dto`)

DTO significa *Data Transfer Object*: um formato para entrada ou saída que pode conter apenas os dados necessários a uma operação. `@Valid`, `@NotBlank`, `@Email` e `@Size` expressam parte das validações; regras que dependem do banco ou de múltiplos campos ficam nos serviços.

| Classe | Uso |
| --- | --- |
| `RegistrationRequest`, `LoginRequest` | Dados para cadastro e autenticação por e-mail/senha. |
| `GoogleLoginRequest`, `GooglePreviewResponse` | Credencial e dados complementares do login Google; resposta da prévia com dados da identidade e candidatos Scholar. |
| `ScholarCandidate` | Dados resumidos de perfil Scholar candidato, incluindo identificador, nome e informações públicas. |
| `CreateGroupRequest`, `UpdateGroupRequest` | Nome e três linhas de pesquisa; criação também recebe `group_id`. |
| `GroupMemberDTO` | Representação de membro, incluindo informações básicas e indicador de liderança. |
| `InviteResponseDTO`, `JoinGroupRequestDTO` | Detalhes do convite e entrada para adesão com token. |
| `CreatePostRequestDTO`, `UpdatePostRequestDTO` | Dados de publicação, com validações de tamanho e conteúdo obrigatório. |
| `CreateTarefaRequestDTO`, `UpdateTarefaRequestDTO`, `MoveTarefaRequestDTO` | Criação, edição e movimentação de tarefas do Kanban. O criador é obtido da sessão, não da entrada de criação. |
| `UpdateUserRequest` | Dados editáveis do perfil, incluindo senha opcional e áreas. |
| `ErrorResponseDTO` | Estrutura de erro com data, status, categoria, mensagem, caminho e, quando aplicável, erros de validação por campo. |
| `ResearchGroupDTO`, `UserDTO` | Representações adicionais existentes no código; sua existência não significa que todas as rotas as utilizem. Por exemplo, a criação em `/api/users` ainda recebe `User`. |

## 5. Entrada HTTP (`controller`)

Controllers traduzem requisições HTTP em chamadas de serviços e definem rotas, status e formato das respostas. Alguns exigem sessão ou verificam permissões explicitamente; consulte cada implementação antes de presumir que uma rota é privada.

### `AuthController` — `/api/auth`

| Método e caminho | Responsabilidade |
| --- | --- |
| `POST /register` | Cadastra e estabelece sessão. |
| `POST /login` | Autentica por e-mail e senha e estabelece sessão. |
| `POST /google/preview` | Valida a credencial Google e apresenta dados e possíveis candidatos Scholar. |
| `POST /google` | Entra ou cria conta Google; no cadastro novo são necessárias áreas válidas. |
| `POST /google/link` | Vincula Google a uma conta autenticada, sujeito à conferência de e-mail. |
| `POST /logout`, `GET /me` | Encerra a sessão ou retorna o usuário autenticado. |
| `GET /scholar/{authorId}` | Consulta interesses públicos do perfil Scholar. |
| `GET /orcid/status`, `/orcid/start`, `/orcid/callback` | Verifica configuração e inicia/conclui o vínculo OAuth ORCID. |

No login, a sessão anterior é invalidada e outra recebe `userId`. No ORCID, `state` é criado e conferido na sessão, para vincular o retorno à tentativa iniciada. Se o navegador perder o cookie ou mudar a origem de forma incompatível, o callback pode falhar. O ORCID é um vínculo de identidade, não um preenchimento automático de interesses.

### `UserController` — `/api/users`

Disponibiliza listagem e consulta por id, criação, alteração e exclusão. As alterações e exclusões verificam a identidade da sessão. É importante distinguir essas verificações de autenticação da configuração global permissiva de `SecurityConfig`.

### `ResearchGroupController` — `/api/research-groups`

Lista, consulta, cria, altera e exclui grupos; lista membros; emite convites; recebe adesões; altera liderança e remove membros. As rotas de administração verificam liderança, conforme a operação. Os endpoints específicos incluem `/{id}/users`, `/{groupId}/invites`, `/join` e `/{groupId}/members/{userEmail}`.

### `KanbanController` — `/api/research-groups/{groupId}`

`GET /board` retorna o quadro; `POST /tasks`, `PUT /tasks/{taskId}`, `PATCH /tasks/{taskId}/column` e `DELETE /tasks/{taskId}` gerenciam tarefas. O controller exige sessão e pertencimento ao grupo para acessar essas operações; a criação usa o id da sessão.

### `PostController` — `/api/research-groups/{groupId}/posts`

Lista e consulta publicações do grupo para usuário autenticado; criação, alteração e exclusão são verificadas pelo serviço quanto à liderança. Um post pertence ao grupo indicado na URL.

### `SocialController` — `/api/social`

Fornece sugestões em `/suggestions/researchers` e `/suggestions/groups`, feed em `/feed`, seguidos em `/following`, e operações de seguir/deixar de seguir em `/researchers/{id}/follow` e `/groups/{id}/follow`. `/recent-posts` retorna até cinco publicações recentes e é público. Nas demais operações sociais, a identidade vem da sessão.

## 6. Regras de negócio (`service`)

| Classe | Responsabilidade conceitual |
| --- | --- |
| `AuthService` | Cadastro, login por senha, validação/entrada Google, prévia Scholar e vínculo de conta Google. Coordena provedores por interfaces e evita manter transações de domínio abertas durante chamadas externas. |
| `SessionUser` | Recupera e valida o usuário da sessão HTTP para os fluxos que a exigem. |
| `UserService` | Persistência e alteração de perfil, tratamento de senha, verificação de duplicidades, validação de áreas, exclusão e enfileiramento de indexação. Impede exclusão de usuário quando deixaria grupo sem líder. |
| `ResearchGroupService` | Cadastro e alteração de grupos, vínculos, liderança, convites e remoções. Usa bloqueio pessimista do grupo nas operações de liderança para ajudar a proteger a regra de ao menos um líder. |
| `InterestService` | Consulta interesses de um Scholar, combina/valida áreas e contém métodos auxiliares de geração de vetores. A regra usual exige exatamente três áreas distintas, não vazias e com tamanho limitado. |
| `OrcidService` | Troca o código recebido com o provedor por uma identidade e vincula o ORCID ao usuário, verificando conflitos. |
| `EstadoService` | Monta o estado do quadro a partir das colunas e tarefas; cria as colunas iniciais quando necessário. |
| `TarefaService` | Cria, edita, move e remove tarefas, verificando vínculo com grupo, coluna e participantes conforme o caso. |
| `PostService` | Verifica acesso e liderança, valida tipo e palavras-chave, salva publicações e solicita indexação. `ARTIGO` exige palavras-chave informadas; `OUTROS` tem palavras-chave geradas posteriormente. |
| `SocialService` | Gerencia seguimentos, consulta recomendações e feed, e retorna publicações recentes. |
| `IndexingQueue` | Mantém trabalhos persistidos, descobre vetores ausentes, executa indexação em segundo plano e controla tentativas, versões e concessão temporária do trabalho. |

Transações `@Transactional` agrupam alterações relacionadas no banco. Elas não devem ser confundidas com atomicidade entre o banco e APIs externas: o sistema usa fila persistida para separar a gravação principal da indexação externa.

## 7. Persistência (`repository`)

Repositórios encapsulam acesso aos dados. Os repositórios de entidades usam Spring Data JPA; `FollowRepository` e `VectorRepository` usam consultas JDBC/SQL diretamente.

| Classe | Dados ou consultas |
| --- | --- |
| `UserRepository` | Usuários; consultas por e-mail, identificadores externos e existência. |
| `ResearchGroupRepository` | Grupos, incluindo consulta com bloqueio para alterações sensíveis de liderança. |
| `User_ResearchGroupRepository` | Membros e líderes de grupos; consultas por chave, grupo e usuário. |
| `GroupInviteRepository` | Convites persistidos por token. |
| `PostRepository` | Publicações por grupo, id e data, inclusive as recentes. |
| `ColunaRepository`, `TarefaRepository` | Colunas e tarefas do Kanban. |
| `FollowRepository` | Inserção, remoção e listagem dos relacionamentos de seguir pesquisadores e grupos. |
| `VectorRepository` | Escrita de vetores de 1024 dimensões, validação dos valores e consultas de similaridade no pgvector. |

### Tabelas complementares em `schema.sql`

- `researcher_follows` e `group_follows`: relacionamentos de acompanhamento, com chaves compostas; o banco impede seguir a si próprio.
- `user_vectors`, `group_vectors`, `post_vectors`: um vetor por entidade, com chave estrangeira e exclusão em cascata.
- `indexing_jobs`: chave por tipo e id da entidade, versão, estado, tentativas, disponibilidade e prazo de concessão ao processador.
- Índices únicos adicionais protegem `author_id`, `google_subject` e `orcid` quando preenchidos.

## 8. Integrações externas (`integration`)

As interfaces descrevem capacidades exigidas pelo domínio; os clientes implementam os detalhes do fornecedor. Isso permite trocar ou simular provedores nos testes sem alterar os serviços consumidores.

| Interface | Implementação | Função |
| --- | --- | --- |
| `EmbeddingProvider` | `EmbeddingClient` | Usa Cloudflare Workers AI, modelo configurado por padrão como `@cf/baai/bge-m3`, para produzir vetores de 1024 dimensões. |
| `KeywordExtractor` | `KeywordClient` | Usa Gemini para solicitar três palavras-chave para posts sem palavras-chave fornecidas. |
| `ResearchProfileProvider` | `ScholarClient` | Usa SerpApi para procurar perfis Scholar e consultar dados públicos de um autor. |
| `GoogleIdentityProvider` | `GoogleIdentityClient` | Valida a credencial Google e extrai identidade; é distinto de buscar interesses no Scholar. |
| `OrcidIdentityProvider` | `OrcidClient` | Monta a autorização OAuth e troca o código de autorização por identificação ORCID. |

As chamadas podem falhar por indisponibilidade, credenciais, limites de cota ou resposta inesperada. A fila implementa novas tentativas para indexação; isso não significa que todas as integrações síncronas tenham retry ou circuit breaker. O vínculo ORCID valida o titular pela autorização, mas não associa automaticamente o perfil Scholar nem preenche áreas de pesquisa.

## 9. Fluxos principais

### Cadastro, autenticação e vínculo de identidade

1. O controller recebe os dados e executa as validações declarativas aplicáveis.
2. `AuthService` confere credenciais ou consulta o provedor Google. Se houver `authorId`, a consulta Scholar pode contribuir com as áreas; sem ele, o usuário informa as áreas.
3. `UserService` valida os dados, armazena a senha com BCrypt e registra a necessidade de indexação na mesma transação da gravação.
4. O controller estabelece uma sessão com `userId`; chamadas posteriores que a exigem consultam essa sessão.
5. O usuário pode iniciar separadamente o OAuth ORCID. Na volta, `state` e sessão são conferidos, e `OrcidService` vincula a identidade retornada.

### Publicação e indexação

1. Um líder cria ou altera um post. O serviço valida os campos e as regras do tipo.
2. O post e um trabalho de indexação são gravados na transação de domínio. O vetor antigo é removido para não recomendar usando informação desatualizada.
3. O processador captura o trabalho; quando necessário, solicita palavras-chave ao Gemini e um vetor à Cloudflare fora da transação de domínio.
4. Antes de salvar, verifica a versão do trabalho. Se o conteúdo tiver sido alterado durante a chamada, o resultado antigo não substitui o mais novo.
5. O vetor fica disponível para consultas semânticas. Assim, uma publicação pode existir antes de aparecer em recomendações.

### Fila e recuperação

Os estados usados são `PENDING`, `PROCESSING` e `ERROR`. A captura usa bloqueio SQL `SKIP LOCKED` e uma concessão de cinco minutos; falhas agendam até cinco tentativas com espera progressiva. Um processo periódico descobre registros elegíveis sem vetor, inclusive dados antigos. Trabalhos em `ERROR` não são automaticamente recriados pela descoberta; após corrigir o problema, o procedimento de reenfileiramento está no `README.md`.

### Recomendações

O texto de áreas ou de palavras-chave e resumo/conteúdo é transformado em vetor. O PostgreSQL compara vetores pela distância cosseno (`<=>`): distância menor indica maior proximidade nesse espaço. Usuário, grupo e post devem estar indexados para participar das comparações.

As sugestões de pessoas e grupos excluem o próprio usuário, entidades já seguidas e grupos dos quais ele já é membro. A consulta combina os dois tipos e limita o conjunto total a três antes de separar os resultados por tipo; portanto, não há garantia de três resultados de cada categoria. O feed é ordenado por distância e, em seguida, por data; com `followedOnly=true`, considera publicações de autores ou grupos seguidos e aplica `RECOMMENDATION_MAX_DISTANCE`. Sem vetor do usuário ou das publicações, o resultado pode ficar vazio.

## 10. Exceções e respostas

`GlobalExceptionHandler` centraliza o tratamento HTTP:

| Situação | Resposta típica |
| --- | --- |
| `AuthenticationRequiredException` | 401: sessão ou credenciais necessárias/inválidas. |
| `SecurityException` | 403: operação não autorizada. |
| `ResourceNotFoundException` | 404: entidade não encontrada. |
| `ConflictException` ou violação de integridade | 409: conflito de dados; detalhes internos da integridade não são enviados ao cliente. |
| Entrada inválida, corpo malformado ou validação | 400, com erros por campo quando disponíveis. |
| `ExternalServiceException` | 503: falha de serviço externo. |
| Exceção inesperada | 500 com referência para correlação; detalhes ficam no log do servidor. |

O status depende da exceção efetivamente lançada: por exemplo, algumas regras usam `IllegalStateException` e são tratadas como 400. Erros que ocorrem no processamento assíncrono aparecem na fila/logs, não na resposta original que gravou a entidade.

## 11. Configuração operacional

O arquivo `.env-example` e o `docker-compose.yml` mostram a configuração de desenvolvimento. Nunca inclua valores reais de segredos na documentação ou em commits.

| Variáveis | Finalidade |
| --- | --- |
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_URL` | Banco PostgreSQL e conexão do backend pelo Compose. |
| `SERPAPI_KEY` | Busca e consulta de perfis Scholar. |
| `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN` | Geração de embeddings. |
| `GEMINI_API_KEY` | Geração de palavras-chave para posts `OUTROS`. |
| `GOOGLE_CLIENT_ID` | Audiência esperada para credenciais Google. |
| `ORCID_CLIENT_ID`, `ORCID_CLIENT_SECRET`, `ORCID_REDIRECT_URI`, `ORCID_BASE_URL` | Cliente, segredo, endereço de retorno e ambiente ORCID. A URL de retorno deve corresponder à cadastrada no provedor. |
| `APP_BASE_URL` | Endereço do frontend usado no redirecionamento após o ORCID. |
| `RECOMMENDATION_MAX_DISTANCE` | Limite de distância do feed de seguidos; padrão `0.65`. |
| `EXTERNAL_CONNECT_TIMEOUT_MS`, `EXTERNAL_READ_TIMEOUT_MS` | Limites do `RestClient` compartilhado; padrões 5000 e 20000 ms. |

O código também aceita `SERPAPI_SEARCH_URL`, `GEMINI_MODEL_URL`, `CLOUDFLARE_EMBEDDING_URL` e os intervalos `INDEX_DISCOVERY_INITIAL_DELAY_MS`, `INDEX_DISCOVERY_DELAY_MS`, `INDEX_WORKER_INITIAL_DELAY_MS` e `INDEX_WORKER_DELAY_MS`. Essas opções não estão todas repassadas pelo Compose: para usá-las em contêiner, é preciso disponibilizá-las no ambiente do serviço, além de colocá-las no arquivo de variáveis. Alterar a URL/modelo de embeddings sem reindexar todos os dados pode misturar espaços semânticos incompatíveis.

## 12. Testes e execução

Com os serviços configurados e o banco disponível:

```bash
docker compose up -d --build
docker compose exec -T backend mvn test
```

Os testes em `src/test/java` cobrem inicialização do contexto, autenticação, interesses, posts, grupos, rede social, consultas vetoriais, autorização do Kanban e tratamento de erros. Os testes de contexto usam a configuração de banco disponível no ambiente; não se deve pressupor isolamento de uma base de produção. Testes com mocks não comprovam conectividade, credenciais ou comportamento real das APIs externas.

## 13. Limites e cuidados atuais

- A autorização depende de verificações distribuídas pelo código; `permitAll` global e CSRF desabilitado exigem revisão antes de produção. HTTP Basic/formulário e as credenciais de desenvolvimento também devem ser revistos.
- Alguns endpoints retornam entidades e a criação de usuário ainda recebe entidade diretamente. DTOs explícitos e revisão de quais campos são públicos reduziriam acoplamento e exposição acidental.
- A fila fornece consistência eventual, não disponibilidade imediata das recomendações; falhas permanentes exigem observação e recuperação operacional.
- A busca Scholar e o vínculo ORCID têm propósitos diferentes: o primeiro consulta dados públicos, o segundo comprova controle da conta autorizada. Nenhum deles garante automaticamente que todos os perfis externos representem a mesma pessoa.
- Os testes não cobrem todos os cenários concorrentes, integrações reais ou políticas de segurança; a documentação não deve ser tratada como substituto de auditoria ou testes de ponta a ponta.
