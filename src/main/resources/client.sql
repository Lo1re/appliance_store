INSERT INTO users (name, email, password)
SELECT
    seed.name,
    seed.email,
    '$2a$10$0X8qfUJzuiWqumSPtgRp/uuUkctogXTvhYjOlZP64ErQMcdUInA2m'
FROM (
         VALUES
             ('Mercury', 'mercury@gmail.com'),
             ('Venus', 'venus@gmail.com'),
             ('Earth', 'earth@gmail.com'),
             ('Mars', 'iamgod@gmail.com'),
             ('Jupiter', 'jupiter@gmail.com'),
             ('Saturn', 'saturn@gmail.com'),
             ('Uranus', 'uranus@gmail.com'),
             ('Neptune', 'neptune@gmail.com')
     ) AS seed(name, email)
WHERE NOT EXISTS (
    SELECT 1
    FROM users existing
    WHERE LOWER(existing.email) = seed.email
);

INSERT INTO client (user_id, card)
SELECT
    account.id,
    seed.card
FROM (
         VALUES
             ('mercury@gmail.com', '5326-1111'),
             ('venus@gmail.com', '5326-2222'),
             ('earth@gmail.com', '5326-3333'),
             ('iamgod@gmail.com', '5326-4444'),
             ('jupiter@gmail.com', '5326-5555'),
             ('saturn@gmail.com', '5326-6666'),
             ('uranus@gmail.com', '5326-7777'),
             ('neptune@gmail.com', '5326-8888')
     ) AS seed(email, card)
         JOIN users account
              ON LOWER(account.email) = seed.email
WHERE NOT EXISTS (
    SELECT 1
    FROM client existing
    WHERE existing.user_id = account.id
)
  AND NOT EXISTS (
    SELECT 1
    FROM employee existing
    WHERE existing.user_id = account.id
);