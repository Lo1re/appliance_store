INSERT INTO users (name, email, password) VALUES
                                              ('Phobos', 'phobos@gmail.com', '111'),
                                              ('Moon', 'moon@gmail.com', '222'),
                                              ('Deimos', 'deimos@gmail.com', '333'),
                                              ('Europa', 'europa@gmail.com', '444');

INSERT INTO employee (user_id, department) VALUES
                                               ((SELECT id FROM users WHERE email = 'phobos@gmail.com'), 'salle'),
                                               ((SELECT id FROM users WHERE email = 'moon@gmail.com'), 'salle'),
                                               ((SELECT id FROM users WHERE email = 'deimos@gmail.com'), 'security'),
                                               ((SELECT id FROM users WHERE email = 'europa@gmail.com'), 'security');