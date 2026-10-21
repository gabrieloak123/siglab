-- =====================================================================
-- DADOS MOCKADOS
-- Ordem de insercao respeita as FKs:
--   users -> research_group -> user_research_group
--   -> kanban_columns -> kanban_tasks -> posts
--
-- Senha de TODOS os usuarios: senha123
-- (hash bcrypt $2b$10$..., compativel com BCryptPasswordEncoder do Spring)
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- (Opcional) Limpar dados existentes antes de rodar o seed.
-- Descomente se quiser recriar tudo do zero.
-- ---------------------------------------------------------------------
-- TRUNCATE posts, kanban_tasks, kanban_columns,
--          user_research_group, research_group, users
--          RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- 1. users
-- Obs: users.groups e BIGINT[], mas research_group.group_id e VARCHAR,
-- entao nao ha como guardar os ids dos grupos ali. Fica vazio; o vinculo
-- real esta em user_research_group.
-- ---------------------------------------------------------------------
INSERT INTO users (id, email, password, name, interest_areas, groups) VALUES
(1,  'ana.souza@example.com',       '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Ana Beatriz Souza',     ARRAY['Aprendizado de Máquina','Imagens Médicas','Ética em IA'],            '{}'),
(2,  'carlos.lima@example.com',     '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Carlos Eduardo Lima',   ARRAY['Engenharia de Software','DevOps','Aprendizado de Máquina'],          '{}'),
(3,  'mariana.ferreira@example.com','$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Mariana Ferreira',      ARRAY['Bioinformática','Processamento de Linguagem Natural'],               '{}'),
(4,  'rafael.oliveira@example.com', '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Rafael Oliveira',      ARRAY['Visão Computacional','Qualidade de Software'],                       '{}'),
(5,  'juliana.martins@example.com', '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Juliana Martins',      ARRAY['Genômica','Bioinformática','Estatística'],                           '{}'),
(6,  'lucas.pereira@example.com',   '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Lucas Pereira',        ARRAY['Mineração de Repositórios','Informática na Educação'],               '{}'),
(7,  'fernanda.costa@example.com',  '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Fernanda Costa',       ARRAY['Redes Biológicas','Gamificação','Aprendizagem Adaptativa'],          '{}'),
(8,  'pedro.alves@example.com',     '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Pedro Henrique Alves', ARRAY['Genômica','Informática na Educação'],                               '{}'),
(9,  'camila.rodrigues@example.com','$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Camila Rodrigues',     ARRAY['Gamificação','Aprendizagem Adaptativa','Design Instrucional'],       '{}'),
(10, 'gabriel.santos@example.com',  '$2b$10$abcdefghijklmnopqrstuu4GiwOG/hJykbcxOdaNQqlq2A2t0sW0m', 'Gabriel Santos',       ARRAY['DevOps','Gamificação','Desenvolvimento Web'],                        '{}');

-- Ajusta a sequence do BIGSERIAL, ja que os ids foram inseridos manualmente
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));

-- ---------------------------------------------------------------------
-- 2. research_group
-- ---------------------------------------------------------------------
INSERT INTO research_group (group_id, name, research_fields) VALUES
('ia-saude',       'Inteligência Artificial Aplicada à Saúde',      ARRAY['Aprendizado de Máquina','Imagens Médicas','Processamento de Linguagem Natural']),
('eng-software',   'Engenharia de Software Experimental',           ARRAY['Qualidade de Software','DevOps','Mineração de Repositórios']),
('bioinfo',        'Bioinformática e Genômica Computacional',       ARRAY['Genômica','Bioinformática','Redes Biológicas']),
('edu-digital',    'Tecnologias Digitais para Educação',            ARRAY['Informática na Educação','Gamificação','Aprendizagem Adaptativa']);

