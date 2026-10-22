# Relatório de implementação — notificação de seguidores na publicação

Branch: `feat/email-trigger`

## 1. Objetivo

Notificar por e-mail os seguidores de um grupo de pesquisa e de um pesquisador sempre que um post é publicado, sem sobrecarregar a requisição HTTP que cria a publicação.

O endpoint manual `POST /api/emails`, introduzido em `feat/send-email`, continua disponível e inalterado; esta implementação apenas automatiza o disparo do e-mail.

## 2. Resumo funcional

- e-mail em texto puro para cada seguidor elegível, disparado na **criação** de um post (edições e exclusões não notificam);
- destinatários: quem segue o **grupo** do post e quem segue o **pesquisador** que publicou;
- sem duplicatas (quem segue o grupo e o pesquisador recebe um único e-mail) e nunca para o autor da publicação;
- quem segue o pesquisador **com áreas de interesse** só recebe quando o post corresponde a alguma delas; quem segue sem escolher áreas recebe todas as publicações;
- envio **assíncrono e após o commit**: a resposta HTTP não depende do SMTP e uma falha de envio não afeta a publicação;
- falha de um destinatário é registrada em log e não interrompe os demais envios.

## 3. Fluxo

1. `PostService.criar` grava o post e o trabalho de indexação na transação de domínio e publica o evento `PostPublishedEvent` com o retrato da publicação.
2. `PostPublishedListener` recebe o evento em `AFTER_COMMIT`: transações revertidas não notificam ninguém.
3. A escuta é `@Async`, portanto roda em outra thread, com o executor do Spring Boot (`spring.task.execution.*`).
4. `FollowerNotificationService` consulta os seguidores do grupo (`group_follows`) e do pesquisador (`researcher_follows`), monta a lista de destinatários e envia um e-mail por destinatário via `EmailService`.

O evento carrega o conteúdo do post, de modo que a notificação não depende de releitura do banco nem da transação que o originou.

## 4. Regra de correspondência por área de interesse

Áreas de interesse são escolhidas pelo usuário ao seguir um pesquisador e validadas contra as áreas declaradas por ele.

Para o seguidor que escolheu áreas, o post é considerado correspondente quando **todas** as palavras de uma das áreas aparecem como **palavra inteira** no título, na descrição, no conteúdo, nas palavras-chave ou nas etiquetas do post, ignorando maiúsculas e acentos.

Exemplos:

| Área escolhida | Post | Notifica |
|---|---|---|
| `Deep Learning` | "Deep learning aplicado a imagens" | sim |
| `Deep Learning` | "Redes convolucionais e transformers" | não |
| `Aprendizado de Máquina` | "Modelos de machine learning" | não (palavras diferentes) |
| — (seguir sem áreas) | qualquer post | sim |

O filtro é textual e independente do pipeline de indexação vetorial, para que a notificação não dependa do processamento assíncrono de embeddings. Como no feed, a correspondência é aproximada: sinônimos e abreviações não são reconhecidos.

## 5. Conteúdo do e-mail

```
Assunto: Novo post em {grupo}: {título}

Olá, {nome}!

O grupo {grupo} publicou um novo post: "{titulo}".

Publicado por {autor}.

{excerto da descrição ou do conteúdo}

Acesse: {APP_BASE_URL}/groups/{groupId}/posts/{postId}

Equipe SIGLab
```

O endereço do link usa `APP_BASE_URL` (mesma variável do redirecionamento do ORCID), com padrão `http://localhost:3000`.
## 6. Arquivos

Backend:

- `service/PostPublishedEvent.java` — evento de domínio com o retrato do post publicado;
- `service/PostPublishedListener.java` — escuta `AFTER_COMMIT` e execução assíncrona;
- `service/FollowerNotificationService.java` — destinatários, filtro de áreas e envio;
- `service/PostService.java` — publica o evento ao criar o post;
- `repository/FollowRepository.java` — listagem de seguidores com nome, e-mail e áreas de interesse;
- `SiglabApplication.java` — `@EnableAsync`;
- `application.properties` — dimensionamento do executor assíncrono.

Testes:

- `service/FollowerNotificationServiceTest.java` — união sem duplicatas, exclusão do autor, filtro de áreas, isolamento de falhas e conteúdo da mensagem;
- `service/PostPublishedListenerTest.java` — delegação e contenção de falhas;
- `service/PostPublishedListenerIntegrationTest.java` — verificação de que a notificação ocorre somente após o commit e em outra thread;
- `service/PostServiceTest.java` — publicação do evento ao criar o post.

Nenhuma alteração de schema é necessária: as notificações leem `group_follows` e `researcher_follows`.

## 7. Testes e verificação

```bash
./mvnw test -Dtest='PostServiceTest,FollowerNotificationServiceTest,PostPublishedListenerTest,PostPublishedListenerIntegrationTest,EmailServiceTest'
```

Todos os testes unitários do backend passam. Os testes que carregam o contexto completo (`SiglabApplicationTests`, `DiscoveryIntegrationTest`) exigem o PostgreSQL do Compose e não executam sem banco disponível.

## 8. Limitações e decisões

- **Sem retentativa**: uma falha de SMTP é apenas registrada em log. Uma fila persistente, como a de indexação, daria retentativa e sobreviveria a reinicializações.
- **Processamento em memória**: notificações pendentes são perdidas se a aplicação reiniciar entre o commit e o envio.
- **Sem descadastro**: todo seguidor recebe as notificações; uma preferência por usuário exigiria nova coluna e migração.
- **Cota do Gmail**: contas gratuitas limitam o volume diário de envios, o que restringe grupos com muitos seguidores. Uma versão futura pode agrupar destinatários ou usar BCC.
- **Sem notificação de edição**: apenas a criação do post notifica, para não transformar cada edição em spam.
- O filtro por área de interesse é textual e não vetorial, para não depender do pipeline de indexação; o feed segue a regra vetorial original.
