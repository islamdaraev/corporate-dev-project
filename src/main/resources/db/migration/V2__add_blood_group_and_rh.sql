-- Группа крови и резус: необязательные колонки. Старые строки получат NULL.
-- V1 не трогаем: любое изменение таблицы это новый файл.
ALTER TABLE blood_unit ADD COLUMN blood_group text;
ALTER TABLE blood_unit ADD COLUMN rh_factor text;

ALTER TABLE blood_unit ADD CONSTRAINT blood_unit_blood_group_known
    CHECK (blood_group IS NULL OR blood_group IN ('O', 'A', 'B', 'AB'));
ALTER TABLE blood_unit ADD CONSTRAINT blood_unit_rh_factor_known
    CHECK (rh_factor IS NULL OR rh_factor IN ('POSITIVE', 'NEGATIVE'));
