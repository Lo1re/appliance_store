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
FROM (VALUES ('Mercury', 'mercury@gmail.com', '$2a$10$1AKtymfGSBjTIz.y82Jdke2w8Z2lE.KGMhD1ZYtKDiYX8Pa7cx/Da'),
             ('Venus', 'venus@gmail.com', '$2a$10$.2BYYUwe5RPu/Rsx8x4d5.TK2RhpvZy/sw3l5E0uXEqF7n1Xv6edq'),
             ('Earth', 'earth@gmail.com', '$2a$10$2/zwG0mdZsM6AM0fnESg9uQo1kDebpsPnbO74BRvqJOis6MO7mEbu'),
             ('Mars', 'iamgod@gmail.com', '$2a$10$I0n6u1r2rnIRyY5jQSBesuyIi3FY4RHvo35oL65V2Cu2ZVzwUFt9S'),
             ('Jupiter', 'jupiter@gmail.com', '$2a$10$6gaQ.8b21MCzSdJxSN04W.cIgeyZvZR5L6ul8V1/AVddHqyChxWqS'),
             ('Saturn', 'saturn@gmail.com', '$2a$10$XvZyRsOSGFw7QFi3nT8fGuratMseTDQnYvxJJm7oES2.5KaWVcyva'),
             ('Uranus', 'uranus@gmail.com', '$2a$10$Iw.ZO/zV1EbG2XrLE8J68OF6N9c1Yb1i7WJ8FAXNL44K/F6TDG4Cy'),
             ('Neptune', 'neptune@gmail.com',
              '$2a$10$oK7l/HpJS5ye1f4OVYBQAuysFDfKUt6puVjgnbizM9YlyzbE2ct3K')) AS seed(name, email, password_hash)
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
FROM (VALUES ('Phobos', 'phobos@gmail.com', '$2a$10$PsRnmS7huldXCKf0kWLObOB7ndzQZaCC3Bt/gO6X4kohWInxkC//W'),
             ('Moon', 'moon@gmail.com', '$2a$10$.HBCvQaK.5FhG249uYw83uynn8LHtRmXXuGhC0UnSX1IpcfiMCCGq'),
             ('Deimos', 'deimos@gmail.com', '$2a$10$upqOsEiH1ZOe7GGUzqYXXOnDHDeTGp0MSNCwHHKCvb1yNDRORqzIq'),
             ('Europa', 'europa@gmail.com',
              '$2a$10$iHRrWTZZum1KLNm0PBb5AOIdwtnayXtpFjPNwZVSNAmPDQYchE4Fy')) AS seed(name, email, password_hash)
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
