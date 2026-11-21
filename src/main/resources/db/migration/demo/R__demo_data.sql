INSERT INTO manufacturer (name)
SELECT seed.name
FROM (VALUES ('Samsung'),
             ('Dell'),
             ('HP'),
             ('Apple'),
             ('Lenovo'),
             ('Acer'),
             ('AMD')) AS seed(name)
WHERE NOT EXISTS (SELECT 1
                  FROM manufacturer existing
                  WHERE existing.name = seed.name);

INSERT INTO users (name, email, password)
SELECT seed.name,
       seed.email,
       seed.password_hash
FROM (VALUES ('Mercury', 'mercury@gmail.com', '${demoPasswordMercury}'),
             ('Venus', 'venus@gmail.com', '${demoPasswordVenus}'),
             ('Earth', 'earth@gmail.com', '${demoPasswordEarth}'),
             ('Mars', 'iamgod@gmail.com', '${demoPasswordMars}'),
             ('Jupiter', 'jupiter@gmail.com', '${demoPasswordJupiter}'),
             ('Saturn', 'saturn@gmail.com', '${demoPasswordSaturn}'),
             ('Uranus', 'uranus@gmail.com', '${demoPasswordUranus}'),
             ('Neptune', 'neptune@gmail.com',
              '${demoPasswordNeptune}')) AS seed(name, email, password_hash)
WHERE NOT EXISTS (SELECT 1
                  FROM users existing
                  WHERE LOWER(existing.email) = seed.email);

INSERT INTO client (user_id, card)
SELECT account.id,
       seed.card
FROM (VALUES ('mercury@gmail.com', '5326-1111'),
             ('venus@gmail.com', '5326-2222'),
             ('earth@gmail.com', '5326-3333'),
             ('iamgod@gmail.com', '5326-4444'),
             ('jupiter@gmail.com', '5326-5555'),
             ('saturn@gmail.com', '5326-6666'),
             ('uranus@gmail.com', '5326-7777'),
             ('neptune@gmail.com', '5326-8888')) AS seed(email, card)
         JOIN users account
              ON LOWER(account.email) = seed.email
WHERE NOT EXISTS (SELECT 1
                  FROM client existing
                  WHERE existing.user_id = account.id)
  AND NOT EXISTS (SELECT 1
                  FROM employee existing
                  WHERE existing.user_id = account.id);

INSERT INTO users (name, email, password)
SELECT seed.name,
       seed.email,
       seed.password_hash
FROM (VALUES ('Phobos', 'phobos@gmail.com', '${demoPasswordPhobos}'),
             ('Moon', 'moon@gmail.com', '${demoPasswordMoon}'),
             ('Deimos', 'deimos@gmail.com', '${demoPasswordDeimos}'),
             ('Europa', 'europa@gmail.com',
              '${demoPasswordEuropa}')) AS seed(name, email, password_hash)
WHERE NOT EXISTS (SELECT 1
                  FROM users existing
                  WHERE LOWER(existing.email) = seed.email);

INSERT INTO employee (user_id, department)
SELECT account.id,
       seed.department
FROM (VALUES ('phobos@gmail.com', 'salle'),
             ('moon@gmail.com', 'salle'),
             ('deimos@gmail.com', 'security'),
             ('europa@gmail.com', 'security')) AS seed(email, department)
         JOIN users account
              ON LOWER(account.email) = seed.email
WHERE NOT EXISTS (SELECT 1
                  FROM employee existing
                  WHERE existing.user_id = account.id)
  AND NOT EXISTS (SELECT 1
                  FROM client existing
                  WHERE existing.user_id = account.id);

INSERT INTO appliance (name,
                       category,
                       model,
                       manufacturer_id,
                       power_type,
                       characteristic,
                       description,
                       power,
                       price)
SELECT seed.name,
       seed.category,
       seed.model,
       manufacturer.id,
       seed.power_type,
       seed.characteristic,
       seed.description,
       seed.power,
       seed.price
FROM (VALUES ('Claw', 'BIG', '-----', 'Samsung',
              'ACCUMULATOR', '-------', '-----', 600, 1.01),

             ('Bane', 'SMALL', '-----', 'HP',
              'AC110', '--------', '-----', 2200, 2.01),

             ('Ecu', 'SMALL', '-----', 'Dell',
              'ACCUMULATOR', '--------', '-----', 800, 3.01),

             ('Kang Dae', 'BIG', '-----', 'Apple',
              'AC220', '--------', '-----', 3600, 4.01),

             ('Gust', 'BIG', '-----', 'Lenovo',
              'ACCUMULATOR', '--------', '-----', 650, 5.01),

             ('Ancile', 'SMALL', '-----', 'Acer',
              'AC220', '--------', '-----', 230, 6.01),

             ('Halo', 'BIG', '-----', 'AMD',
              'ACCUMULATOR', '--------', '-----', 300, 7.01)) AS seed(
                                                                      name,
                                                                      category,
                                                                      model,
                                                                      manufacturer_name,
                                                                      power_type,
                                                                      characteristic,
                                                                      description,
                                                                      power,
                                                                      price
    )
         JOIN manufacturer
              ON manufacturer.name = seed.manufacturer_name
WHERE NOT EXISTS (SELECT 1
                  FROM appliance existing
                  WHERE existing.manufacturer_id = manufacturer.id
                    AND existing.model = seed.model
                    AND existing.name = seed.name);
