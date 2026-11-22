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


-- Manufacturers used by the enriched local demonstration catalog.
INSERT INTO manufacturer(name)
SELECT seed.name FROM (VALUES ('Amazon'), ('Apple'), ('Asus'), ('Beats'), ('Dell'), ('Huawei'), ('Lenovo'), ('Oppo'), ('Realme'), ('Samsung')) AS seed(name)
WHERE NOT EXISTS (SELECT 1 FROM manufacturer existing WHERE existing.name = seed.name);


-- Upgrade only untouched legacy placeholders; preserve product IDs and order-row references.
UPDATE appliance SET name = 'Samsung Galaxy S10',
    model = 'Galaxy S10',
    category = 'SMALL',
    power_type = 'ACCUMULATOR',
    characteristic = 'Смартфон; сенсорний екран; USB-C; акумуляторне живлення',
    description = 'Компактний смартфон Samsung для спілкування, фотографування та щоденних справ. Зручний вибір для месенджерів, навігації й перегляду відео.',
    power = 15,
    price = 8999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Samsung')
WHERE name = 'Claw' AND model = '-----' AND description = '-----' AND price = 1.01
  AND category = 'BIG' AND power_type = 'ACCUMULATOR'
  AND characteristic = '-------' AND power = 600
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Samsung');

UPDATE appliance SET name = 'Apple MacBook Pro 14',
    model = 'MacBook Pro 14 M1 Pro',
    category = 'BIG',
    power_type = 'ACCUMULATOR',
    characteristic = 'Apple M1 Pro; 14-дюймовий дисплей; портативний ноутбук',
    description = 'Ноутбук Apple для роботи з документами, розробки та творчих завдань. Поєднує продуктивність чипа M1 Pro з якісним дисплеєм і компактним корпусом.',
    power = 67,
    price = 64999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Apple')
WHERE name = 'Bane' AND model = '-----' AND description = '-----' AND price = 2.01
  AND category = 'SMALL' AND power_type = 'AC110'
  AND characteristic = '--------' AND power = 2200
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'HP');

UPDATE appliance SET name = 'Dell XPS 13 9300',
    model = 'XPS 13 9300',
    category = 'BIG',
    power_type = 'ACCUMULATOR',
    characteristic = '13-дюймовий ноутбук; дисплей InfinityEdge; USB-C',
    description = 'Компактний Dell XPS для роботи й навчання. Тонкі рамки дисплея та невеликий корпус роблять його зручним для робочого столу й поїздок.',
    power = 45,
    price = 49999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Dell')
WHERE name = 'Ecu' AND model = '-----' AND description = '-----' AND price = 3.01
  AND category = 'SMALL' AND power_type = 'ACCUMULATOR'
  AND characteristic = '--------' AND power = 800
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Dell');

UPDATE appliance SET name = 'Apple iPhone 13 Pro',
    model = 'iPhone 13 Pro',
    category = 'SMALL',
    power_type = 'ACCUMULATOR',
    characteristic = 'Смартфон; iOS; потрійна камера; акумуляторне живлення',
    description = 'Смартфон Apple для повсякденного використання, фото та відео. Зручний для спілкування, мобільних застосунків і роботи з екосистемою Apple.',
    power = 20,
    price = 26999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Apple')
WHERE name = 'Kang Dae' AND model = '-----' AND description = '-----' AND price = 4.01
  AND category = 'BIG' AND power_type = 'AC220'
  AND characteristic = '--------' AND power = 3600
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Apple');

UPDATE appliance SET name = 'Lenovo Yoga 920',
    model = 'Yoga 920',
    category = 'BIG',
    power_type = 'ACCUMULATOR',
    characteristic = 'Ноутбук-трансформер; сенсорний екран; шарнір 360°',
    description = 'Універсальний Lenovo Yoga для навчання, роботи й презентацій. Корпус-трансформер дає змогу користуватися пристроєм як ноутбуком або в планшетному режимі.',
    power = 65,
    price = 32999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Lenovo')
WHERE name = 'Gust' AND model = '-----' AND description = '-----' AND price = 5.01
  AND category = 'BIG' AND power_type = 'ACCUMULATOR'
  AND characteristic = '--------' AND power = 650
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Lenovo');

UPDATE appliance SET name = 'Asus Zenbook Pro Duo',
    model = 'Zenbook Pro Duo',
    category = 'BIG',
    power_type = 'ACCUMULATOR',
    characteristic = 'Ноутбук; два екрани; клавіатура; акумуляторне живлення',
    description = 'Ноутбук Asus із додатковим екраном для багатозадачної роботи. Підходить для одночасної роботи з кількома вікнами, творчих проєктів і комунікації.',
    power = 180,
    price = 72999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Asus')
