INSERT INTO manufacturer (name)
SELECT seed.name
FROM (
         VALUES
             ('Samsung'),
             ('Dell'),
             ('HP'),
             ('Apple'),
             ('Lenovo'),
             ('Acer'),
             ('AMD')
     ) AS seed(name)
WHERE NOT EXISTS (
    SELECT 1
    FROM manufacturer existing
    WHERE existing.name = seed.name
);