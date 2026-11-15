-- Campus Resource Exchange - sample data. Run AFTER schema.sql.
USE campus_exchange;

INSERT INTO users (name, email) VALUES
    ('Aarav Sharma', 'aarav@srmist.edu.in'),
    ('Priya Nair',   'priya@srmist.edu.in'),
    ('Rohan Verma',  'rohan@srmist.edu.in');

-- owner_id 1 = Aarav, 2 = Priya, 3 = Rohan
INSERT INTO resources (title, category, status, owner_id) VALUES
    ('Introduction to Algorithms (CLRS)', 'Books',         'Available', 1),
    ('Head First Java',                    'Books',         'Available', 2),
    ('Casio fx-991EX Classwiz',            'Calculators',   'Available', 1),
    ('Casio fx-82MS',                      'Calculators',   'Borrowed',  3),
    ('Lab Coat (Size M)',                  'Lab Equipment', 'Available', 2),
    ('Vernier Caliper',                    'Lab Equipment', 'Available', 3),
    ('DBMS Unit 1-5 Handwritten Notes',    'Notes',         'Available', 1),
    ('AOOP Lab Record (2025)',             'Notes',         'Available', 3),
    ('Arduino Uno Starter Kit',            'Electronics',   'Available', 2),
    ('Digital Multimeter',                 'Electronics',   'Available', 1);

-- Priya borrowed Rohan's fx-82MS (matches its 'Borrowed' status above)
INSERT INTO requests (resource_id, borrower_id, request_date) VALUES
    (4, 2, CURDATE());
