USE sunrise_dental;

INSERT INTO treatments (treatment_code, treatment_name, description, category, base_cost, duration_minutes) VALUES
('CONS',  'Consultation',           'New or returning patient oral examination',           'GENERAL',     2500.00,  20),
('SCAL',  'Scaling & Polishing',    'Professional cleaning to remove plaque and tartar',   'GENERAL',     6500.00,  45),
('FILL',  'Composite Filling',      'Tooth-coloured restoration for cavities',             'GENERAL',     8000.00,  40),
('RCT',   'Root Canal Treatment',   'Endodontic therapy to save an infected tooth',        'GENERAL',    25000.00,  90),
('EXT',   'Tooth Extraction',       'Simple or surgical removal of a tooth',               'SURGICAL',    7500.00,  30),
('WHIT',  'Teeth Whitening',        'In-clinic bleaching for a brighter smile',            'COSMETIC',   18000.00,  60),
('CRWN',  'Dental Crown',           'Custom crown to restore a damaged tooth',             'GENERAL',    35000.00,  60),
('BRCE',  'Braces Consultation',    'Orthodontic assessment and treatment planning',       'ORTHODONTIC', 5000.00,  30),
('PEDS',  'Pediatric Check-up',     'Child-friendly examination and fluoride advice',      'PEDIATRIC',   4000.00,  25),
('EMER',  'Emergency Care',         'Same-day pain relief and urgent treatment',           'EMERGENCY',  12000.00,  45);

INSERT INTO dentists (full_name, specialization, consultation_fee, phone, email, available) VALUES
('Dr. Anushka Perera',      'General Dentistry',     2500.00, '0771234001', 'anushka.perera@sunrisedental.lk', 1),
('Dr. Sahan Jayawardena',   'Orthodontics',          3500.00, '0771234002', 'sahan.j@sunrisedental.lk',        1),
('Dr. Malini Fernando',     'Pediatric Dentistry',   2800.00, '0771234003', 'malini.f@sunrisedental.lk',       1),
('Dr. Ruwan Silva',         'Oral Surgery',          4000.00, '0771234004', 'ruwan.silva@sunrisedental.lk',    1);
