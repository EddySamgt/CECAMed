ALTER TABLE patients ALTER COLUMN identification_number DROP NOT NULL;
UPDATE patients SET identification_number = NULL WHERE TRIM(identification_number) = '';
