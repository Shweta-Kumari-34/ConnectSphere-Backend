-- ================================================
-- ConnectSphere — Database Initialization Script
-- ================================================
-- Creates all required databases for microservices.
-- Auto-executed by MySQL Docker container on first start.
-- ================================================

CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS post_db;
CREATE DATABASE IF NOT EXISTS comment_db;
CREATE DATABASE IF NOT EXISTS like_db;
CREATE DATABASE IF NOT EXISTS follow_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS media_db;
CREATE DATABASE IF NOT EXISTS search_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS chat_db;
