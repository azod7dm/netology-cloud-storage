-- Удаляем старые таблицы
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS files;

-- Таблица пользователей
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,  -- ← BIGSERIAL
    login VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL
);

-- Таблица файлов
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,  -- ← BIGSERIAL
    filename VARCHAR(255) NOT NULL,
    size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    owner_login VARCHAR(255) NOT NULL,
    FOREIGN KEY (owner_login) REFERENCES users(login)
);