-- ---------------------------------------------------------------------
-- 3. user_research_group  (1 lider por grupo)
-- ---------------------------------------------------------------------
INSERT INTO user_research_group (user_email, group_id, is_leader) VALUES
-- ia-saude
('ana.souza@example.com',        'ia-saude',     TRUE),
('carlos.lima@example.com',      'ia-saude',     FALSE),
('mariana.ferreira@example.com', 'ia-saude',     FALSE),
('rafael.oliveira@example.com',  'ia-saude',     FALSE),
-- eng-software
('carlos.lima@example.com',      'eng-software', TRUE),
('rafael.oliveira@example.com',  'eng-software', FALSE),
('lucas.pereira@example.com',    'eng-software', FALSE),
('gabriel.santos@example.com',   'eng-software', FALSE),
-- bioinfo
('juliana.martins@example.com',  'bioinfo',      TRUE),
('mariana.ferreira@example.com', 'bioinfo',      FALSE),
('fernanda.costa@example.com',   'bioinfo',      FALSE),
('pedro.alves@example.com',      'bioinfo',      FALSE),
-- edu-digital
('camila.rodrigues@example.com', 'edu-digital',  TRUE),
('fernanda.costa@example.com',   'edu-digital',  FALSE),
('gabriel.santos@example.com',   'edu-digital',  FALSE),
('lucas.pereira@example.com',    'edu-digital',  FALSE),
('pedro.alves@example.com',      'edu-digital',  FALSE);

-- ---------------------------------------------------------------------
-- 4. kanban_columns  (4 colunas por grupo)
-- ids: ia-saude 1-4 | eng-software 5-8 | bioinfo 9-12 | edu-digital 13-16
-- ---------------------------------------------------------------------
INSERT INTO kanban_columns (id, name, column_order, research_group_id) VALUES
(1,  'A Fazer',       1, 'ia-saude'),
(2,  'Em Andamento',  2, 'ia-saude'),
(3,  'Em Revisão',    3, 'ia-saude'),
(4,  'Concluído',     4, 'ia-saude'),

(5,  'A Fazer',       1, 'eng-software'),
(6,  'Em Andamento',  2, 'eng-software'),
(7,  'Em Revisão',    3, 'eng-software'),
(8,  'Concluído',     4, 'eng-software'),

(9,  'A Fazer',       1, 'bioinfo'),
(10, 'Em Andamento',  2, 'bioinfo'),
(11, 'Em Revisão',    3, 'bioinfo'),
(12, 'Concluído',     4, 'bioinfo'),

(13, 'A Fazer',       1, 'edu-digital'),
(14, 'Em Andamento',  2, 'edu-digital'),
(15, 'Em Revisão',    3, 'edu-digital'),
(16, 'Concluído',     4, 'edu-digital');

-- Ajusta a sequence da coluna IDENTITY
SELECT setval(pg_get_serial_sequence('kanban_columns', 'id'), (SELECT MAX(id) FROM kanban_columns));

-- ---------------------------------------------------------------------
-- 5. kanban_tasks
-- A FK composta (column_id, group_id) exige que a coluna pertenca ao
-- mesmo grupo da tarefa. Uma tarefa sem responsavel (assigned_to_id NULL)
-- foi incluida de proposito para testar esse caso.
-- ---------------------------------------------------------------------
INSERT INTO kanban_tasks (group_id, created_by_id, assigned_to_id, title, description, tags, column_id) VALUES
-- ia-saude (usuarios 1, 2, 3, 4)
('ia-saude', 1, 3, 'Revisão sistemática sobre segmentação de tomografias',
    'Levantar artigos dos últimos 5 anos sobre segmentação de imagens de tomografia computadorizada e organizar em planilha.',
    ARRAY['revisão','literatura'], 2),
('ia-saude', 1, 4, 'Pré-processar dataset de radiografias de tórax',
    'Normalizar resolução, remover duplicatas e dividir em treino, validação e teste.',
    ARRAY['dados','pré-processamento'], 1),
