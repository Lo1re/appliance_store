INSERT INTO users (name, email, password)
SELECT
    seed.name,
    seed.email,
    '$2a$10$5ofiCZ7taCZGYQh88t6m7OrNtvMf6bBkdF2CrzdDIu1jIPjQek41W'
FROM (
         VALUES
             ('Phobos', 'phobos@gmail.com'),
             ('Moon', 'moon@gmail.com'),
             ('Deimos', 'deimos@gmail.com'),
             ('Europa', 'europa@gmail.com')
     ) AS seed(name, email)
WHERE NOT EXISTS (
    SELECT 1
    FROM users existing
    WHERE LOWER(existing.email) = seed.email
);

INSERT INTO employee (user_id, department)
SELECT
    account.id,
    seed.department
FROM (
         VALUES
             ('phobos@gmail.com', 'salle'),
             ('moon@gmail.com', 'salle'),
             ('deimos@gmail.com', 'security'),
             ('europa@gmail.com', 'security')
     ) AS seed(email, department)
         JOIN users account
              ON LOWER(account.email) = seed.email
WHERE NOT EXISTS (
    SELECT 1
    FROM employee existing
    WHERE existing.user_id = account.id
)
  AND NOT EXISTS (
    SELECT 1
    FROM client existing
    WHERE existing.user_id = account.id
);