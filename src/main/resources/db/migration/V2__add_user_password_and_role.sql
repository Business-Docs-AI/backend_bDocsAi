-- Adiciona autenticação real (senha) e o perfil de acesso (role) ao usuário.
ALTER TABLE user_entity ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE user_entity ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USUARIO';
