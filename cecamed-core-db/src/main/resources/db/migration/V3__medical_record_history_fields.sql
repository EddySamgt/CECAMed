ALTER TABLE medical_records
    ADD COLUMN gynecological_obstetric_history TEXT,
    ADD COLUMN water_glasses_per_day INTEGER CHECK (water_glasses_per_day >= 0),
    ADD COLUMN meals_per_day INTEGER CHECK (meals_per_day >= 0);
