CREATE TABLE IF NOT EXISTS destination (
    id IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100)
);
