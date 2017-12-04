-- Script for create the database schema of the project

CREATE TABLE IF NOT EXISTS telegram_user (
  id        INTEGER PRIMARY KEY,
  tag       VARCHAR(20) NOT NULL,
  bank_info TEXT
);


CREATE TABLE IF NOT EXISTS reason (
  id      INTEGER PRIMARY KEY,
  message VARCHAR(160) NOT NULL
);


CREATE TABLE IF NOT EXISTS debt (
  id              INTEGER PRIMARY KEY,
  user_from       INTEGER REFERENCES telegram_user (id),
  user_to         INTEGER REFERENCES telegram_user (id),
  amount          INTEGER NOT NULL,
  indebted_amount INTEGER NOT NULL,
  active          BOOLEAN DEFAULT 1,
  msg_id          INTEGER REFERENCES reason (id),
  creation_time   DATETIME DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS log (
  debt_id  INTEGER REFERENCES debt (id),
  amount   INTEGER NOT NULL,
  log_time DATETIME DEFAULT CURRENT_TIMESTAMP
);


CREATE VIEW IF NOT EXISTS active_debt AS
  SELECT
    user_from,
    user_to,
    sum(indebted_amount) AS amount
  FROM debt
  WHERE active = 1 GROUP BY user_to, user_from;