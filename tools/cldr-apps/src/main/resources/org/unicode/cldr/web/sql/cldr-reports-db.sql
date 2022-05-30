-- This is called from ReportsDB.setupDB
-- note that VOTE_REPORTS is substituted, as is 'last_mod TIMESTAMP'
CREATE TABLE VOTE_REPORTS (
    submitter int not null,
    locale varchar(35),
    report varchar(15),
    completed boolean,
    acceptable boolean,
    last_mod TIMESTAMP
);

CREATE INDEX VOTE_REPORTS_slr ON VOTE_REPORTS (submitter, locale, report);
