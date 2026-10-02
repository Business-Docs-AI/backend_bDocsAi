-- Usuário ADMIN inicial para ambiente de desenvolvimento (senha: admin123).
-- Só existe nesta pasta (locations do perfil dev) para nunca ser aplicada em prod.
INSERT INTO user_entity (name, email, permission_id, created_at, update_at, active, password, role)
VALUES ('Administrador', 'admin@businessdocs.ai', NULL, now(), now(), true,
        '$2a$10$Z01mjKAZ9l7AJkGUGM4OSuPQkkzmVFVzSbUs9qg108mp4fiHvQfdG', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
