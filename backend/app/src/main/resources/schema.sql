CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Удаляем существующие таблицы
DROP TABLE IF EXISTS user_languages CASCADE;
DROP TABLE IF EXISTS user_interests CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS languages CASCADE;
DROP TABLE IF EXISTS interests CASCADE;

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    age INTEGER NOT NULL CHECK (age >= 18),
    location VARCHAR(200),
    bio TEXT,
    profile_picture_url VARCHAR(500),
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Таблица языков
CREATE TABLE IF NOT EXISTS languages (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    native_name VARCHAR(100)
);

-- Таблица интересов
CREATE TABLE IF NOT EXISTS interests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Связь пользователя с языками
CREATE TABLE IF NOT EXISTS user_languages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    language_id BIGINT NOT NULL REFERENCES languages(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, language_id)
);

-- Связь пользователя с интересами
CREATE TABLE IF NOT EXISTS user_interests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interest_id BIGINT NOT NULL REFERENCES interests(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, interest_id)
);

-- Начальные данные (языки)
INSERT INTO languages (code, name, native_name) VALUES
    ('en', 'English', 'English'),
    ('ru', 'Russian', 'Русский'),
    ('es', 'Spanish', 'Español'),
    ('fr', 'French', 'Français'),
    ('de', 'German', 'Deutsch'),
    ('it', 'Italian', 'Italiano'),
    ('pt', 'Portuguese', 'Português'),
    ('zh', 'Chinese', '中文'),
    ('ja', 'Japanese', '日本語'),
    ('ko', 'Korean', '한국어'),
    ('ar', 'Arabic', 'العربية'),
    ('hi', 'Hindi', 'हिन्दी'),
    ('tr', 'Turkish', 'Türkçe'),
    ('nl', 'Dutch', 'Nederlands'),
    ('pl', 'Polish', 'Polski'),
    ('uk', 'Ukrainian', 'Українська'),
    ('he', 'Hebrew', 'עברית'),
    ('sv', 'Swedish', 'Svenska'),
    ('da', 'Danish', 'Dansk'),
    ('fi', 'Finnish', 'Suomi'),
    ('no', 'Norwegian', 'Norsk'),
    ('cs', 'Czech', 'Čeština'),
    ('hu', 'Hungarian', 'Magyar'),
    ('el', 'Greek', 'Ελληνικά'),
    ('th', 'Thai', 'ไทย'),
    ('vi', 'Vietnamese', 'Tiếng Việt')
ON CONFLICT (code) DO NOTHING;

-- Начальные данные (интересы)
INSERT INTO interests (name) VALUES
    ('Music'), ('Cinema'), ('History'), ('Sport'), 
    ('Food'), ('Art'), ('Travel'), ('Literature'),
    ('Technology'), ('Photography'), ('Gaming'), ('Fashion'),
    ('Nature'), ('Science'), ('Dancing'), ('Theatre'),
    ('Architecture'), ('Philosophy'), ('Psychology'), ('Business')
ON CONFLICT (name) DO NOTHING;