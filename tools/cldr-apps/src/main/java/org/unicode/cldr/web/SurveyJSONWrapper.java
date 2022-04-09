package org.unicode.cldr.web;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.unicode.cldr.test.CheckCLDR;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.CheckStatus.Subtype;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.SurveyException.ErrorCode;

/**
 * Consolidate my JSONify functions here.
 *
 * @author srl
 *
 * This is not org.json.JSONWriter
 */
public final class SurveyJSONWrapper {
    private final JSONObject j = new JSONObject();

    public SurveyJSONWrapper() {
    }

    public final void put(String k, Object v) {
        try {
            j.put(k, v);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e.toString(), e);
        }
    }

    @Override
    public final String toString() {
        return j.toString();
    }

    /**
     * Converts this CheckStatus to JSON.
     * @param status
     * @return
     * @throws JSONException
     */
    public static JSONObject wrap(CheckStatus status) throws JSONException {
        final CheckStatus cs = status;
        return new JSONObject() {
            {
                put("message", cs.getMessage());
                put("type", cs.getType());
                if (cs.getCause() != null) {
                    put("cause", wrap(cs.getCause()));
                }
                Subtype subtype = cs.getSubtype();
                if (subtype != null) {
                    // in json, subtype is like "missingPlaceholders" NOT "missing placeholders"
                    // so use name() not toString() -- this is consistent with SurveyAjax.java (2022-03-07)
                    put("subtype", subtype.name());
                    put("subtypeUrl", SubtypeToURLMap.forSubtype(subtype)); // could be null.
                }
            }
        };
    }

    /**
     * Wrap information about the given user into a JSONObject.
     *
     * @param u the user
     * @return the JSONObject
     * @throws JSONException
     *
     * This function threw NullPointerException for u == null from sm.reg.getInfo(poster),
     * now fixed in SurveyForum.java. Maybe this function should check for u == null.
     * TODO: remove this in favor of jax-rs serialization
     */
    public static JSONObject wrap(UserRegistry.User u) throws JSONException {
        return new JSONObject().put("id", u.id).put("email", u.email).put("name", u.name).put("userlevel", u.userlevel)
            .put("emailHash", u.getEmailHash())
            .put("userlevelName", u.getLevel()).put("org", u.org).put("time", u.last_connect);
    }

    public static JSONObject wrap(CheckCLDR check) throws JSONException {
        final CheckCLDR cc = check;
        return new JSONObject() {
            {
                put("class", cc.getClass().getSimpleName());
                put("phase", cc.getPhase());
            }
        };
    }

    public static JSONObject wrap(ResultSet rs) throws SQLException, IOException, JSONException {
        return DBUtils.getJSON(rs);
    }

    public static List<Object> wrap(List<CheckStatus> list) throws JSONException {
        if (list == null || list.isEmpty())
            return null;
        List<Object> newList = new ArrayList<>();
        for (final CheckStatus cs : list) {
            newList.add(wrap(cs));
        }
        return newList;
    }

    public static JSONObject wrap(final VoteResolver<String> r) throws JSONException {
        JSONObject ret = new JSONObject()
            .put("raw", r.toString()) /* "raw" is only used for debugging (stdebug_enabled) */
            .put("requiredVotes", r.getRequiredVotes());

        EnumSet<Organization> conflictedOrgs = r.getConflictedOrganizations();

        Map<String, Long> valueToVote = r.getResolvedVoteCounts();

        JSONObject orgs = new JSONObject();
        for (Organization o : Organization.values()) {
            String orgVote = r.getOrgVote(o);
            if (orgVote == null)
                continue;
            Map<String, Long> votes = r.getOrgToVotes(o);

            JSONObject org = new JSONObject();
            org.put("status", r.getStatusForOrganization(o));
            org.put("orgVote", orgVote);
            org.put("votes", votes);
            if (conflictedOrgs.contains(org)) {
                org.put("conflicted", true);
            }
            orgs.put(o.name(), org);
        }
        ret.put("orgs", orgs);
        JSONArray valueToVoteA = new JSONArray();
        for (Map.Entry<String, Long> e : valueToVote.entrySet()) {
            valueToVoteA.put(e.getKey()).put(e.getValue());
        }
        ret.put("valueIsLocked", r.isValueLocked());
        ret.put("value_vote", valueToVoteA);
        ret.put("nameTime", r.getNameTime());
        return ret;
    }

    public static JSONObject wrap(PathHeader pathHeader) throws JSONException {
        if (pathHeader == null) return null;
        return new JSONObject().put("section", pathHeader.getSectionId().name())
            .put("page", pathHeader.getPageId().name())
            .put("header", pathHeader.getCode())
            .put("code", pathHeader.getCode())
            .put("str", pathHeader.toString());
    }

    public static void putException(SurveyJSONWrapper r, Throwable t) {
        r.put("err", "Exception: " + t.toString());
        if (t instanceof SurveyException) {
            SurveyException se = (SurveyException) t;
            r.put("err_code", se.getErrCode());
            try {
                se.addDataTo(r);
            } catch (JSONException e) {
                r.put("err_data", e.toString());
            }
        } else {
            r.put("err_code", ErrorCode.E_INTERNAL);
        }
    }

    public static Object wrap(Collection<CLDRLocale> allLanguages) {
        JSONArray a = new JSONArray();
        for(final CLDRLocale l : allLanguages) {
            a.put(wrap(l));
        }
        return a;
    }

    private static String wrap(CLDRLocale l) {
        return l.getBaseName();
    }
}