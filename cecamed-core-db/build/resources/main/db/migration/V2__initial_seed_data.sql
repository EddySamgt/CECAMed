-- =============================================================================
-- CECAMed Database Schema Migration - V2__initial_seed_data.sql
-- Description: Semillas iniciales del horario de atención médica predeterminado
-- =============================================================================

INSERT INTO doctor_schedules (day_of_week, start_time, end_time, slot_duration_minutes, is_active, created_at, version)
VALUES 
    ('MONDAY', '08:00:00', '17:00:00', 30, TRUE, CURRENT_TIMESTAMP, 0),
    ('TUESDAY', '08:00:00', '17:00:00', 30, TRUE, CURRENT_TIMESTAMP, 0),
    ('WEDNESDAY', '08:00:00', '17:00:00', 30, TRUE, CURRENT_TIMESTAMP, 0),
    ('THURSDAY', '08:00:00', '17:00:00', 30, TRUE, CURRENT_TIMESTAMP, 0),
    ('FRIDAY', '08:00:00', '17:00:00', 30, TRUE, CURRENT_TIMESTAMP, 0),
    ('SATURDAY', '08:00:00', '12:00:00', 30, TRUE, CURRENT_TIMESTAMP, 0)
ON CONFLICT DO NOTHING;
