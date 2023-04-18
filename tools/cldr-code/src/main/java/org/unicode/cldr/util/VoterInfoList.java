package org.unicode.cldr.util;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.VoteResolver.Level;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

public class VoterInfoList {
    /** Create a VoterInfoList with no users */
    public VoterInfoList() {
        clearVoterToInfo();
    }

    /** Static info read from file */
    private Map<Integer, VoterInfo> voterToInfo;

    private Map<String, Map<Organization, Level>> localeToOrganizationToMaxVote;

    synchronized Map<Integer, VoterInfo> getVoterToInfo() {
        return voterToInfo;
    }

    /** Clear out all users. */
    VoterInfoList clearVoterToInfo() {
        setVoterToInfo(Collections.emptyMap());
        return this;
    }

    public VoterInfo getInfoForVoter(int voter) {
        return getVoterToInfo().get(voter);
    }

    /**
     * Set the voter info.
     *
     * <p>Synchronized, however, once this is called, you must NOT change the contents of your copy
     * of newVoterToInfo. You can create a whole new one and set it.
     */
    public VoterInfoList setVoterToInfo(Map<Integer, VoterInfo> newVoterToInfo) {
        computeMaxVotesAndSet(newVoterToInfo);
        return this;
    }

    /** Set the voter info from a users.xml file. */
    public VoterInfoList setVoterToInfo(String fileName) {
        MyHandler myHandler = new MyHandler();
        XMLFileReader xfr = new XMLFileReader().setHandler(myHandler);
        xfr.read(fileName, XMLFileReader.CONTENT_HANDLER | XMLFileReader.ERROR_HANDLER, false);
        return setVoterToInfo(myHandler.testVoterToInfo);
    }

    private void computeMaxVotesAndSet(Map<Integer, VoterInfo> newVoterToInfo) {
        // compute the localeToOrganizationToMaxVote
        Map<String, Map<Organization, Level>> newLocaleToOrganizationToMaxVote = new TreeMap<>();
        for (int voter : newVoterToInfo.keySet()) {
            VoterInfo info = newVoterToInfo.get(voter);
            if (info.getLevel() == Level.tc || info.getLevel() == Level.locked) {
                continue; // skip TCs, locked
            }

            for (CLDRLocale loc : info.getLocales()) {
                String locale = loc.getBaseName();
                Map<Organization, Level> organizationToMaxVote =
                        newLocaleToOrganizationToMaxVote.get(locale);
                if (organizationToMaxVote == null) {
                    newLocaleToOrganizationToMaxVote.put(
                            locale, organizationToMaxVote = new TreeMap<>());
                }
                Level maxVote = organizationToMaxVote.get(info.getOrganization());
                if (maxVote == null || info.getLevel().compareTo(maxVote) > 0) {
                    organizationToMaxVote.put(info.getOrganization(), info.getLevel());
                    // System.out.println("Example best voter for " + locale + " for " +
                    // info.organization + " is " +
                    // info);
                }
            }
        }
        // setters
        synchronized (this) {
            CldrUtility.protectCollection(newLocaleToOrganizationToMaxVote);
            localeToOrganizationToMaxVote = newLocaleToOrganizationToMaxVote;
            voterToInfo = Collections.unmodifiableMap(newVoterToInfo);
        }
    }

    /**
     * Handles fine in xml format, turning into:
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/level[@n="1"][@type="TC"]
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/name
     * Mike Tardif
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/org
     * Adobe
     * //users[@host="sarasvati.unicode.org"]/user[@id="286"][@email="mike.tardif@adobe.com"]/locales[@type="edit"]
     *
     * <p>Steven's new format: //users[@generated="Wed May 07 15:57:15 PDT
     * 2008"][@host="tintin"][@obscured="true"] /user[@id="286"][@email="?@??.??"]
     * /level[@n="1"][@type="TC"]
     */
    static class MyHandler extends XMLFileReader.SimpleHandler {
        private static final Pattern userPathMatcher =
                Pattern.compile(
                        "//users(?:[^/]*)"
                                + "/user\\[@id=\"([^\"]*)\"](?:[^/]*)"
                                + "/("
                                + "org"
                                + "|name"
                                + "|level\\[@n=\"([^\"]*)\"]\\[@type=\"([^\"]*)\"]"
                                + "|locales\\[@type=\"([^\"]*)\"]"
                                + "(?:/locale\\[@id=\"([^\"]*)\"])?"
                                + ")",
                        Pattern.COMMENTS);

        enum Group {
            all,
            userId,
            mainType,
            n,
            levelType,
            localeType,
            localeId;

            String get(Matcher matcher) {
                return matcher.group(this.ordinal());
            }
        }

        private static final boolean DEBUG_HANDLER = false;
        Map<Integer, VoterInfo> testVoterToInfo = new TreeMap<>();
        Matcher matcher = userPathMatcher.matcher("");

        @Override
        public void handlePathValue(String path, String value) {
            if (DEBUG_HANDLER) System.out.println(path + "\t" + value);
            if (matcher.reset(path).matches()) {
                if (DEBUG_HANDLER) {
                    for (int i = 1; i <= matcher.groupCount(); ++i) {
                        Group group = Group.values()[i];
                        System.out.println(i + "\t" + group + "\t" + group.get(matcher));
                    }
                }
                int id = Integer.parseInt(Group.userId.get(matcher));
                VoterInfo voterInfo = testVoterToInfo.get(id);
                if (voterInfo == null) {
                    testVoterToInfo.put(id, voterInfo = new VoterInfo());
                }
                final String mainType = Group.mainType.get(matcher);
                if (mainType.equals("org")) {
                    Organization org = Organization.fromString(value);
                    voterInfo.setOrganization(org);
                    value = org.name(); // copy name back into value
                } else if (mainType.equals("name")) {
                    voterInfo.setName(value);
                } else if (mainType.startsWith("level")) {
                    String level = Group.levelType.get(matcher).toLowerCase();
                    voterInfo.setLevel(Level.valueOf(level));
                } else if (mainType.startsWith("locale")) {
                    final String localeIdString = Group.localeId.get(matcher);
                    if (localeIdString != null) {
                        CLDRLocale locale = CLDRLocale.getInstance(localeIdString.split("_")[0]);
                        voterInfo.addLocale(locale);
                    } else if (DEBUG_HANDLER) {
                        System.out.println("\tskipping");
                    }
                } else if (DEBUG_HANDLER) {
                    System.out.println("\tFailed match* with " + path + "=" + value);
                }
            } else {
                System.out.println("\tFailed match with " + path + "=" + value);
            }
        }
    }

    public Map<Organization, Level> getOrganizationToMaxVote(String locale) {
        locale = locale.split("_")[0]; // take base language
        Map<Organization, Level> result = localeToOrganizationToMaxVote.get(locale);
        if (result == null) {
            result = Collections.emptyMap();
        }
        return result;
    }

    public Map<Organization, Level> getOrganizationToMaxVote(Set<Integer> voters) {
        Map<Organization, Level> orgToMaxVoteHere = new TreeMap<>();
        for (int voter : voters) {
            VoterInfo info = getInfoForVoter(voter);
            if (info == null) {
                continue; // skip unknown voter
            }
            Level maxVote = orgToMaxVoteHere.get(info.getOrganization());
            if (maxVote == null || info.getLevel().compareTo(maxVote) > 0) {
                orgToMaxVoteHere.put(info.getOrganization(), info.getLevel());
                // System.out.println("*Best voter for " + info.organization + " is " + info);
            }
        }
        return orgToMaxVoteHere;
    }

    public VoterInfo get(int voter) {
        return voterToInfo.get(voter);
    }
}
