# Relatório de implementações — e-mail e conferências

## 1. Escopo

Este documento resume as implementações presentes na branch `feat/send-email` (ainda não commitadas):

1. **Envio de e-mail** pelo servidor SMTP do SIGLab.
2. **Conferências sugeridas** na página inicial, baseadas nas áreas de interesse do usuário.
3. **Busca de conferências** por período, área de pesquisa e país (opcional).

As conferências vêm da [Conference Partner API](https://www.myhuiban.com/developers), com atribuição obrigatória em todos os cards.

## 2. Resumo funcional

- envio de e-mail em texto puro, restrito a usuários autenticados;
- credenciais de SMTP e da Conference Partner carregadas do `.env`;
- recomendações de conferências futuras por similaridade vetorial com o perfil;
- cache local das conferências com atualização no máximo uma vez por dia por área;
- formulário de busca com período e área obrigatórios e país opcional;
- filtro por país aplicado no backend sobre a localização do evento;
- cache curto das buscas para preservar a cota diária da API;
- testes unitários cobrindo cliente HTTP, países, serviço e e-mail.

## 3. Envio de e-mail

### 3.1. Endpoint

| Método | Endpoint | Autenticação | Resposta |
|---|---|---|---|
| `POST` | `/api/emails` | obrigatória (sessão) | `204 No Content` |

Corpo da requisição (`SendEmailRequestDTO`):

```json
{
  "to": "destinatario@exemplo.com",
  "subject": "Assunto",
  "body": "Corpo em texto puro"
}
```

### 3.2. Regras

- `to` é obrigatório e validado como e-mail (`@Email`);
- `subject` e `body` são obrigatórios (`@NotBlank`);
- o corpo é enviado como **texto puro** (`helper.setText(body, false)`);
- o endpoint exige sessão ativa (`SessionUser.require`), evitando uso como relay de spam;
- falhas de SMTP viram `ExternalServiceException` e são respondidas como **503**.

### 3.3. Configuração

- dependência `spring-boot-starter-mail` adicionada ao `pom.xml`;
- SMTP do Gmail em `application.properties`: `smtp.gmail.com:587`, `auth=true`, `starttls=true`, timeouts de 5 s;
- credenciais `SMTP_USERNAME` e `SMTP_PASSWORD` vêm do ambiente (`.env` / `docker-compose.yml`).

### 3.4. Arquivos

- `backend/src/main/java/br/ufrn/dimap/siglab/controller/EmailController.java`
- `backend/src/main/java/br/ufrn/dimap/siglab/dto/SendEmailRequestDTO.java`
- `backend/src/main/java/br/ufrn/dimap/siglab/service/EmailService.java`
- `backend/src/test/java/br/ufrn/dimap/siglab/service/EmailServiceTest.java`

## 4. Conferências sugeridas

### 4.1. Endpoint

| Método | Endpoint | Autenticação |
|---|---|---|
| `GET` | `/api/social/suggestions/conferences` | obrigatória (sessão) |

Retorna até **6** conferências futuras, ordenadas por similaridade de conteúdo com o perfil.

### 4.2. Fluxo

1. As três áreas de interesse do usuário são mapeadas para um dos **21 slugs** de `field` da API (`ai`, `vision`, `nlp`, `data`, `network`, `security`, `se`, `systems`, `theory`, `hci`, `graphics`, `bio`, `robotics`, `web`, `ee`, `energy`, `materials`, `civil`, `education`, `management`, `math`).
2. Áreas sem correspondência caem no parâmetro `query` da API (chave interna `q:<área>`).
3. Os resultados são buscados **sempre com `conference_date >= hoje`**, nunca recomendando eventos passados.
4. Cada conferência recebe um embedding `bge-m3` (1024 dimensões) e entra no cache local.
5. A ordenação usa o mesmo operador de cosseno (`<=>`) das demais sugestões do SIGLab; se o usuário ainda não tiver vetor de perfil, a ordem é pela data do evento.
6. O cache por área é atualizado a cada **24 h**, no máximo uma requisição à API por área por dia.

### 4.3. Integração com a API

- `ConferenceClient` consulta `GET /api/conferences`;
- usa `CONFERENCE_API_KEY` (cota de 200/dia); sem chave, funciona no modo anônimo (50/dia por IP);
- envia `User-Agent` descritivo, pois o Cloudflare rejeita agentes padrão de scripts (HTTP 403);
- trata `401` (chave inválida), `402/429` (cota esgotada) e `403` (Cloudflare) como `ExternalServiceException` (503).

### 4.4. Embeddings em lote

`EmbeddingClient` ganhou `embedBatch(List<String>)`, que envia até **16 textos por requisição** e preserva a ordem dos vetores retornados. `embed(String)` continua existindo e delega para o lote.

### 4.5. Banco de dados

Novas tabelas em `schema.sql`:

| Tabela | Função |
|---|---|
| `conferences` | cache das conferências (`id`, `field`, nome, datas, local, ranking CCF, CP-I, `detail_page`) |
| `conference_vectors` | embedding `vector(1024)` de cada conferência, com cascata em `conferences` |
| `conference_field_refresh` | marcador de última atualização por área/chave de consulta |

Índice `idx_conferences_field_date` em `(field, conference_date)`.

### 4.6. Frontend

- `ConferencePanel`: seção "Conferências sugeridas" na área autenticada da página inicial;
- `ConferenceCard`: card compartilhado com sigla, nome, datas, local, selos CCF/CP-I e link **"Ver na Conference Partner"** (`detail_page`);
- atribuição "Conferências fornecidas por Conference Partner" ao final da seção.

## 5. Busca de conferências

### 5.1. Endpoints

| Método | Endpoint | Parâmetros | Autenticação |
|---|---|---|---|
| `GET` | `/api/social/conferences/search` | `start`, `end`, `field`, `country` (opcional) | obrigatória |
| `GET` | `/api/social/conferences/countries` | — | obrigatória |

Exemplo:

```
GET /api/social/conferences/search?start=2027-01-01&end=2027-12-30&field=se&country=Brazil
```

### 5.2. Validação (HTTP 400)

- `start` e `end` obrigatórios, no formato `AAAA-MM-DD`;
- `end >= start`;
- intervalo máximo de **5 anos**;
- `field` deve ser um dos 21 slugs válidos;
- `country`, quando informado, deve constar na lista de países.

### 5.3. Filtro por país

A Conference Partner **não possui parâmetro de país** e não retorna o país como campo separado. O filtro é aplicado no backend sobre o campo `location`:

- considera apenas o **último segmento** separado por vírgula (`"Krakow, Poland"` → `Poland`);
- compara sem acentos e sem diferenciar maiúsculas;
- aceita sinônimos reais da API: `USA`/`United States`, `UK`/`United Kingdom`, `Korea`/`South Korea`, `the Netherlands`, `Srilanka`, `UAE`, `Czechia`, etc.;
- usa correspondência por **palavra inteira**, evitando falsos positivos como `UK` em `Ukraine` ou `Oman` em `Romania`.

A lista oferecida no formulário é curada (~75 países) e inclui países fora do top-20 de `/api/statistics`, como Brasil, Polônia, Holanda e Suíça. Os nomes aparecem em inglês porque é o idioma dos dados de localização.

### 5.4. Paginação e cache

- até **5 páginas** de 100 resultados (500 eventos varridos por busca);
- remoção de duplicatas por `id`;
- ordenação pela data do evento (mais próximo primeiro);
- retorno de no máximo **60** conferências;
- cache em memória de **30 min** por combinação `(field, start, end, country)`, limitado a 200 entradas, para proteger a cota de 200 requisições/dia.

### 5.5. Frontend

`ConferenceSearch`, renderizado logo abaixo de "Conferências sugeridas":

- **Data inicial** e **data final** obrigatórias, preenchidas por padrão com hoje e daqui a um ano;
- **Área de pesquisa** obrigatória, com os 21 rótulos em português;
- **País** opcional ("Todos os países");
- estados de carregamento, erro, lista vazia e contagem de resultados;
- resultados nos mesmos cards e com a mesma atribuição das sugestões.

### 5.6. Arquivos

Backend:

- `integration/ConferenceClient.java` — busca paginada com `conference_date_end` e `page`;
- `service/ConferenceCountries.java` — lista de países e regra de correspondência;
- `service/ConferenceService.java` — validação, filtro, ordenação, limite e cache;
- `controller/SocialController.java` — endpoints de busca e de países.

Frontend:

- `components/groups/ConferenceSearch.tsx`
- `components/groups/ConferencePanel.tsx` (refatorado para usar o card compartilhado)
- `components/groups/ConferenceCard.tsx`
- `lib/api.ts` — `searchConferences` e `conferenceCountries`
- `app/page.tsx` — renderiza `<ConferenceSearch />` após `<ConferencePanel />`

## 6. Configuração

Variáveis adicionadas a `.env-example` e `docker-compose.yml`:

```env
SMTP_USERNAME=
SMTP_PASSWORD=
CONFERENCE_API_KEY=
```

## 7. Testes e verificação

- **Backend:** 48 testes aprovados, incluindo:
  - `ConferenceClientTest` (6) — envelope, campos opcionais e paginação;
  - `ConferenceCountriesTest` (5) — Brasil, Holanda, Coreia do Sul, EUA, Reino Unido, `Online` e falso positivo `UK`/`Ukraine`;
  - `ConferenceServiceTest` (16) — mapeamento de áreas, cache de sugestões, validação da busca, filtro de país, ordenação, paginação, deduplicação e cache.
- **Frontend:** `npm run lint` sem erros e `npm run build` concluído.
- **Dados reais:** a regra de país foi validada sobre 171 conferências de IA de 2027 (Poland 2, Netherlands 2, South Korea 3, USA 6, UK 2, Brazil 1).

Comando usado para os testes do backend:

```bash
./mvnw test -Dtest='!SiglabApplicationTests'
```

## 8. Como aplicar as mudanças

O serviço `backend` do `docker-compose.yml` usa a imagem `maven:3.9-eclipse-temurin-25` com o código montado por bind e `mvn spring-boot:run`. Por isso, `--build` não reconstrói nada nem recria um container que já está rodando. Após alterar o backend:

```bash
sudo docker compose restart backend
# se necessário:
sudo docker compose up -d --force-recreate backend
```

Verificação de que as rotas novas subiram (sem sessão deve responder **403**, e não 404):

```bash
curl -i http://localhost:8080/api/social/conferences/countries
```

O frontend roda em modo `next dev` com o diretório montado, então as alterações são recarregadas automaticamente.

## 9. Limitações e decisões

- a busca depende da cota diária da Conference Partner (200/dia com chave); o cache de 30 min reduz o consumo;
- como a API não filtra por país, a busca varre até 500 eventos antes de aplicar o país — filtros muito amplos podem não encontrar correspondências além desse limite;
- buscas limitadas a 5 anos e a 60 resultados exibidos;
- recomendações e buscas exigem login, consistente com as demais seções sociais da página inicial;
- os nomes dos países são exibidos em inglês para coincidir com os dados de `location`.
