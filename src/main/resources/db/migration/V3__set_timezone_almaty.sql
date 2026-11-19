-- Время по Казахстану (UTC+5) для всех, кто откроет базу: psql, DBeaver и само приложение.
DO $$
BEGIN
EXECUTE format('ALTER DATABASE %I SET timezone TO %L', current_database(), 'Asia/Almaty');
END
$$;
