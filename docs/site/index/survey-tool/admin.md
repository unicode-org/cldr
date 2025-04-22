---
title: "Survey Tool Administrator's Page"
---

## CLA

Note: A SurveyTool restart will be required to flush the CLA settings cache.

### Adding a CLA org

You can set a property in `cldr.properties` to temporarily add an Organization, pending a code update

```properties
# any orgs listed here will be added to the list of CLA orgs
ADD_CLA_ORGS=cherokee
```

### Updating the Organization status of a single user

The following SQL will update a single user (id 12) with a specific organization

```sql
-- get ready to update
-- find the set kind for SignedCla
SET @SET_CLA = (SELECT set_id FROM set_kinds WHERE set_name = 'SignedCla'); 
SET @UPDATE_ORG = 'airbnb'; SET @SIGNED_DATE='2023-08-28'; SET @CLDR_VER='48';
SET @UPDATE_USER = 12;
SELECT @SET_CLA,@UPDATE_ORG,@SIGNED_DATE,@CLDR_VER,@UPDATEUSER; -- verify vars
-- Update ONE USER
REPLACE INTO set_values (usr_id, set_id, set_value) 
  VALUES (@UPDATE_USER, @SET_CLA, 
    CONCAT('{"email":"-","name":"-","employer":"', 
        @UPDATE_ORG, 
        '","corporate":"true","version":"',
        @CLDR_VER,
        '","signed":"',
        @SIGNED_DATE,
        '","readonly":"true"}')
    );
-- update ALL USERS
SELECT id from CLDR_USERS WHERE org = @UPDATE_ORG;
-- now, double check (dumps the entire table)
SELECT usr_id, set_id, set_value, CONVERT(set_value USING utf8) AS j FROM set_values WHERE set_id = @SET_CLA AND usr_id = @UPDATE_USER;
```

### Updating the Organization status of an entire organization

If the in-Java org list isn't updated properly, you can use the script `cla-org-sql-update.sql` (in the repo) to add a stored procedure. (Load and execute the .sql first, it's not loaded by default.)

```sql
CALL update_org_cla('airbnb','48','2023-08-28');
```

This will update every user in the organization's CLA status.
