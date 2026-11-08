INSERT INTO users (name, email, password) VALUES
                                              ('Mercury', 'mercury@gmail.com', '111'),
                                              ('Venus', 'venus@gmail.com', '222'),
                                              ('Earth', 'earth@gmail.com', '333'),
                                              ('Mars', 'iamgod@gmail.com', '444'),
                                              ('Jupiter', 'jupiter@gmail.com', '55555'),
                                              ('Saturn', 'saturn@gmail.com', '666'),
                                              ('Uranus', 'uranus@gmail.com', '777'),
                                              ('Neptune', 'neptune@gmail.com', '888');

INSERT INTO client (user_id, card) VALUES
                                       ((SELECT id FROM users WHERE email = 'mercury@gmail.com'), '5326-1111'),
                                       ((SELECT id FROM users WHERE email = 'venus@gmail.com'), '5326-2222'),
                                       ((SELECT id FROM users WHERE email = 'earth@gmail.com'), '5326-3333'),
                                       ((SELECT id FROM users WHERE email = 'iamgod@gmail.com'), '5326-4444'),
                                       ((SELECT id FROM users WHERE email = 'jupiter@gmail.com'), '5326-5555'),
                                       ((SELECT id FROM users WHERE email = 'saturn@gmail.com'), '5326-6666'),
                                       ((SELECT id FROM users WHERE email = 'uranus@gmail.com'), '5326-7777'),
                                       ((SELECT id FROM users WHERE email = 'neptune@gmail.com'), '5326-8888');