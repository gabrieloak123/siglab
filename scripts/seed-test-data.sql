BEGIN;

TRUNCATE TABLE indexing_jobs, post_vectors, user_vectors, group_vectors, researcher_follows,
  group_follows, posts, kanban_tasks, kanban_columns, group_invites,
  user_research_group, research_group, users RESTART IDENTITY CASCADE;

INSERT INTO users (name, email, password, interest_areas, verification_level) VALUES
('Ana Inteligência', 'ana.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Inteligência Artificial','Aprendizado de Máquina','Visão Computacional'], 'NAO_VERIFICADO'),
('Bruno Robótica', 'bruno.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Robótica','Inteligência Artificial','Sistemas Autônomos'], 'NAO_VERIFICADO'),
('Carla Dados', 'carla.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Ciência de Dados','Aprendizado de Máquina','Mineração de Dados'], 'NAO_VERIFICADO'),
('Diego Visão', 'diego.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Visão Computacional','Processamento de Imagens','Inteligência Artificial'], 'NAO_VERIFICADO'),
('Elisa Saúde', 'elisa.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Informática em Saúde','Bioinformática','Diagnóstico Assistido'], 'NAO_VERIFICADO'),
('Felipe Bio', 'felipe.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Bioinformática','Genômica','Saúde Digital'], 'NAO_VERIFICADO'),
('Gabriela Médica', 'gabriela.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Imagens Médicas','Diagnóstico Assistido','Saúde Digital'], 'NAO_VERIFICADO'),
('Henrique Clínico', 'henrique.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Dados Clínicos','Informática em Saúde','Epidemiologia'], 'NAO_VERIFICADO'),
('Isabela Ambiente', 'isabela.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Mudanças Climáticas','Sensoriamento Remoto','Sustentabilidade'], 'NAO_VERIFICADO'),
('João Energia', 'joao.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Energia Renovável','Sustentabilidade','Otimização'], 'NAO_VERIFICADO'),
('Karen Clima', 'karen.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Climatologia','Modelagem Ambiental','Mudanças Climáticas'], 'NAO_VERIFICADO'),
('Lucas Oceano', 'lucas.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Oceanografia','Conservação','Sensoriamento Remoto'], 'NAO_VERIFICADO'),
('Marina Educação', 'marina.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Tecnologia Educacional','Aprendizagem','Educação a Distância'], 'NAO_VERIFICADO'),
('Nicolas Ensino', 'nicolas.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Metodologias Ativas','Formação Docente','Avaliação'], 'NAO_VERIFICADO'),
('Olívia Inclusão', 'olivia.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Educação Inclusiva','Acessibilidade','Tecnologia Assistiva'], 'NAO_VERIFICADO'),
('Paulo Jogos', 'paulo.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Jogos Educacionais','Gamificação','Aprendizagem'], 'NAO_VERIFICADO'),
('Renata Redes', 'renata.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Redes de Computadores','Segurança','Sistemas Distribuídos'], 'NAO_VERIFICADO'),
('Sérgio Segurança', 'sergio.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Cibersegurança','Criptografia','Privacidade'], 'NAO_VERIFICADO'),
('Talita Nuvem', 'talita.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Computação em Nuvem','Sistemas Distribuídos','DevOps'], 'NAO_VERIFICADO'),
('Vitor IoT', 'vitor.teste@siglab.local', '$2y$10$D7DB0PrPd.LlyFBkm.u1WO0MgnXyv6CNdDK2TWELResi4C4pTz/ka', ARRAY['Internet das Coisas','Redes de Sensores','Sistemas Embarcados'], 'NAO_VERIFICADO');

INSERT INTO research_group (group_id, name, research_fields) VALUES
('lab-ia', 'Laboratório de Inteligência Artificial', ARRAY['Inteligência Artificial','Aprendizado de Máquina','Visão Computacional']),
('lab-saude', 'Laboratório de Saúde Digital', ARRAY['Informática em Saúde','Bioinformática','Diagnóstico Assistido']),
('lab-ambiente', 'Laboratório de Sustentabilidade', ARRAY['Mudanças Climáticas','Sensoriamento Remoto','Sustentabilidade']),
('lab-educacao', 'Laboratório de Educação e Tecnologia', ARRAY['Tecnologia Educacional','Aprendizagem','Educação Inclusiva']),
('lab-redes', 'Laboratório de Redes e Segurança', ARRAY['Redes de Computadores','Cibersegurança','Sistemas Distribuídos']);

