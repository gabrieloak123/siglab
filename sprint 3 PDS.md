Analise todo o código do repositório atual e faça as seguintes implementações. 

O código gerado deve seguir a arquitetura em camadas do Spring Boot e deve ser cauteloso quanto ao Tratamento de Exceções, Validação das regras de negócio e todo serviço externo que for utilizado deve ser desacoplado da camada de serviço por meio da criação de uma classe própria.
## Algoritmo de Sugestão inteligente 

Adiciona funcionalidade de seguir um pesquisador ou grupo.

Sugere pesquisadores e grupos para um usuário com base nas linhas de pesquisa e áreas de interesse. 

Caso existam posts feitos por um pesquisador ou um grupo que o usuário siga e que os posts tenham relação com as áreas de interesse do usuário, sugere aquele post para o usuário. 

Na área do usuário, após o login, aparecem pesquisadores/grupos cujo linhas de pesquisa/áreas de interesse se relacionam com o usuário. 

## Lógica por trás do algoritmo

Utiliza um banco de dados vetorial que armazena o id de um usuário ou de um grupo de pesquisa e seus embeddings, calculados pelo campo de area de interesse no caso do usuário e pelo campo de linhas de pesquisa no caso do grupo de pesquisa, e gerador de embeddings por meio do endpoint @cf/baai/bge-m3 da empresa CloudFlare.

Cada post que é criado primeiro é passado pelo endpoint de LLM do Google Gemini 3.5 Flash-Lite, que lê todo o post e escolhe três palavras chaves que definem o post, salvando ambos juntos numa mesma entrada do banco. Essas três palavras chaves são então passadas pelo modelo de embeddings para o posterior cálculo da similaridade.

Quando um usuário acessa a área de posts gerais da rede, os posts são ordenados primeiro por similaridade de embeddings e segundo por tempo. 

Quando um usuário acessa a área do usuário, aparecem os três usuários/grupos com maior similaridade que ele ainda não segue.

## Como funciona a questão das áreas de interesse

Um usuário/grupo sendo cadastrado precisa necessariamente preencher 3 áreas de interesse principais. 

Essas áreas de interesse são preenchidas:

- Ou inserindo o author id do Google Scholar manualmente, caso o login esteja sendo feito SEM UTILIZAR o Google Scholar
- Ou automaticamente caso o usuário faça login direto com o Google Scholar
- As áreas de interesses são puxadas do perfil do Google Scholar por meio da busca por author id na API: https://serpapi.com/google-scholar-author-api.
	- Caso no Google Scholar não tenham no mínimo 3 áreas de interesse, preenche somente as que tiver e pede para o usuário preencher as outras obrigatoriamente.

O campo "author_id" é OBRIGATÓRIO para o Login de um usuário.

## Telas

### Posts

Um post é criado por meio do botão "criar post" na área do usuário ou na área de posts. Um post obrigatoriamente ou é um Artigo, ou é sobre Outros. No caso do Artigo, o Post precisa obrigatoriamente conter um campo com as palavras chaves do artigo. No caso de ser artigo, o campo palavras chave vai ser imediatamente utilizado para geração de embeddings da busca inteligente. No caso de ser outros, vai ser utilizada a estratégia de palavras chave geradas por LLM dita anteriormente. 


## Login

A tela de login vai ter duas opções:

- Login via Google Scholar
- Login 

Devem seguir portanto as instruções citadas antes referente ao uso do Google Scholar para as lógicas de cálculo de embeddings de um usuário. 

Sempre analise se as suas implementações estão seguindo as instruções iniciais e estão dentro das boas práticas de código. Gere testes unitários para as classes/métodos que forem sendo implementados.