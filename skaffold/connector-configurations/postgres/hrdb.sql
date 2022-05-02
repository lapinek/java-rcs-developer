SELECT 'CREATE DATABASE hrdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hrdb') \gexec

\c hrdb

DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS account_groups;
DROP TABLE IF EXISTS organizations;

CREATE TABLE users(
    id BIGSERIAL NOT NULL PRIMARY KEY,
    uid char(32) NOT NULL,
    password char(128),
    firstname varchar(32) NOT NULL default '',
    lastname varchar(32) NOT NULL default '',
    fullname varchar(32),
    email varchar(32),
    organization varchar(32),
    timestamp TIMESTAMP(0) default CURRENT_TIMESTAMP
);

CREATE SEQUENCE account_groups_seq;

CREATE TABLE account_groups(
    id INT NOT NULL DEFAULT NEXTVAL ('account_groups_seq') PRIMARY KEY,
    gid char(32) NOT NULL,
    name varchar(32) NOT NULL default '',
    description varchar(32),
    timestamp TIMESTAMP(0) default CURRENT_TIMESTAMP
);

CREATE SEQUENCE organizations_seq;

CREATE TABLE organizations(
    id INT NOT NULL DEFAULT NEXTVAL ('organizations_seq') PRIMARY KEY,
    name varchar(32) NOT NULL default '',
    description varchar(32),
    timestamp TIMESTAMP(0) default CURRENT_TIMESTAMP
);

INSERT INTO users
( uid, password, firstname, lastname, fullname, email, organization, timestamp )
VALUES
('bob', 'password1','Bob', 'Fleming','Bob Fleming','Bob.Fleming@example.com','HR',CURRENT_TIMESTAMP),
('rowley', 'password2','Rowley','Birkin','Rowley Birkin','Rowley.Birkin@example.com','SALES',CURRENT_TIMESTAMP),
('louis', 'password3','Louis', 'Balfour','Louis Balfour','Louis.Balfour@example.com','SALES',CURRENT_TIMESTAMP),
('john', 'password4','John', 'Smith','John Smith','John.Smith@example.com','SUPPORT',CURRENT_TIMESTAMP),
('jdoe', 'password5','John', 'Doe','John Doe','John.Does@example.com','ENG',CURRENT_TIMESTAMP);

INSERT INTO account_groups VALUES ("0","100","admin","Admin group",CURRENT_TIMESTAMP);
INSERT INTO account_groups VALUES ("0","101","users","Users group",CURRENT_TIMESTAMP);

INSERT INTO organizations VALUES ("0","HR","HR organization",CURRENT_TIMESTAMP);
INSERT INTO organizations VALUES ("0","SALES","Sales organization",CURRENT_TIMESTAMP);
INSERT INTO organizations VALUES ("0","SUPPORT","Support organization",CURRENT_TIMESTAMP);
INSERT INTO organizations VALUES ("0","ENG","Engineering organization",CURRENT_TIMESTAMP);

-- SQLINES DEMO *** --------------------------
-- SQLINES DEMO *** r MySQL 5.6 and lower)
-- SQLINES DEMO *** -------------------------
-- SQLINES DEMO *** .* to root@'%' IDENTIFIED BY 'password';

-- SQLINES DEMO *** --------------------------
-- SQLINES DEMO *** r MySQL 5.7 and higher)
-- SQLINES DEMO *** -------------------------
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON hrdb.* TO 'root'@'%' WITH GRANT OPTION;