INSERT INTO user_research_group (user_email, group_id, is_leader) VALUES
('ana.teste@siglab.local','lab-ia',true), ('bruno.teste@siglab.local','lab-ia',false),
('elisa.teste@siglab.local','lab-saude',true), ('felipe.teste@siglab.local','lab-saude',false),
('isabela.teste@siglab.local','lab-ambiente',true), ('joao.teste@siglab.local','lab-ambiente',false),
('marina.teste@siglab.local','lab-educacao',true), ('nicolas.teste@siglab.local','lab-educacao',false),
('renata.teste@siglab.local','lab-redes',true), ('sergio.teste@siglab.local','lab-redes',false);

INSERT INTO posts (group_id, publisher_user_id, title, description, content, tags, post_type, keywords, created_at) VALUES
('lab-ia',1,'Detecção de objetos em tempo real','Novo método para detectar objetos em imagens com baixa latência.','Resumo experimental do artigo de visão computacional.',ARRAY['IA','visão'],'ARTIGO',ARRAY['visão computacional','detecção de objetos','aprendizado profundo'],now()-interval '1 day'),
('lab-ia',2,'Robôs móveis em ambientes dinâmicos','Planejamento seguro de trajetórias para robôs autônomos.','Resultados de navegação e controle.',ARRAY['robótica'],'ARTIGO',ARRAY['robótica','planejamento','sistemas autônomos'],now()-interval '2 days'),
('lab-saude',5,'IA no diagnóstico por imagem','Classificação de exames médicos com apoio computacional.','Estudo aplicado a imagens médicas.',ARRAY['saúde','IA'],'ARTIGO',ARRAY['imagens médicas','diagnóstico assistido','saúde digital'],now()-interval '3 days'),
('lab-saude',6,'Genômica para medicina personalizada','Análise de variantes genéticas para apoio clínico.','Pesquisa em bioinformática e genômica.',ARRAY['bioinformática'],'ARTIGO',ARRAY['genômica','bioinformática','medicina personalizada'],now()-interval '4 days'),
('lab-ambiente',9,'Monitoramento climático por satélite','Uso de sensoriamento remoto para acompanhar mudanças climáticas.','Resumo de dados ambientais orbitais.',ARRAY['clima'],'ARTIGO',ARRAY['mudanças climáticas','sensoriamento remoto','sustentabilidade'],now()-interval '5 days'),
('lab-ambiente',10,'Otimização de energia solar','Modelo para melhorar a geração de energia renovável.','Análise de eficiência energética.',ARRAY['energia'],'ARTIGO',ARRAY['energia renovável','otimização','sustentabilidade'],now()-interval '6 days'),
('lab-educacao',13,'Plataformas adaptativas de aprendizagem','Ambientes educacionais adaptados ao perfil do estudante.','Tecnologia aplicada ao ensino.',ARRAY['educação'],'ARTIGO',ARRAY['tecnologia educacional','aprendizagem','personalização'],now()-interval '7 days'),
('lab-educacao',14,'Metodologias ativas no ensino superior','Avaliação de aprendizagem baseada em projetos.','Relato de prática docente.',ARRAY['ensino'],'ARTIGO',ARRAY['metodologias ativas','avaliação','formação docente'],now()-interval '8 days'),
('lab-redes',17,'Detecção de intrusão em redes','Análise distribuída de tráfego para detectar ataques.','Experimentos de segurança em redes.',ARRAY['segurança'],'ARTIGO',ARRAY['cibersegurança','redes de computadores','detecção de intrusão'],now()-interval '9 days'),
('lab-redes',18,'Privacidade em sistemas distribuídos','Protocolo criptográfico para serviços em nuvem.','Resumo sobre privacidade e criptografia.',ARRAY['privacidade'],'ARTIGO',ARRAY['criptografia','privacidade','sistemas distribuídos'],now()-interval '10 days');

-- Os embeddings não são fabricados pelo seed. Na primeira consulta às sugestões
-- o processamento em segundo plano indexa estes registros com o mesmo modelo
-- Cloudflare usado para usuários, grupos e posts criados normalmente.

INSERT INTO researcher_follows(follower_id, followed_id) VALUES
(1,2),(1,5),(5,6),(9,10),(13,14),(17,18),(2,3),(6,7),(10,11),(14,15);
INSERT INTO group_follows(follower_id, group_id) VALUES
(1,'lab-ia'),(1,'lab-saude'),(5,'lab-saude'),(9,'lab-ambiente'),
(13,'lab-educacao'),(17,'lab-redes');

COMMIT;
