-- © 2020 and later: Unicode, Inc. and others.
-- License & terms of use: http://www.unicode.org/copyright.html

-- Example use: (first arg is the organization)
-- CALL cldr_forum_participation('SurveyTool', '38');

DROP PROCEDURE IF EXISTS cldr_forum_participation;
-- @DELIMITER ;;
CREATE PROCEDURE cldr_forum_participation (my_org VARCHAR(256), my_version VARCHAR(122))
BEGIN
	-- Cleanup. (The tables will be cleaned up automatically per-connection otherwise)
	DROP TEMPORARY TABLE IF EXISTS myorgusers;
	DROP TEMPORARY TABLE IF EXISTS replycount;
	DROP TEMPORARY TABLE IF EXISTS allreplycount;
	
	-- This table is a subset of all users, only having our current org.
	CREATE TEMPORARY TABLE myorgusers SELECT * FROM cldr_users WHERE org = my_org;
	
	-- FIRST a table of OUR ORG replies
	CREATE TEMPORARY TABLE replycount
	SELECT 
	    p.loc AS loc,
	    p.root AS rootid,
	    COUNT(p.id) AS replies,
	    SUM(IF(p.type IN (3 , 4), 1, 0)) AS agreedecl,
	    MAX(p.last_time) AS lastorgpost
	FROM
	    cldr_forum_posts p
	WHERE
	    p.version = my_version AND p.parent <> - 1
	        AND EXISTS( SELECT 
	            u2.id
	        FROM
	            myorgusers u2
	        WHERE
	            p.poster = u2.id)
	GROUP BY p.loc , p.root;
	
	CREATE TEMPORARY TABLE allreplycount
	SELECT
		p.loc AS loc,
	    p.root AS rootid,
		COUNT(1) AS count,
	    MAX(p.last_time) AS last_time
	    FROM cldr_forum_posts p
	    WHERE
	    p.version = my_version AND p.parent <> -1
	    GROUP BY loc, rootid;
	
	DROP TEMPORARY TABLE IF EXISTS problemcount;
	
	CREATE TEMPORARY TABLE problemcount
	SELECT 
	    rp.loc as loc,
	    rp.id as id,
	    rp.type as type,
	    MAX(IF(rp.is_open = 1 
	    	AND u.id IS NULL 
	    	AND rp.type = 2 
	    	AND (c.agreedecl IS NULL OR c.agreedecl < 1), 1, 0)) 
				AS agreeneeded, -- open request from another org, and we didn't vote
	    MAX(IF(rp.type = 1 
	    	AND (c.lastorgpost IS NULL OR replies.last_time IS NULL OR c.lastorgpost < replies.last_time), 1, 0)) 
	    		AS discussneeded, -- discussion: [we didnt resp, nobody resp, we werent last]
	    MAX(IF(u.id IS NULL, 0, 1)) AS orgcount, -- count from our org
	    MAX(IF(rp.is_open = 1 AND u.id IS NOT NULL AND rp.type = 2, 1, 0)) AS openreq, -- open requests from our org
	    MAX(IF(rp.is_open = 1 AND u.id IS NOT NULL AND rp.type = 1, 1, 0)) AS opendisc -- open discussion from our org
	FROM
	    cldr_forum_posts rp
	    LEFT JOIN replycount c ON rp.id = c.rootid
	    LEFT JOIN allreplycount replies ON replies.rootid = rp.id
	    LEFT JOIN myorgusers u on u.id = rp.poster
	WHERE
	    rp.version = my_version AND rp.parent = - 1
	    AND rp.type IN (1,2) -- request or discuss
	    GROUP BY rp.loc, rp.id
	    ORDER BY rp.loc ASC;
	    

	-- Now finally we get back the full count
	SELECT 
	    pc.loc AS LOCALE,
	    COUNT(pc.id) AS FORUM_TOTAL,
	    SUM(pc.orgcount) AS FORUM_ORG,
	    SUM(pc.openreq) AS FORUM_REQUEST,
	    SUM(pc.opendisc) AS FORUM_DISCUSS,
	    SUM(pc.agreeneeded + pc.discussneeded) AS FORUM_ACT
	FROM
	    problemcount pc
	GROUP BY loc
	ORDER BY pc.loc ASC;
END;;
-- @DELIMITER ;


