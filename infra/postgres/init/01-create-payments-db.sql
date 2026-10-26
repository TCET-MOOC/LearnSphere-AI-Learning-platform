-- The official postgres image creates POSTGRES_DB (MOOC) before running
-- this directory. The payments service needs its own database on the same
-- server.
CREATE DATABASE learnsphere_payments;