('ia-saude', 2, 2, 'Treinar baseline U-Net',
    'Treinar o modelo de referência e registrar métricas (Dice e IoU) para comparação futura.',
    ARRAY['modelo','experimento'], 3),
('ia-saude', 1, NULL, 'Redigir seção de métodos do artigo',
    'Descrever datasets, arquitetura e protocolo experimental. Ainda sem responsável.',
    ARRAY['escrita'], 1),
('ia-saude', 1, 1, 'Submeter protocolo ao comitê de ética',
    'Enviar documentação completa e acompanhar o parecer.',
    ARRAY['ética','burocracia'], 4),

-- eng-software (usuarios 2, 4, 6, 10)
('eng-software', 2, 4, 'Definir métricas de qualidade de código',
    'Escolher conjunto de métricas (complexidade ciclomática, acoplamento, code smells) para o estudo.',
    ARRAY['métricas','planejamento'], 5),
('eng-software', 2, 6, 'Coletar repositórios do GitHub para análise',
    'Selecionar repositórios por estrelas, linguagem e atividade recente.',
    ARRAY['dados','github'], 6),
('eng-software', 6, 10, 'Configurar pipeline de CI para os experimentos',
    'Automatizar execução dos scripts de análise a cada push na branch principal.',
    ARRAY['ci','devops'], 7),
('eng-software', 2, 6, 'Escrever script de mineração de commits',
    'Extrair histórico de commits e autores usando PyDriller.',
    ARRAY['script','mineração'], 8),

-- bioinfo (usuarios 5, 3, 7, 8)
('bioinfo', 7, 3, 'Documentar ambiente conda do grupo',
    'Padronizar o environment.yml para reprodutibilidade das análises.',
    ARRAY['documentação','infra'], 9),
('bioinfo', 5, 8, 'Alinhar sequências do lote 03',
    'Rodar o alinhamento contra o genoma de referência e checar taxa de mapeamento.',
    ARRAY['alinhamento','sequenciamento'], 10),
('bioinfo', 5, 7, 'Validar pipeline de anotação de variantes',
    'Comparar resultados do pipeline com um conjunto de variantes já curadas.',
    ARRAY['validação','variantes'], 11),
('bioinfo', 5, 7, 'Gerar figuras de redes de interação proteica',
    'Produzir figuras em alta resolução para o manuscrito.',
    ARRAY['figuras','redes'], 12),

-- edu-digital (usuarios 9, 7, 6, 8, 10)
('edu-digital', 7, 8, 'Transcrever entrevistas com professores',
    'Transcrever e anonimizar as 12 entrevistas realizadas em setembro.',
    ARRAY['entrevistas','análise qualitativa'], 13),
('edu-digital', 9, 10, 'Prototipar mecânica de gamificação',
    'Criar protótipo navegável com pontos, medalhas e ranking.',
    ARRAY['protótipo','gamificação'], 14),
('edu-digital', 9, 7, 'Aplicar questionário piloto com alunos',
    'Aplicar em uma turma e coletar feedback sobre clareza das questões.',
    ARRAY['pesquisa','piloto'], 15),
('edu-digital', 9, 6, 'Preparar apresentação para o simpósio de educação',
    'Montar slides e ensaiar a apresentação de 15 minutos.',
    ARRAY['evento','apresentação'], 16);

-- ---------------------------------------------------------------------
-- 6. posts
-- ---------------------------------------------------------------------
INSERT INTO posts (group_id, publisher_user_id, title, description, content, tags, file_path, created_at) VALUES
('ia-saude', 2,
    'Resultados preliminares do baseline U-Net',
    'Primeiros números do modelo de referência na segmentação de radiografias.',
    E'# Resultados preliminares\n\nTreinamos o baseline U-Net por 50 épocas.\n\n- Dice médio: 0.81\n- IoU médio: 0.69\n\nPróximos passos: testar data augmentation e comparar com Attention U-Net.',
    ARRAY['resultados','segmentação'],
    '/uploads/ia-saude/resultados-unet-v1.pdf',
    '2026-09-12 14:30:00'),

