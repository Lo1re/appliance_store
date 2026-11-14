INSERT INTO appliance (
    name,
    category,
    model,
    manufacturer_id,
    power_type,
    characteristic,
    description,
    power,
    price
)
SELECT
    seed.name,
    seed.category,
    seed.model,
    manufacturer.id,
    seed.power_type,
    seed.characteristic,
    seed.description,
    seed.power,
    seed.price
FROM (
         VALUES
             ('Claw', 'BIG', '-----', 'Samsung',
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
              'ACCUMULATOR', '--------', '-----', 300, 7.01)
     ) AS seed(
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
WHERE NOT EXISTS (
    SELECT 1
    FROM appliance existing
    WHERE existing.manufacturer_id = manufacturer.id
      AND existing.model = seed.model
      AND existing.name = seed.name
);