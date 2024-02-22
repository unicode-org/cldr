UPDATE cldr_users SET password=SUBSTRING(MD5(RAND()) FROM 1 FOR 6), name=CONCAT(org,'#',id),email=CONCAT('u_',id,'@',LOWER(org),'.example.com') WHERE id>1;
UPDATE cldr_forum_posts SET text='(redacted 🆗)';
