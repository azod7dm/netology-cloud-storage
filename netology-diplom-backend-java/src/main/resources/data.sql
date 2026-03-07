INSERT INTO users (login, password_hash, salt)
VALUES ('testuser', 'vG4N+pjRo7Y1xdg35T/23Mrx1es9yIubaWe+E0hNBuE=', 'cGFzc3dvcmQ=')
ON CONFLICT (login) DO NOTHING;