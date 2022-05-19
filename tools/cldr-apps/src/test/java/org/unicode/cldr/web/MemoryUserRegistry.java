package org.unicode.cldr.web;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.VoteResolver.VoterInfo;

public class MemoryUserRegistry extends UserRegistry {

    Map<Integer, User> info = new ConcurrentHashMap<>();

    @Override
    public Set<String> getCovGroupsForOrg(String st_org) {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    @Override
    public Set<CLDRLocale> anyVotesForOrg(String st_org) {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    @Override
    public UserSettingsData getUserSettings() {
        return null; // do not use
    }

    @Override
    public UserSettings getSettings(int id) {
        return new UserSettings() {

            @Override
            public int compareTo(UserSettings o) {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public String get(String name, String defaultValue) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void set(String name, String value) {
                // TODO Auto-generated method stub
            }

        };
    }

    @Override
    public User getInfo(int id) {
        return info.get(id);
    }

    @Override
    public User get(String pass, String email, String ip, boolean letmein) throws LogoutException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String setUserLevel(User me, User them, int newLevel) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String setLocales(CookieSession session, User user, String newLocales, boolean intLocs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String delete(WebContext ctx, int theirId, String theirEmail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    String updateInfo(WebContext ctx, int theirId, String theirEmail, InfoType type, String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String resetPassword(String forEmail, String ip) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lockAccount(String forEmail, String reason, String ip) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPassword(WebContext ctx, int theirId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User newUser(WebContext ctx, User u) {
        info.put(u.id, u);
        return u;
    }

    @Override
    public Map<Integer, VoterInfo> getVoterToInfo() {
        // TODO Auto-generated method stub
        return Collections.emptyMap();
    }

    @Override
    public String[] getOrgList() {
        // TODO Auto-generated method stub
        return new String[0];
    }

    @Override
    protected Set<User> getAnonymousUsersFromDb() {
        // TODO Auto-generated method stub
        return Collections.emptySet();
    }

    @Override
    protected void createAnonymousUsers(int existingCount, int desiredCount) {
        // TODO Auto-generated method stub

    }
}
