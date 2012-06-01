define({
	root: ({
			copyright: "(C) 2012 IBM Corporation and Others. All Rights Reserved",
			loading: "loading",
			loading2: "loading.",
			loading3: "loading..",
			loadingOneRow: "loading....",
			voting: "Voting",
			checking: "Checking",
			
			itemCount: "Items: ${itemCount}",
			itemCountHidden: "Items shown: ${itemCount}; Items hidden at ${coverage} coverage level: ${skippedDueToCoverage}",
			itemCountAllHidden: "No items visible due to coverage level.",
			itemCountNone: "No items!",
			noVotingInfo: " (no voting info received)",
			newDataWaiting: "(new data waiting)",

			clickToCopy: "click to copy to input box",
			file_a_ticket: "File a ticket...",
			file_ticket_unofficial: "This is not an official Survey Tool instance.",
			file_ticket_must: "You must file a ticket to modify this item.",

			htmlst: "Errors",
			htmldraft: "Approved",
			htmlvoted: "Voting",
			htmlcode: "Code",
			htmlbaseline: "$BASELINE_LANGUAGE_NAME",
			htmlproposed: "Proposed",
			htmlothers: "Others",
			htmlchange: "Change",
			htmlnoopinion: "Abstain",

			flyoverst: "Status Icon",
			flyoverdraft: "Draft Icon",
			flyovervoted: "Shows a checkmark if you voted",
			flyovercode: "Code for this item",
			flyoverbaseline: "Comparison value",
			flyoverproposed: "Winning value",
			flyoverothers: "Other non-winning items",
			flyoverchange: "Enter new values here",
			flyovernoopinion: "Abstain from voting on this row",
			
			draftStatus: "Status: ${0}",
			confirmed: "Confirmed", 
			approved: "Approved", 
			unconfirmed: "Unconfirmed", 
			contributed: "Contributed", 
			provisional: "Provisional",
			
			
			admin_settings: "Settings",
			admin_settings_desc: "Survey tool settings",
			adminSettingsChangeTemp: "Temporary change:",
			appendInputBoxChange: "Change",
			appendInputBoxCancel: "Clear",
			
			userlevel_admin: "Admin",
			userlevel_tc: "TC",
			userlevel_expert: "Expert",
			userlevel_vetter: "Vetter",
			userlevel_street: "Guest",
			userlevel_locked: "Locked",
			userlevel_manager: "Manager",

			userlevel_admin_desc: "Administrator",
			userlevel_tc_desc: "CLDR-Technical Committee member",
			userlevel_expert_desc: "Language Expert",
			userlevel_vetter_desc: "Regular Vetter",
			userlevel_street_desc: "Guest User",
			userlevel_manager_desc: "Project Manager",
			userlevel_locked_desc: "Locked User, no login",
			
			admin_threads: "Threads",
			admin_threads_desc: "All Threads",
			adminClickToViewThreads: "Click a thread to view its call stack",

			admin_exceptions: "Exception Log",
			admin_exceptions_desc: "Contents of the exceptions.log",
			adminClickToViewExceptions: "Click an exception to view its call stack",
			
			adminExceptionSQL_desc: "SQL state and code",
			adminExceptionSTACK_desc: "Exception call stack",
			adminExceptionMESSAGE_desc: "Exception message",
			adminExceptionUptime_desc: "ST uptime at stack time",
			adminExceptionHeader_desc: "Overall error message and cause",
			adminExceptionLogsite_desc: "Location of logException call",
			adminExceptionDup: "(${0} other time(s))",
			last_exception: "(last exception)",
			more_exceptions: "(more exceptions...)",
			no_exceptions: "(no exceptions.)",
			adminExceptionDupList: "List of other instances:",
			clickToSelect: "select",
			
			recentLoc: "Locale",
			recentXpath: "XPath",
			recentValue: "Value",
			recentWhen: "When",
			recentOrg: "Organization",
			recentNone: "No items to show.",
                        recentCount: "Count",
                        downloadXmlLink: "Download XML...",

			testOkay: "has no errors or warnings",
			testWarn: "has warnings",
			testError: "has errors",
			
			voTrue: "You have already voted on this item.",
			voFalse: "You have not yet voted on this item.",

			online: "Online",
			disconnected: "Disconnected",
			startup: "Starting up...",
			
			admin_users: "Users",
			admin_users_desc: "Currently logged-in users",
                        
                        // pClass ( see DataSection.java)
                        pClass_winner: "This item is currently winning.",
                        pClass_alias: "This item is aliased from another location.",
                        pClass_fallback_code: "This item is an untranslated code.",
                        pClass_fallback_root: "This item is inherited from the root locale.",
                        pClass_loser: "This is a proposed item which is not currently winning.",
                        pClass_fallback: "This item is inherited from ${inheritFromDisplay}.",
			
           // CheckCLDR.StatusAction 
           StatusAction_msg:              "Item was not submitted: ${0}",
           StatusAction_ALLOW:            "(Actually, it was allowed.)", // shouldn't happen
           StatusAction_FORBID:           "Forbidden.",
           StatusAction_FORBID_ERRORS:    "Item had errors.",
           StatusAction_FORBID_READONLY:  "Read-only.",
           StatusAction_FORBID_COVERAGE:  "Outside of coverage.",
                        
			"": ""})
//		"mt-MT": false
	
  // sublocales
});