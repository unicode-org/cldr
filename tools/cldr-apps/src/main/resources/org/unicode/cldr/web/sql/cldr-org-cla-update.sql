-- © 2025 and later: Unicode, Inc. and others.
-- SPDX-License-Identifier: Unicode-3.0

-- USAGE:
--  CALL update_org_cla('airbnb','48','2023-08-28'); 
-- pass in the org, the version, and the signed date
-- will update all users in the ENTIRE org.

DROP PROCEDURE IF EXISTS update_org_cla;
DELIMITER ;;
CREATE PROCEDURE update_org_cla (IN my_org VARCHAR(256), IN my_version VARCHAR(122), IN signed_date VARCHAR(20))
BEGIN
	-- the set_id for CLA requests
	DECLARE set_cla INT;
    DECLARE usr_id INT;
    DECLARE usr_email VARCHAR(256);
    DECLARE usr_name VARCHAR(256);
    -- cursor for users in the org
    DECLARE done INT DEFAULT FALSE;
	DECLARE usr CURSOR FOR SELECT id, email, name FROM cldr_users WHERE org = my_org; 
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    -- get the set_id for set_cla
	SELECT set_id FROM set_kinds WHERE set_name = 'SignedCla' INTO set_cla;
    OPEN usr;
    usr_loop: LOOP
		FETCH usr INTO usr_id, usr_email, usr_name;
        IF done THEN
			LEAVE usr_loop;
		END IF;
        SET @v = CONCAT(
				'{"email":"', usr_email, 
                '","name":"', usr_name, 
                '","employer":"', my_org, 
                '","corporate":"true","version":"', my_version,
                '","signed":"', signed_date,
                '","readonly":"true"}');
		-- Uncomment the following line to get a debug trace of each row.
        -- SELECT usr_id, set_cla, @v; 
        REPLACE INTO set_values (usr_id, set_id, set_value) 
 			VALUES (usr_id, set_cla, @v);
    END LOOP;
END;;
DELIMITER ;
