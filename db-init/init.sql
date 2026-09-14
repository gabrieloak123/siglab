    -- 1. Tabela de Usuários (users)
    CREATE TABLE IF NOT EXISTS users (
        id BIGSERIAL PRIMARY KEY,
        email VARCHAR(255) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        name VARCHAR(255) NOT NULL,
        interest_areas TEXT[],
        groups BIGINT[]
    );

    -- 2. Tabela de Grupos de Pesquisa (research_group)
    CREATE TABLE IF NOT EXISTS research_group (
        group_id VARCHAR(255) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        research_fields TEXT[]
    );

    -- 3. Tabela de Junção (user_research_group)
    CREATE TABLE IF NOT EXISTS user_research_group (
        user_email VARCHAR(255) NOT NULL,
        group_id VARCHAR(255) NOT NULL,
        is_leader BOOLEAN NOT NULL,
        PRIMARY KEY (user_email, group_id),
        CONSTRAINT fk_user_research_group_user
            FOREIGN KEY (user_email) REFERENCES users (email) ON DELETE CASCADE ON UPDATE CASCADE,
        CONSTRAINT fk_user_research_group_group
            FOREIGN KEY (group_id) REFERENCES research_group (group_id) ON DELETE CASCADE ON UPDATE CASCADE
    );

