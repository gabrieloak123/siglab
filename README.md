# SIGLab

## TD;DR

To run
```bash
docker compose up --build
```

## Sprint 3: rede e recomendações

Copie `.env-example` para `.env` e configure `SERPAPI_KEY`, `CLOUDFLARE_ACCOUNT_ID`,
`CLOUDFLARE_API_TOKEN`, `GEMINI_API_KEY` e `GOOGLE_CLIENT_ID`. O cadastro exige
três áreas distintas e grava vetores de 1024 dimensões do
[`@cf/baai/bge-m3`](https://developers.cloudflare.com/workers-ai/models/bge-m3/)
no PostgreSQL com pgvector. Posts do tipo Outros recebem três palavras-chave
do [`gemini-3.5-flash-lite`](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite);
artigos usam as palavras-chave informadas pelo autor. O feed em `/posts` ordena
por similaridade e depois por data; o painel inicial recomenda os três perfis
ou grupos mais semelhantes ainda não seguidos. A indexação ocorre em uma fila
persistida em segundo plano; até seu processamento, novos dados ou alterações
podem não aparecer nas recomendações.

O botão de entrada com Google usa [Google Identity](https://developers.google.com/identity/gsi/web/guides/verify-google-id-token).
No primeiro acesso, nome e e-mail são preenchidos pela identidade Google e o usuário
deve confirmar exatamente três áreas de interesse. O fluxo Google não pesquisa
perfis Scholar nem tenta deduzir `author_id` pelo e-mail.
A API pública do Scholar não oferece autenticação do titular
nem fornece `author_id` na credencial Google; portanto, o vínculo informado não
prova a propriedade do perfil Scholar. Configure no Google Cloud um ID de cliente
OAuth para a origem do frontend (por exemplo `http://localhost:3000`).
Quem já possui uma conta com senha pode vinculá-la ao Google em Gerenciar Perfil;
o e-mail da credencial Google precisa coincidir com o da conta.

O `author_id` só pode ser preenchido em Gerenciar Perfil depois da autenticação
ORCID. Essa autenticação autoriza o preenchimento, mas não comprova que o perfil
Scholar indicado pertence à mesma pessoa. O selo `INSTITUCIONAL` usa exclusivamente
o claim confiável `hd` do Google Workspace; o domínio textual do e-mail não é
usado como prova. Para habilitar o selo ORCID, registre um cliente OAuth público e
configure `ORCID_CLIENT_ID`, `ORCID_CLIENT_SECRET` e a URL de retorno indicada no
`.env-example`. O vínculo usa OAuth com `state` anti-CSRF e não armazena a senha ORCID.

Se já existir volume PostgreSQL antigo, o `schema.sql` do backend aplica as
novas tabelas e colunas na inicialização, mas a imagem do serviço de banco deve
ser atualizada para `pgvector/pgvector:pg16` para disponibilizar a extensão.
Usuários antigos devem completar três áreas no perfil para receber
recomendações.

### Descoberta de interesses por publicações

Na janela de login ou cadastro, **Descobrir minhas áreas de interesse** abre
uma lista paginada de publicações. Escolha até 30 posts indexados: suas
palavras-chave sugerem até três áreas editáveis e, após a autenticação, a média
normalizada dos vetores dos posts é salva separadamente do vetor das áreas
declaradas. A busca combina os dois perfis com o mesmo peso quando ambos existem.
As sugestões por palavras-chave são uma heurística, não uma conversão do vetor
em texto; se faltarem áreas, complete-as manualmente. A seleção fica na aba do
navegador até a aplicação à conta, e pode ser refeita. Posts ainda sem vetor não
podem ser selecionados; aguarde a indexação. Se a aplicação falhar, uma opção
de nova tentativa aparece após o login.

To reset
```bash
docker compose down -v
```

### Dados de demonstração

Os dados de demonstração (20 usuários, 5 grupos e 10 artigos de teste) são
carregados automaticamente na criação do banco: a pasta `db-init` é montada em
`/docker-entrypoint-initdb.d`, e o `db-init/mock1.sql` roda depois do
`db-init/init.sql`. Para recriá-los em um banco que já está em uso:

```bash
docker compose exec -T db psql -v ON_ERROR_STOP=1 -U siglab -d siglab < db-init/mock1.sql
```

Todos os usuários de demonstração usam a senha `teste123`. O script também cria
seguimentos, mas não fabrica vetores. O processo periódico identifica registros
sem vetor e gera os embeddings pela Cloudflare com o mesmo modelo dos cadastros.
A primeira indexação consome cota da API, mas mantém todos os registros no mesmo
espaço semântico. Artigos são indexados pelas palavras-chave junto do resumo (ou
conteúdo, quando não há resumo). O feed de entidades seguidas usa o limite de
distância configurável `RECOMMENDATION_MAX_DISTANCE` (padrão `0.65`).

Falhas na indexação são repetidas com espera progressiva até cinco tentativas e
registradas nos logs do backend. Depois de corrigir as credenciais ou a causa da
falha, trabalhos em estado `ERROR` podem ser reenfileirados com:

```bash
docker compose exec -T db psql -U siglab -d siglab -c "UPDATE indexing_jobs SET status='PENDING', attempts=0, available_at=now(), lease_until=NULL WHERE status='ERROR';"
```

Os limites de conexão e leitura dos clientes HTTP externos podem ser configurados
em milissegundos com `EXTERNAL_CONNECT_TIMEOUT_MS` e `EXTERNAL_READ_TIMEOUT_MS`
(padrões: 5000 e 20000). O serviço precisa ser reiniciado após alterar o `.env`.

## Front

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.

## Back

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.1/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.1/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.1/reference/web/servlet.html)
* [Validation](https://docs.spring.io/spring-boot/4.1.1/reference/io/validation.html)
* [Spring Security](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html)
* [OAuth2 Resource Server](https://docs.spring.io/spring-boot/4.1.1/reference/web/spring-security.html#web.security.oauth2.server)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.1.1/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.1.1/reference/using/devtools.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

