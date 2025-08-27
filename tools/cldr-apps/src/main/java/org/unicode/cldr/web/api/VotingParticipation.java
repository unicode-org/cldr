package org.unicode.cldr.web.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.UserRegistry;

@Path("/voting/participation")
@Tag(
        name = "voting_participation",
        description = "APIs for voting and retrieving vote participation")
public class VotingParticipation {
    Logger logger = SurveyLog.forClass(VotingParticipation.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get voting participation summary",
            description = "Get voting participation summary")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Vote participation results",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                VotingParticipationResults.class))),
                @APIResponse(
                        responseCode = "401",
                        description = "Authorization required, send a valid session id"),
                @APIResponse(
                        responseCode = "403",
                        description = "Forbidden, no access to this data"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response getParticipation(@HeaderParam(Auth.SESSION_HEADER) String session) {
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        if (mySession.user == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }
        VotingParticipationResults results = new VotingParticipationResults();
        if (!UserRegistry.userIsAdmin(mySession.user)) {
            // Restrict results to one organization
            results.onlyOrg = mySession.user.getOrganization();

            if (CLDRConfig.getInstance().getEnvironment() == CLDRConfig.Environment.PRODUCTION) {
                // This is a slow operation (querying all rows of votes in a dozen tables)
                // so, for now, restrict it to admins only. It's only available in the
                // admin UI at present.
                return Response.status(Status.FORBIDDEN).build();
            }
        }
        logger.info(
                "Getting full vetting participation (slow), requested by "
                        + mySession.user.toString());
        DBUtils dbUtils = DBUtils.getInstance();
        for (final Organization o : Organization.values()) {
            if (results.onlyOrg != null && (results.onlyOrg != o)) continue;
            OrgParticipation p = new OrgParticipation(o);
            results.results.add(p);
            UserRegistry reg = CookieSession.sm.reg;
            try (Connection conn = dbUtils.getAConnection();
                    PreparedStatement userListStatement = reg.list(o.name(), conn);
                    ResultSet userList = userListStatement.executeQuery(); ) {
                // get all vetters
                final Set<String> localesWithVetters = new TreeSet<String>();
                while (userList.next()) {
                    int theirId = userList.getInt("id");
                    appendInterestLocales(reg, localesWithVetters, theirId);
                }
                p.localesWithVetters = localesWithVetters.toArray(new String[0]);
            } catch (SQLException sqe) {
                SurveyLog.logException(logger, sqe, "while checking voting participation for " + o);
                return new STError(sqe).build();
            }
        }

        // add per-version
        try (Connection conn = dbUtils.getAConnection(); ) {
            Collection<String> tables =
                    DBUtils.getTables(
                            conn,
                            Pattern.compile("cldr_vote_value_[0-9]+$").asPredicate()); // skip beta
            for (String table : tables) {
                final String ver = "v" + table.substring(table.lastIndexOf('_') + 1);
                SurveyParticipation participation = new SurveyParticipation();
                results.participationByVersion.put(ver, participation);
                try (PreparedStatement psParticipation =
                                conn.prepareStatement(
                                        "SELECT count(v.xpath) as count, v.locale as locale, u.org as org \n"
                                                + "    FROM "
                                                + table
                                                + " as v, cldr_users as u\n"
                                                + "    WHERE v.submitter = u.id\n"
                                                + " group by u.org, v.locale",
                                        ResultSet.TYPE_FORWARD_ONLY,
                                        ResultSet.CONCUR_READ_ONLY);
                        ResultSet rs = psParticipation.executeQuery(); ) {
                    while (rs.next()) {
                        final int count = rs.getInt("count");
                        final String locale = rs.getString("locale");
                        final String org = rs.getString("org");
                        participation.put(count, locale, org);
                    }
                    logger.info(
                            "Got full vetter info for "
                                    + ver
                                    + "- locales: "
                                    + participation.allParticipationByLocale.size()
                                    + "- orgs: "
                                    + participation.allParticipationByOrgLocale.size());
                }
            }
        } catch (SQLException sqe) {
            SurveyLog.logException(logger, sqe, "while getting versioned participation");
            return new STError(sqe).build();
        }

        return Response.ok(results).build();
    }

    private void appendInterestLocales(
            UserRegistry reg, final Set<String> localesWithVetters, int theirId) {
        final UserRegistry.User theUser = reg.getInfo(theirId);
        if (UserRegistry.userIsLocked(theUser)) {
            return; // skip locked users
        }
        LocaleSet interestLocales = theUser.getInterestLocales();
        if (!interestLocales.isAllLocales()) {
            final Set<CLDRLocale> intSet = interestLocales.getSet();
            if (intSet != null) {
                for (final CLDRLocale l : intSet) {
                    localesWithVetters.add(l.getBaseName());
                }
            }
        }
    }

    public static class VotingParticipationResults {
        public Organization onlyOrg = null;
        private Vector<OrgParticipation> results = new Vector<>();

        public OrgParticipation[] getResults() {
            return results.toArray(new OrgParticipation[0]);
        }

        public Map<String, SurveyParticipation> participationByVersion =
                new TreeMap<String, SurveyParticipation>();
    }

    public static class SurveyParticipation {
        public Map<String, Integer> allParticipationByLocale = new TreeMap<>();
        public Map<String, Map<String, Integer>> allParticipationByOrgLocale = new TreeMap<>();

        public void put(int count, String locale, String org) {
            // Cumulative count for this locale
            int allCount = count + allParticipationByLocale.computeIfAbsent(locale, ignored -> 0);
            allParticipationByLocale.put(locale, allCount);

            // Set the participation for this org, locale
            allParticipationByOrgLocale
                    .computeIfAbsent(org, ignored -> new TreeMap<String, Integer>())
                    .put(locale, count);
        }
    }

    public static class OrgParticipation {
        public String[] localesWithVetters;
        public final String[] coverageLocales;
        public String defaultCoverage;

        public OrgParticipation(Organization o) {
            this.org = o;
            StandardCodes codes = StandardCodes.make();
            this.defaultCoverage = codes.getDefaultLocaleCoverageLevel(o).toString();
            coverageLocales = codes.getLocaleCoverageLocales(o).toArray(new String[0]);
        }

        public Organization org;
    }
}