('ia-saude', 1,
    'Chamada de bolsas de iniciação científica 2027',
    'Abertas as inscrições para bolsistas no grupo.',
    E'# Bolsas de IC 2027\n\nO grupo tem **2 vagas** para iniciação científica.\n\nInteressados devem enviar histórico e carta de motivação à liderança até o fim do mês.',
    ARRAY['aviso','bolsas'],
    NULL,
    '2026-10-02 09:15:00'),

('eng-software', 2,
    'Slides da reunião de planejamento do semestre',
    'Apresentação com metas, prazos e divisão de tarefas.',
    E'# Planejamento 2026.2\n\nAs metas do semestre estão nos slides em anexo. Revisem a divisão de tarefas no quadro Kanban até sexta.',
    ARRAY['planejamento','reunião'],
    '/uploads/eng-software/planejamento-2026-2.pptx',
    '2026-08-20 10:00:00'),

('eng-software', 6,
    'Dataset de repositórios coletado',
    'CSV com os 500 repositórios selecionados para o estudo.',
    E'# Dataset coletado\n\nColunas: nome, linguagem, estrelas, número de contribuidores e data do último commit.\n\nCritério de inclusão: ao menos 100 commits nos últimos 12 meses.',
    ARRAY['dados','github'],
    '/uploads/eng-software/repos-dataset.csv',
    '2026-09-25 16:45:00'),

('bioinfo', 5,
    'Guia rápido do pipeline de anotação',
    'Passo a passo para rodar a anotação de variantes no servidor do grupo.',
    E'# Guia rápido\n\n1. Ative o ambiente conda do grupo\n2. Coloque os arquivos VCF em `data/input`\n3. Execute `make annotate`\n4. Confira os resultados em `data/output`\n\nEm caso de erro, avisar no canal do grupo.',
    ARRAY['tutorial','pipeline'],
    NULL,
    '2026-09-05 11:20:00'),

('bioinfo', 8,
    'Relatório de qualidade do lote 03',
    'Resultado do controle de qualidade das sequências do último lote.',
    E'# QC do lote 03\n\nTaxa média de mapeamento: **96,4%**. Duas amostras abaixo do limiar foram marcadas para ressequenciamento.',
    ARRAY['qc','sequenciamento'],
    '/uploads/bioinfo/qc-lote03.html',
    '2026-10-10 15:05:00'),

('edu-digital', 9,
    'Protocolo do questionário piloto',
    'Roteiro de aplicação e termo de consentimento do piloto com alunos.',
    E'# Protocolo do piloto\n\nO questionário tem 20 questões e leva cerca de 15 minutos. Aplicar em sala, com autorização prévia da coordenação.',
    ARRAY['pesquisa','protocolo'],
    '/uploads/edu-digital/protocolo-piloto.pdf',
    '2026-09-18 13:40:00'),

('edu-digital', 10,
    'Resumo aceito no simpósio de educação',
    'Nosso trabalho sobre gamificação foi aceito para apresentação oral.',
    E'# Resumo aceito\n\nA apresentação oral será em novembro. Precisamos ensaiar na semana anterior.',
    ARRAY['evento','publicação'],
    NULL,
    '2026-10-15 08:30:00');

COMMIT;

-- ---------------------------------------------------------------------
-- Conferencia rapida (opcional)
-- ---------------------------------------------------------------------
-- SELECT g.name, COUNT(*) AS membros FROM user_research_group urg JOIN research_group g USING (group_id) GROUP BY g.name;
-- SELECT c.name AS coluna, COUNT(t.id) AS tarefas FROM kanban_columns c LEFT JOIN kanban_tasks t ON t.column_id = c.id GROUP BY c.id, c.name ORDER BY c.id;