WHERE name = 'Ancile' AND model = '-----' AND description = '-----' AND price = 6.01
  AND category = 'SMALL' AND power_type = 'AC220'
  AND characteristic = '--------' AND power = 230
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Acer');

UPDATE appliance SET name = 'Huawei MateBook X Pro',
    model = 'MateBook X Pro',
    category = 'BIG',
    power_type = 'ACCUMULATOR',
    characteristic = 'Ноутбук; сенсорний дисплей; компактний корпус',
    description = 'Тонкий ноутбук Huawei для роботи та подорожей. Сенсорний дисплей і компактний корпус зручні для щоденних завдань, читання та презентацій.',
    power = 65,
    price = 42999,
    manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'Huawei')
WHERE name = 'Halo' AND model = '-----' AND description = '-----' AND price = 7.01
  AND category = 'BIG' AND power_type = 'ACCUMULATOR'
  AND characteristic = '--------' AND power = 300
  AND manufacturer_id = (SELECT id FROM manufacturer WHERE name = 'AMD');

-- Idempotent inserts never overwrite changes to an already configured model.
INSERT INTO appliance(name, category, model, manufacturer_id, power_type, characteristic, description, power, price)
SELECT seed.name, seed.category, seed.model, manufacturer.id, seed.power_type, seed.characteristic, seed.description, seed.power, seed.price
FROM (VALUES
    ('Samsung Galaxy S10', 'SMALL', 'Galaxy S10', 'Samsung', 'ACCUMULATOR', 'Смартфон; сенсорний екран; USB-C; акумуляторне живлення', 'Компактний смартфон Samsung для спілкування, фотографування та щоденних справ. Зручний вибір для месенджерів, навігації й перегляду відео.', 15, 8999),
    ('Apple MacBook Pro 14', 'BIG', 'MacBook Pro 14 M1 Pro', 'Apple', 'ACCUMULATOR', 'Apple M1 Pro; 14-дюймовий дисплей; портативний ноутбук', 'Ноутбук Apple для роботи з документами, розробки та творчих завдань. Поєднує продуктивність чипа M1 Pro з якісним дисплеєм і компактним корпусом.', 67, 64999),
    ('Dell XPS 13 9300', 'BIG', 'XPS 13 9300', 'Dell', 'ACCUMULATOR', '13-дюймовий ноутбук; дисплей InfinityEdge; USB-C', 'Компактний Dell XPS для роботи й навчання. Тонкі рамки дисплея та невеликий корпус роблять його зручним для робочого столу й поїздок.', 45, 49999),
    ('Apple iPhone 13 Pro', 'SMALL', 'iPhone 13 Pro', 'Apple', 'ACCUMULATOR', 'Смартфон; iOS; потрійна камера; акумуляторне живлення', 'Смартфон Apple для повсякденного використання, фото та відео. Зручний для спілкування, мобільних застосунків і роботи з екосистемою Apple.', 20, 26999),
    ('Lenovo Yoga 920', 'BIG', 'Yoga 920', 'Lenovo', 'ACCUMULATOR', 'Ноутбук-трансформер; сенсорний екран; шарнір 360°', 'Універсальний Lenovo Yoga для навчання, роботи й презентацій. Корпус-трансформер дає змогу користуватися пристроєм як ноутбуком або в планшетному режимі.', 65, 32999),
    ('Asus Zenbook Pro Duo', 'BIG', 'Zenbook Pro Duo', 'Asus', 'ACCUMULATOR', 'Ноутбук; два екрани; клавіатура; акумуляторне живлення', 'Ноутбук Asus із додатковим екраном для багатозадачної роботи. Підходить для одночасної роботи з кількома вікнами, творчих проєктів і комунікації.', 180, 72999),
    ('Huawei MateBook X Pro', 'BIG', 'MateBook X Pro', 'Huawei', 'ACCUMULATOR', 'Ноутбук; сенсорний дисплей; компактний корпус', 'Тонкий ноутбук Huawei для роботи та подорожей. Сенсорний дисплей і компактний корпус зручні для щоденних завдань, читання та презентацій.', 65, 42999),
    ('Oppo A57', 'SMALL', 'A57', 'Oppo', 'ACCUMULATOR', 'Смартфон; Android; сенсорний екран; USB-C', 'Практичний смартфон Oppo для дзвінків, повідомлень і мобільних застосунків. Підійде для щоденного спілкування, навігації та перегляду контенту.', 33, 6499),
    ('Oppo F19 Pro Plus', 'SMALL', 'F19 Pro Plus', 'Oppo', 'ACCUMULATOR', 'Смартфон; Android; камери; акумуляторне живлення', 'Смартфон Oppo для щоденних завдань і фотографування. Зручний для роботи з месенджерами, соціальними мережами та мультимедіа.', 50, 10999),
    ('Realme C35', 'SMALL', 'C35', 'Realme', 'ACCUMULATOR', 'Смартфон; Android; сенсорний дисплей', 'Доступний смартфон Realme для базових щоденних справ. Підходить для спілкування, онлайн-навчання, навігації та прослуховування музики.', 18, 4999),
    ('Realme X', 'SMALL', 'X', 'Realme', 'ACCUMULATOR', 'Смартфон; Android; дисплей; акумуляторне живлення', 'Смартфон Realme для звичного набору мобільних завдань. Переглядайте відео, спілкуйтеся та користуйтеся необхідними застосунками протягом дня.', 20, 8999),
    ('Realme XT', 'SMALL', 'XT', 'Realme', 'ACCUMULATOR', 'Смартфон; Android; камера; акумуляторне живлення', 'Смартфон Realme з акцентом на щоденне використання та фотографування. Зручний для месенджерів, соціальних мереж і перегляду відео.', 20, 9499),
    ('Samsung Galaxy S8', 'SMALL', 'Galaxy S8', 'Samsung', 'ACCUMULATOR', 'Смартфон; Android; USB-C; сенсорний дисплей', 'Компактний смартфон Samsung для дзвінків, фотографій і повсякденних застосунків. Модель попереднього покоління для базових мобільних завдань.', 15, 5999),
    ('Apple iPad mini 2021', 'SMALL', 'iPad mini 2021', 'Apple', 'ACCUMULATOR', 'Планшет; iPadOS; компактний сенсорний екран; USB-C', 'Компактний планшет Apple для читання, нотаток і мультимедіа. Його зручно брати із собою для навчання, подорожей або перегляду відео.', 20, 21999),
    ('Samsung Galaxy Tab S8 Plus', 'SMALL', 'Galaxy Tab S8 Plus', 'Samsung', 'ACCUMULATOR', 'Планшет; Android; великий сенсорний дисплей; USB-C', 'Планшет Samsung для роботи, навчання та відпочинку. Великий екран зручний для документів, презентацій, нотаток і перегляду відео.', 45, 31999),
    ('Amazon Echo Plus', 'SMALL', 'Echo Plus', 'Amazon', 'AC220', 'Розумна колонка; Wi-Fi; голосове керування', 'Домашня розумна колонка для музики та голосових сценаріїв. Допомагає організувати повсякденні завдання й озвучити кімнату.', 30, 4499),
    ('Apple AirPods', 'SMALL', 'AirPods', 'Apple', 'ACCUMULATOR', 'Бездротові навушники; Bluetooth; зарядний футляр', 'Легкі бездротові навушники Apple для музики, подкастів і дзвінків. Зарядний футляр зручно носити із собою протягом дня.', 5, 6499),
    ('Apple AirPods Max Silver', 'SMALL', 'AirPods Max', 'Apple', 'ACCUMULATOR', 'Повнорозмірні навушники; Bluetooth; акумулятор', 'Повнорозмірні навушники Apple для музики, відео та робочих дзвінків. М’які амбушури й бездротове підключення зручні для тривалого прослуховування.', 5, 23999),
    ('Apple HomePod mini', 'SMALL', 'HomePod mini', 'Apple', 'AC220', 'Розумна колонка; Wi-Fi; компактний корпус', 'Компактна колонка Apple для домашнього прослуховування музики. Займає небагато місця та доповнює домашню екосистему Apple.', 20, 5499),
    ('Beats Flex Wireless', 'SMALL', 'Flex Wireless', 'Beats', 'ACCUMULATOR', 'Бездротові навушники; Bluetooth; гнучка шийна дужка', 'Бездротові навушники Beats для музики й телефонних розмов. Гнучка конструкція зручна для використання в дорозі та щоденних справ.', 5, 2499)) AS seed(name,category,model,manufacturer_name,power_type,characteristic,description,power,price)
JOIN manufacturer ON manufacturer.name = seed.manufacturer_name
WHERE NOT EXISTS (SELECT 1 FROM appliance existing
                  WHERE existing.manufacturer_id = manufacturer.id AND existing.model = seed.model);
