/*
 * cldrText: encapsulate text messages for the user interface.
 */
const CLDR_TEXT_DEBUG = false;

const strings = {
  git_commit_url_prefix: "https://github.com/unicode-org/cldr/commit/",
  git_compare_url_prefix: "https://github.com/unicode-org/cldr/compare/",
  git_compare_url_main: "main",
  git_compare_url_release: "release-${0}",

  loading: "loading",
  loading2: "loading.",
  loading3: "loading..",

  loadingMsg_desc: "Current loading status",
  loading_reloading: "Force Reloading Page",
  loading_reload: "Reload This Page",
  loading_home: "Start Over",
  loading_retrying: "Retrying",
  loading_nocontent: "This locale cannot be displayed.",
  loadingOneRow: "loading....",
  voting: "Voting",

  emailHidden: "(hidden)",
  noVotingInfo: " (no voting info received)",
  newDataWaiting: "(new data waiting)",

  clickToCopy: "click to copy to input box",
  file_a_ticket:
    "Read all above links, and then click here to file a ticket only if changes are necessary. Do not submit a translation of this text without reading the above text and links.",
  file_ticket_unofficial: "This is not an official Survey Tool instance.",
  file_ticket_must: "You must file a ticket to modify this item.",
  file_ticket_notice: "May not be modified- see details.",

  htmldraft: "A",
  htmlcode: "Code",
  htmltranshint: "$TRANS_HINT_LANGUAGE_NAME",
  htmlproposed: "Winning",
  htmlothers: "Others",
  htmltoadd: "Add",
  htmlnoopinion: "Abstain",

  possibleProblems: "Possible problems with this locale:",

  flyoveradd: "Add another value",
  flyoverdraft: "Approval Status",
  flyovercode: "Code for this item",
  flyovercomparison: "Comparison value",
  flyoverproposed: "Winning value",
  flyoverothers: "Other non-winning items",
  flyovernoopinion: "Abstain from voting on this item",
  "i-override_desc":
    "You have voted on this item with a lower vote count (shown in parenthesis).",

  draftStatus: "Status: ${0}",
  confirmed: "Confirmed",
  approved: "Approved",
  unconfirmed: "Unconfirmed",
  contributed: "Contributed",
  provisional: "Provisional",
  missing: "Missing",
  "inherited-unconfirmed": "Inherited and Unconfirmed",
  "inherited-provisional": "Inherited and Provisional",

  adminDeadThreadsHeader: "Deadlocked Threads!",

  admin_settings: "Settings",
  admin_settings_desc: "Survey tool settings",
  adminSettingsChangeTemp: "Temporary change:",
  appendInputBoxChange: "Change",
  appendInputBoxCancel: "Clear",

  userlevel_admin: "Admin",
  userlevel_tc: "TC",
  userlevel_vetter: "Vetter",
  userlevel_guest: "Guest",
  userlevel_locked: "Locked",
  userlevel_manager: "Manager",

  userlevel_admin_desc: "Administrator",
  userlevel_tc_desc: "CLDR-Technical Committee member",
  userlevel_vetter_desc: "Regular Vetter",
  userlevel_guest_desc: "Guest User",
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

  admin_ops: "Actions",
  admin_ops_desc: "Administrative Actions",

  notselected_desc: "",

  recentLoc: "Locale",
  recentXpath: "XPath",
  recentXpathCode: "XPath Code",
  recentValue: "Value",
  recentWhen: "When",
  recentOrg: "Organization",
  recentNone: "No items to show.",
  recentCount: "Count",
  downloadXmlLink: "Download XML...",
  downloadCsvLink: "Download CSV...",

  testOkay: "has no errors or warnings",
  testWarn: "has warnings",
  testError: "has errors",

  voTrue: "You have already voted on this item.",
  voFalse: "You have not yet voted on this item.",

  online: "Online",
  disconnected: "Disconnected",
  error_restart: "(May be due to Survey Tool restart on server)",
  error: "Disconnected: Error",
  details: "Details...",
  startup: "Starting up...",

  admin_users: "Users",
  admin_users_desc: "Currently logged-in users",
  admin_users_action_kick: "Kick",
  admin_users_action_kick_desc: "Logout this user",

  // pClass ( see DataPage.java)
  pClass_winner: "This item is currently winning.",
  pClass_alias: "This item is aliased from another location.",
  pClass_fallback_code: "This item is an untranslated code.",
  pClass_fallback_root: "This item is inherited from the root locale.",
  pClass_loser: "This item is currently losing.",
  pClass_fallback: "This item is inherited.",
  pClassExplain_desc: "This area shows the item's status.",
  followAlias: "Jump to Original ⇒",
  noFollowAlias: "This item is constructed from other values.",

  override_explain_msg:
    "You have voted for this item with ${overrideVotes} votes instead of the usual ${votes}",
  voteInfo_overrideExplain_desc: "",
  mustflag_explain_msg:
    "The item you voted for is not winning. However, you may post a forum entry to flag the item for Committee review.",
  voteInfo_mustflag_explain_desc: "",
  flag_desc:
    "This item has been flagged for review by the CLDR Technical Committee.",
  flag_d_desc: "Losing items may be flagged for CLDR Committee review.",
  explainRequiredVotes: "Changes to this item require ${requiredVotes} votes.",
  valueIsLocked:
    "This item has been locked by the CLDR Technical Committee. See the forum entry.",
  xpath_desc:
    "This is the XPath denoting the currently clicked item. For more information, see http://cldr.unicode.org (click to select)",

  winningStatus_disputed: "Disputed",
  winningStatus_msg: "${1} ${0} Value ",

  dataPageInitialGuidance:
    "Please consult the <a target='_blank' href='http://cldr.unicode.org/translation/getting-started/guide'>Instructions <span class='glyphicon glyphicon-share'></span></a> page.<br/><br/>Briefly, for each row:<br/><ol><li>Click on a cell in the 'Code' column.</li><li>Read the details that appear in the right panel (widen your window to see it).</li><li> Hover over the English and the Winning value to see examples.</li><li>To vote:<ol><li>for an existing item in the Winning or Others column, click on the <input type='radio'/> for that item.</li><li>for a new value, click on the button in the \"Add\" column. A new editing box will open. Enter the new value and hit RETURN.</li><li>for no value (abstain, or retract a vote), click on the  <input type='radio'/> in the Abstain column.</li></ol></li></ol>",
  generalPageInitialGuidance:
    "This area will show details of items as you work with the Survey Tool.",
  generalSpecialGuidance:
    "Please hover over the sidebar to choose a section to begin entering data. If you have not already done so, please read the <a target='_blank' href='http://www.unicode.org/cldr/survey_tool.html'>Instructions</a>, particularly the Guide and the Walkthrough. You can also use the Dashboard to see all the errors, warnings, and missing items in one place.",

  loginGuidance: "You may not make any changes, you are not logged in.",
  readonlyGuidance: "You may not make changes to this locale.",

  htmlvorg: "Org",
  htmlvorgvote: "Organization's vote",
  htmlvdissenting: "Dissenting Votes",
  flyovervorg: "List of Organizations",
  flyovervorgvote: "The final vote for this organization",
  flyovervdissenting:
    "Other votes cast against the final vote by members of the organization",
  voteInfoScorebox_msg: "${0}: ${1}",
  voteInfo_established_url:
    "http://cldr.unicode.org/index/process#TOC-Draft-Status-of-Optimal-Field-Value",
  voteInfo_orgColumn: "Org.",
  voteInfo_noVotes: "(no votes)",
  voteInfo_anon: "(imported anonymously)",
  voteInfo_iconBar_desc: "This area shows the status of each candidate item.",
  voteInfo_noVotes_desc: "There were no votes for this item.",
  voteInfo_key: "Key:",
  voteInfo_valueTitle_desc: "Item's value",
  voteInfo_orgColumn_desc: "Which organization is voting",
  voteInfo_voteTitle_desc: "The total vote score for this value",
  voteInfo_orgsVote_desc: "This vote is the organization's winning vote",
  voteInfo_orgsNonVote_desc: "This vote is not the organization's winning vote",
  voteInfo_baseline_desc:
    "This is the “baseline” data. See http://cldr.unicode.org/translation/getting-started/guide#TOC-Icons",
  voteInfo_winningItem_desc:
    "This mark shows the item which is currently winning.",
  voteInfo_winningKey_desc:
    "This mark shows the item which is currently winning.",
  voteInfo_perValue_desc:
    "This shows the state and voters for a particular item.",
  voteInfo_moreInfo:
    "Click here for a full explanation of the icons and their meanings.",
  voteInfo_votesForInheritance: "These are votes for inheritance.",
  voteInfo_votesForSpecificValue:
    "These are votes for the specific value currently matching the inherited value. Votes for this specific value are combined with any votes for inheritance.",
  // CheckCLDR.StatusAction
  StatusAction_msg: "Not submitted: ${0}",
  StatusAction_popupmsg:
    "Sorry, your vote for '${1}' could not be submitted: ${0}", // same as StatusAction_msg but with context
  StatusAction_ALLOW: "(Actually, it was allowed.)", // shouldn't happen
  StatusAction_FORBID_ERRORS: "The item had errors.",
  StatusAction_FORBID_READONLY: "The item is read-only.",
  StatusAction_FORBID_NULL: "The item has no value.",
  StatusAction_FORBID_ROOT: "The item is a root annotation code.",
  StatusAction_FORBID_PERMANENT_WITHOUT_FORUM:
    "A forum entry is required to make a Permanent vote.",
  StatusAction_FORBID_CODE: "The item is the same as the code.",

  "v-title2_desc": "Locale title",
  v_bad_special_msg:
    'Bad URL (mistyped?), unknown special action: "${special}"',
  v_oldvotes_title: "Old Votes",
  v_oldvotes_count_msg: "Vote Count: ${count}",
  v_oldvotes_title_uncontested: "Winning Votes",
  v_oldvotes_title_contested: "Losing Votes",
  v_oldvotes_locale_list_help_msg:
    "Listed are locales that you have voted for in previous releases of CLDR. Click one to review and import your old votes. Note that some of the locales listed may have votes which are no longer applicable in CLDR.",
  v_oldvotes_return_to_locale_list: "Return to List of Locales with old votes",
  v_oldvotes_path: "Path",
  v_oldvotes_locale_msg:
    "Below are your previous votes that differed from the released value in the last release (CLDR ${version}) in ${locale}. You can import them by selecting individual items or by group. Select/Unselect All options are available at the bottom of this page.",
  "v-oldvotes-loc-help_desc": "Specific help on this locale's old votes",
  "v-oldvotes-desc_desc": "Specific help on this type of vote",
  "v-accept_desc":
    "Checked items will be imported, unchecked items will not be imported.",
  code_desc: "The short code for this item. ",
  "v-path_desc":
    "The short code for this item. Click here to view the item, in a new window.",
  "v-comp_desc": "The comparison value (English)",
  "v-win_desc": "This was the winning value for the earlier CLDR",
  "v-mine_desc": "This was your vote from the earlier CLDR",
  pathChunk_desc: "This header separates common items",
  v_oldvotes_winning_msg: "CLDR ${version} winning",
  v_oldvotes_mine: "My old vote",
  v_oldvotes_accept: "Import?",
  v_oldvotes_go: "view",
  v_oldvotes_hide: "Close this section",
  v_oldvotes_show: "Show: ",
  v_oldvotes_all: "Select All",
  v_oldvotes_none: "Unselect All",
  v_oldvotes_all_section: "Select/Unselect All in section: ",
  v_oldvotes_no_contested: "No losing votes.",
  v_oldvotes_no_old_here:
    "No old votes to import. You're done with this locale!",
  v_oldvotes_no_old: "No old votes to import. You're done with old votes!",
  v_submit_msg: "Import selected items",
  v_submit_busy: "Submitting...",
  v_oldvote_auto_msg: "Automatic Import",
  v_oldvote_auto_progress_msg:
    "Please wait while your old winning votes are imported...",
  v_oldvote_auto_desc_msg:
    "${count} old winning votes were automatically imported",
  "v-title_desc":
    "This area shows the date before which votes are considered “old”.",

  section_forum: "Forum",
  section_subpages: "Subpages",

  searchNoResults: "No results found.",
  searchGuidance:
    "This is a basic search facility. An exact word such as 'Monday' or 'Montag' can be entered, or an XPath or string ID like 'eeaf1f975877a5d'.  An optional locale ID can be prefixed to any search term, so 'mt:Monday' or 'mt:eeaf1f975877a5d'.",
  section_help:
    "Choose an item from the 'Subpages' menu to begin working with this section.",

  statisticsGuidance:
    "This shows some basic statistics.  Note that this page may take a couple of minutes to load completely. Data shown here may be many minutes old.",
  stats_overview: "Statistics Overview",

  stats_byday: "Votes by Day",
  stats_byloc: "Votes by Locale",
  stats_recent: "Recently Submitted Items",

  section_info_Core_Data:
    "The Core Data is vital for proper functioning of each locale. Because changes can disrupt the survey tool, data can only be changed via tickets. Please also review the Plural Rules for your locale: they are also vital.",
  section_info_Locale_Display_Names:
    "The Locale Display Names are used to format names of locales, languages, scripts, and regions (including countries).",
  section_info_DateTime:
    "The Date and Time data is used to format dates and times, including intervals (eg, 'Dec 10-12'). After completing this section, you should review the overall results with Review: Date/Time.",
  section_info_Timezones:
    "The Timezones data is used to display timezones in a variety of ways. They also contain a list of cities associated with timezones. After completing this section, you should review the overall results with Review: Zones.",
  section_info_Numbers:
    "The Numbers data is used to format numbers and currencies, including compact numbers (eg, '3M' for 3,000,000). After completing this section, you should review the overall results with Review: Numbers.",
  section_info_Currencies:
    "The Currencies data is used to format the names of currencies, and also provides the various currency symbols. After completing this section, you should review the overall results with Review: Numbers.",
  section_info_Units:
    "The Units is used for formatting measurements, such as '3 hours' or '4 kg'.",
  section_info_Misc:
    "The Miscellaneous data is used to some special purpose items, such as lists (eg, 'A, B, and C') and truncated strings (eg, 'supercalifrag…cious').",

  survey_title: "CLDR Survey Tool",
  forumNewPostButton: "New Forum Post",
  forumNewButton_desc:
    "Clicking this will bring up a form to reply to this particular item.",
  forumNewPostFlagButton: "Flag for Review",
  forumNewPostFlagButton_desc:
    "Clicking this will bring up a form to reply to this particular item.",

  user_me: "Me",
  users_guidance:
    "[BETA] This is a Users page. <p>Uncheck 'Hide Locked' to include locked users. <p>To 'View Old Vote Stats', click the so-named button. <p>To transfer votes, click 'Transfer Old Votes' on the user you want to transfer votes TO, and give the email address of the user to transfer old votes FROM, the locale to transfer FROM, and the locale to transfer TO. This button will also reset the 'Do you want import old votes?' button for that user. After import, look for the 'result_count': value under the TO user indicating how many votes actually transferred. ",
  users_infoVotesButton: "View Old Vote Stats",
  users_loadVotesButton: "Transfer Old Votes...",

  forum_noposts: "No posts in this forum.",
  forum_item: "Item",
  forum_reply: "Reply",
  forum_msg: "Showing posts for ${forum} and all sublocales.",
  forumGuidance:
    "This page will not reload automatically when new posts come in. You can use your browser's Refresh button to load new posts.",
  forum_prefill_agree: "I agree. I am changing my vote to the requested “${0}”",
  forum_prefill_close: "I'm closing this thread",
  forum_prefill_decline:
    "I decline changing my vote to the requested “${0}”. My reasons are:\n",
  forum_prefill_request: "Please consider voting for “${0}”. My reasons are:\n",
  forum_remember_vote:
    "⚠️ Please remember to vote – submitting a forum post does NOT cause any actual vote to be made.",

  generic_nolocale: "No locale chosen.",
  defaultContent_msg:
    "This locale, ${name} is the <i><a target='CLDR-ST-DOCS' href='https://cldr.unicode.org/translation/translation-guide-general/default-content'>default content locale</a></i> for <b><a class='notselected' href='#/${dcParent}'>${dcParentName}</a></b>, and thus editing or viewing is disabled.",
  defaultContentChild_msg:
    "This locale, ${name}, supplies the <i><a target='CLDR-ST-DOCS' href='https://cldr.unicode.org/translation/translation-guide-general/default-content'>default content</a></i> for <b><a class='notselected' href='#/${dcChild}'>${dcChildName}</a></b>. Please make sure that all the changes that you make here are appropriate for <b>${dcChildName}</b>. If there are multiple acceptable choices, please try to pick the one that would work for the most other sublocales.",
  defaultContent_brief_msg:
    "${name} is a default content locale and may not be edited",
  defaultContent_header_msg: "= ${dcChild}",
  defaultContent_titleLink: "content",
  readonly_msg: "This locale (${locale}) may not be edited.<br/> ${msg}",
  readonly_unknown: "Reason: Administrative Policy.",
  scratch_locale: "Test Locale",
  readonly_in_limited:
    "It is not open for submission during this limited cycle.",
  readonly_in_limited_brief:
    "Not open for submission during this limited cycle.",
  beta_msg:
    "The SurveyTool is currently in Beta. Any data added here will NOT go into CLDR.",
  sidewaysArea_desc: "view of what the votes are in other, sister locales",
  sideways_loading0: " ",
  sideways_loading1: "Comparing to other locales...",
  sideways_same: "Other locales have the same value.",
  sideways_diff: "Other locales have different values!",
  sideways_noValue: "(no value)",

  ari_message: "Problem with the SurveyTool",
  ari_sessiondisconnect_message: "Your session has been disconnected.",
  ari_force_reload: "[Second try: will force page reload]",

  coverage_auto: "Default",
  coverage_auto_msg: "${surveyOrgCov} (Default)",
  coverage_core: "Core",
  coverage_posix: "POSIX",
  coverage_minimal: "Minimal",
  coverage_basic: "Basic",
  coverage_moderate: "Moderate",
  coverage_modern: "Modern",
  coverage_comprehensive: "Comprehensive",
  coverage_optional: "Optional",
  coverage_no_items: "No items at this current coverage level.",
  coverage_menu_desc:
    'Change the displayed coverage level. "Default" will use your organization\'s preferred value for this locale, if any.',
  coverage_unknown: "Unknown",
  coverage_reset_msg: "Coverage Reset",
  coverage_reset_desc:
    "The coverage level was reset to its default after a period of inactivity",

  section_mail: "Messages",

  flaggedGuidance:
    "This shows a list of items which are flagged for TC review. Items are sorted by locale and then date. ",
  flaggedTotalCount: "Total: ",
  vsummaryGuidance:
    "This is the vetting summary. Click Recalculate to start and be patient, this may take a while.",
  vsReload: "Recalculate",
  vsStop: "Stop",
  vsContent_initial: "Click Recalculate to calculate the summary",

  forum_participationGuidance: "This is the Forum Participation page.",
  forum_participation_TOTAL: "Posts in this release",
  forum_participation_ORG: "Posts by my org.",
  forum_participation_REQUEST: "Open Requests",
  forum_participation_DISCUSS: "Open Discussions",
  forum_participation_ACT: "Needing action",

  vetting_participationGuidance:
    "This is the Vetting Participation page. Specifically assigned vetters are marked in <b>bold</b>. Asterisk (*) denotes users who may vote in any locale.",

  bulk_close_postsGuidance:
    "This is the Forum Bulk Close Posts page. The results may take several minutes to load.",

  jsonStatus_msg:
    "You should see your content shortly, thank you for waiting. By the way, there are ${users} logged-in users and ${observers} visitors to the Survey Tool. The server's workload is about ${sysloadpct} of normal capacity. You have been waiting about ${waitTime} seconds.",
  err_what_section: "load part of this locale",
  err_what_locmap: "load the list of locales",
  err_what_menus: "load the Survey Tool menus",
  err_what_status: "get the latest status from the server",
  err_what_unknown: "process your request",
  err_what_oldvotes: "fetch or import your old votes",
  err_what_vote: "vote for a value",
  E_UNKNOWN:
    "An error occurred while trying to '${what}', and the error code is '${code}'.\n Reloading may resume your progress.",
  E_INTERNAL:
    "An internal error occurred trying to '${what}'. This is probably a bug in the SurveyTool.",
  E_BAD_SECTION:
    "An error occurred while trying to ${what}, the server could not find what was requested. \nPerhaps the URL is incorrect?",
  E_BAD_LOCALE:
    "The locale, '${surveyCurrentLocale}',\n does not exist. It was either mistyped or has not been added to the Survey Tool.",
  E_NOT_STARTED:
    "The SurveyTool is still starting up. Please wait a bit and hit Reload.",
  E_SPECIAL_SECTION:
    "An error occurred while trying to ${what}.\nThe XPath “${surveyCurrentId}” in locale “${surveyCurrentLocale}” is not visible in the SurveyTool.\nPerhaps the URL is incorrect or an item was deprecated?",
  E_SESSION_DISCONNECTED:
    "Your session has timed out or the SurveyTool has restarted. To continue from where you were, hit Reload.",
  E_DISCONNECTED:
    "You were disconnected from the SurveyTool. To reconnect, hit Reload.",
  E_NO_PERMISSION: "You do not have permission to do that operation.",
  E_NO_OLD_VOTES:
    "Error: Old votes submitted in the former version are not available.",
  E_NOT_LOGGED_IN: "That operation cannot be done without being logged in.",
  E_BAD_VALUE: "The vote was not accepted: ${message}",
  E_BAD_XPATH: "This item does not exist in this locale.",

  TRANS_HINT_LANGUAGE_NAME: "English", // must match SurveyMain.TRANS_HINT_LANGUAGE_NAME

  report_acceptable: "Acceptable",
  report_notAcceptable: "Not Acceptable",
  report_missing: "Missing",

  special_about: "About Survey Tool",
  special_announcements: "Announcements",
  special_account: "Account Settings",
  special_admin: "Admin Panel",
  special_add_user: "Add a Survey Tool user",
  special_auto_import: "Import Old Winning Votes",
  special_bulk_close_posts: "Bulk Close Posts",
  special_createAndLogin: "Create and Login",
  special_default: "Missing Page",
  special_dashboard: "Dashboard",
  special_error_subtypes: "Error Subtypes",
  special_flagged: "Flagged Items",
  special_forum: "Forum Posts",
  special_forum_participation: "Forum Participation",
  special_general: "General Info",
  special_list_emails: "List Email Addresses",
  special_list_users: "List Users",
  special_locales: "Locale List",
  special_lock_account: "Lock (Disable) My Account",
  special_lookup: "Look up a code or xpath",
  special_mail: "Notifications (SMOKETEST ONLY)",
  special_menu: "☰",
  special_oldvotes: "Import Old Votes",
  // The special_r_* are the names of reports, used by ReportResponse.vue and others
  special_r_compact: "Numbers",
  special_r_datetime: "Datetime",
  special_r_zones: "Zones",
  special_r_personnames: "Person Names",
  special_recent_activity: "Recent Activity",
  special_retry: "Retry",
  special_retry_inplace: "Retry",
  special_search: "Search",
  special_statistics: "Statistics",
  special_transfervotes: "Copy Old Votes Tool",
  special_users: "Users",
  special_vetting_participation: "Vetting Participation",
  special_vetting_participation2: "Vetting Participation v2",
  special_vsummary: "Priority Items Summary",
  special_test_panel: "Experimental Test Panel",

  lock_account_admin: "The Admin account cannot be locked.",
  lock_account_login:
    "To lock (unsubscribe and disable) your account, first log in. You may request a password reset if needed.",
  lock_account_caution:
    "You are requesting to lock (disable) your Survey Tool account permanently. All of your votes will be ignored. Are you absolutely sure you wish to do this?",
  lock_account_instruction:
    "To <em>permanently disable</em> your account, please fill in the following information, then press the button below.",
  lock_account_sum: "The sum of ${0} and ${1}",
  lock_account_email: "Your E-mail address",
  lock_account_reason: "The reason for the account lock request",
  lock_account_button: "Permanently lock my account",
  lock_account_err_email: "The E-mail address must match exactly.",
  lock_account_err_math: "Sorry, that answer to the math question was wrong.",
  lock_account_err_reason: "The reason for the request must be filled in.",
  lock_account_success:
    "The account has been locked successfully. Thank you for using the Survey Tool. If you have difficulty still, contact the person who set up your account.",

  notification_category_abstained:
    "You have abstained, or not yet voted for any value.",
  notification_category_changed:
    "The winning value was altered from the baseline value. (Informational)",
  notification_category_disputed:
    "Different organizations are choosing different values. Please review to approve or reach consensus.",
  notification_category_english_changed:
    "The English value has changed in CLDR, but the corresponding value for your language has not. " +
    "Check if any changes are needed in your language.",
  notification_category_error:
    "The Survey Tool detected an error in the winning value.",
  notification_category_inherited_changed:
    "The winning inherited value was altered from its baseline value. (Informational)",
  notification_category_losing:
    "The value that your organization chose (overall) is either not the winning value, or doesn’t have enough votes to be approved. " +
    "This might be due to a dispute between members of your organization.",
  notification_category_missing:
    "Your current coverage level requires the item to be present. " +
    "(During the vetting phase, this is informational: you can’t add new values.)",
  notification_category_provisional:
    "There are not enough votes for this item to be approved (and used).",
  notification_category_warning:
    "The Survey Tool detected a warning about the winning value.",
  notification_category_reports: "A Report has not been completed.",

  progress_page: "Your voting in this page",
  progress_voter: "Your voting in this locale",
  progress_voter_disabled:
    "This meter will be enabled if you open the Dashboard",
  progress_all_vetters: "Completion for all vetters in this locale",
  progress_negative:
    "A negative numerator indicates new problems not in baseline",

  summary_help: "The following summarizes the Priority Items across locales.",
  summary_coverage_org_specific:
    "The coverage level for each locale is specific to your organization.",
  summary_coverage_neutral:
    "This chart is an overall summary, and does not reflect organization-specific data. The overall progress is based on the Error+Missing+Provisional value to zero; once that is done the progress will read at 100%.",
  summary_access_denied:
    "To see the summary, you must be logged in as a manager, TC, or admin.",
  summary_snapshot_hover:
    "Show the Priority Items Summary snapshot with id “${0}” [${1}]",

  // LocaleNormalizer.LocaleRejection
  locale_rejection_unknown: "Unknown or not in CLDR",
  locale_rejection_outside_org_coverage: "Outside of org’s coverage",

  transcript_flyover: "Explain vote counts",
  transcript_note:
    "PREVIEW FEATURE: The above is an attempt to explain votes. Feedback to https://unicode-org.atlassian.net/browse/CLDR-14943",

  empty_comparison_cell_hint: "not available in English; see the Info Panel",
};

/**
 * Get the string for the given key
 *
 * @param k the key
 * @return the string for the given key, if the key exists in the map; otherwise, return the key itself
 */
function get(k) {
  if (k in strings) {
    return strings[k];
  }
  if (CLDR_TEXT_DEBUG) {
    console.log("get: missing string for k = " + k);
  }
  return k;
}

/**
 * Substitute the placeholders in the template for the given key using the given map
 *
 * @param k the key for the template
 * @param map an array like ['a', 'b'] or an object like {a: 'A', b: 'B'}
 * @return the string with substitutions made, or an empty string for failure
 */
function sub(k, map) {
  const template = get(k);
  return subTemplate(template, map);
}

/**
 * Substitute the placeholders in the template for the given key using the given map
 *
 * @param template template string
 * @param map an array like ['a', 'b'] or an object like {a: 'A', b: 'B'}
 * @return the string with substitutions made, or an empty string for failure
 */
function subTemplate(template, map) {
  if (template) {
    if (map instanceof Array) {
      return template.replace(/\${(\d)}/g, (blank, i) => map[i]);
    }
    if (map instanceof Object) {
      return template.replace(/\${([^}]+)}/g, (blank, i) => map[i]);
    }
  }
  return "";
}

export { get, sub, subTemplate };
