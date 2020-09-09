
drop schema if exists cldrtest;
create schema cldrtest;
flush privileges;
drop user if exists 'cldrtest'@'localhost';
CREATE USER 'cldrtest'@'localhost' IDENTIFIED BY 'VbrB3LFCr6A!';
GRANT ALL PRIVILEGES ON cldrtest . * TO 'cldrtest'@'localhost';
flush privileges;
