-- =============================================================================
-- CECAMed Database Schema Migration - V1__init_schema.sql
-- Database: PostgreSQL 14+
-- Description: Creación inicial de tablas, restricciones e índices para el sistema clínico
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Tabla de Pacientes (patients)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    identification_number VARCHAR(50) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_type VARCHAR(20) DEFAULT 'DESCONOCIDO',
    phone VARCHAR(25),
    email VARCHAR(100),
    address VARCHAR(255),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(25),
    emergency_contact_relationship VARCHAR(50),
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_patient_dni ON patients(identification_number);
CREATE INDEX IF NOT EXISTS idx_patient_names ON patients(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_patient_phone ON patients(phone);
CREATE INDEX IF NOT EXISTS idx_patient_active ON patients(active);

-- -----------------------------------------------------------------------------
-- 2. Tabla de Expedientes Clínicos Base (medical_records)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medical_records (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL UNIQUE REFERENCES patients(id) ON DELETE CASCADE,
    record_number VARCHAR(50) NOT NULL UNIQUE,
    allergies TEXT,
    pathological_history TEXT,
    non_pathological_history TEXT,
    family_history TEXT,
    surgical_history TEXT,
    current_medications TEXT,
    general_observations TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_record_number ON medical_records(record_number);
CREATE UNIQUE INDEX IF NOT EXISTS idx_record_patient_id ON medical_records(patient_id);

-- -----------------------------------------------------------------------------
-- 3. Tabla de Consultas Médicas / Notas de Evolución (medical_consultations)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS medical_consultations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    consultation_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    symptoms TEXT,
    physical_examination TEXT,
    -- Signos Vitales (VitalSigns Embedded)
    systolic_pressure INTEGER,
    diastolic_pressure INTEGER,
    heart_rate INTEGER,
    respiratory_rate INTEGER,
    temperature_celsius NUMERIC(4, 2),
    weight_kg NUMERIC(5, 2),
    height_cm NUMERIC(5, 2),
    bmi NUMERIC(4, 2),
    oxygen_saturation_percentage INTEGER,
    diagnosis TEXT NOT NULL,
    icd10_code VARCHAR(20),
    treatment_plan TEXT,
    private_notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_consultation_patient_id ON medical_consultations(patient_id);
CREATE INDEX IF NOT EXISTS idx_consultation_date_time ON medical_consultations(consultation_date_time);

-- -----------------------------------------------------------------------------
-- 4. Tabla de Documentos Adjuntos (patient_documents)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS patient_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    consultation_id BIGINT REFERENCES medical_consultations(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    description TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_doc_patient_id ON patient_documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_doc_consultation_id ON patient_documents(consultation_id);
CREATE INDEX IF NOT EXISTS idx_doc_type ON patient_documents(document_type);

-- -----------------------------------------------------------------------------
-- 5. Tabla de Citas Médicas y Google Calendar (appointments)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PROGRAMADA',
    reason_for_visit VARCHAR(255) NOT NULL,
    cancellation_reason TEXT,
    notes TEXT,
    google_event_id VARCHAR(255),
    google_calendar_id VARCHAR(255),
    google_sync_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    google_html_link VARCHAR(500),
    google_last_synced_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_appointment_patient_id ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointment_time_range ON appointments(start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_appointment_status ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointment_google_event_id ON appointments(google_event_id);
CREATE INDEX IF NOT EXISTS idx_appointment_sync_status ON appointments(google_sync_status);

-- -----------------------------------------------------------------------------
-- 6. Tabla de Horario Habitual de Atención (doctor_schedules)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS doctor_schedules (
    id BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_schedule_day_active ON doctor_schedules(day_of_week, is_active);

-- -----------------------------------------------------------------------------
-- 7. Tabla de Bloqueos de Agenda / Días No Laborables (schedule_blocks)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS schedule_blocks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    start_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    block_type VARCHAR(30) NOT NULL DEFAULT 'PERSONAL',
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    reason TEXT,
    google_event_id VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_block_time_range ON schedule_blocks(start_date_time, end_date_time);
CREATE INDEX IF NOT EXISTS idx_block_type ON schedule_blocks(block_type);

-- -----------------------------------------------------------------------------
-- 8. Tabla de Insumos y Medicamentos (inventory_items)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    generic_name VARCHAR(150),
    category VARCHAR(40) NOT NULL,
    description TEXT,
    unit_of_measure VARCHAR(30) NOT NULL,
    current_stock NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    min_stock_alert NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    max_stock NUMERIC(12, 2),
    unit_cost NUMERIC(12, 2),
    selling_price NUMERIC(12, 2),
    location_in_clinic VARCHAR(100),
    expiration_date DATE,
    lot_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_item_code ON inventory_items(code);
CREATE INDEX IF NOT EXISTS idx_item_name ON inventory_items(name);
CREATE INDEX IF NOT EXISTS idx_item_category ON inventory_items(category);
CREATE INDEX IF NOT EXISTS idx_item_stock_alert ON inventory_items(current_stock, min_stock_alert);
CREATE INDEX IF NOT EXISTS idx_item_expiration ON inventory_items(expiration_date);

-- -----------------------------------------------------------------------------
-- 9. Tabla de Movimientos de Inventario / Kardex (inventory_movements)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_movements (
    id BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id) ON DELETE RESTRICT,
    movement_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(12, 2) NOT NULL,
    stock_before NUMERIC(12, 2) NOT NULL,
    stock_after NUMERIC(12, 2) NOT NULL,
    unit_cost_at_moment NUMERIC(12, 2),
    patient_id BIGINT REFERENCES patients(id) ON DELETE SET NULL,
    consultation_id BIGINT REFERENCES medical_consultations(id) ON DELETE SET NULL,
    reason_notes TEXT,
    movement_date_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    performed_by VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_movement_item_id ON inventory_movements(inventory_item_id);
CREATE INDEX IF NOT EXISTS idx_movement_date_time ON inventory_movements(movement_date_time);
CREATE INDEX IF NOT EXISTS idx_movement_type ON inventory_movements(movement_type);
