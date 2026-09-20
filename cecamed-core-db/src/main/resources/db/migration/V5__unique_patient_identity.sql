-- Keep historical identification values, but use name and birth date for uniqueness.
CREATE FUNCTION normalize_patient_name(value TEXT) RETURNS TEXT
LANGUAGE SQL IMMUTABLE STRICT PARALLEL SAFE AS $$
    SELECT trim(regexp_replace(lower(regexp_replace(normalize(value, NFD),
        U&'[\0300-\036f]', '', 'g')), '[[:space:]]+', ' ', 'g'));
$$;

ALTER TABLE patients
    ADD COLUMN normalized_first_name VARCHAR(100),
    ADD COLUMN normalized_last_name VARCHAR(100);

UPDATE patients SET
    normalized_first_name = normalize_patient_name(first_name),
    normalized_last_name = normalize_patient_name(last_name);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM patients
        GROUP BY normalized_first_name, normalized_last_name, birth_date HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Existen pacientes con los mismos nombres, apellidos y fecha de nacimiento'
            USING HINT = 'Revise los registros duplicados, incluidos los inactivos, antes de aplicar esta migración. No se han eliminado ni fusionado expedientes.';
    END IF;
END $$;

ALTER TABLE patients
    ALTER COLUMN normalized_first_name SET NOT NULL,
    ALTER COLUMN normalized_last_name SET NOT NULL,
    ADD CONSTRAINT uk_patient_identity UNIQUE (normalized_first_name, normalized_last_name, birth_date);

CREATE FUNCTION set_patient_identity() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    NEW.normalized_first_name := normalize_patient_name(NEW.first_name);
    NEW.normalized_last_name := normalize_patient_name(NEW.last_name);
    RETURN NEW;
END $$;

CREATE TRIGGER patient_identity_before_write BEFORE INSERT OR UPDATE ON patients
    FOR EACH ROW EXECUTE FUNCTION set_patient_identity();
