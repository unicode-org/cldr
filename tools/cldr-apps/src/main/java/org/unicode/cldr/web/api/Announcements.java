package org.unicode.cldr.web.api;

import java.util.*;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.LocaleNormalizer;
import org.unicode.cldr.util.LocaleSet;
import org.unicode.cldr.web.*;

@ApplicationScoped
@Path("/announce")
@Tag(name = "announce", description = "APIs for Survey Tool announcements")
public class Announcements {
    public static final String AUDIENCE_TC = "TC";
    public static final String AUDIENCE_MANAGERS = "Managers";
    public static final String AUDIENCE_VETTERS = "Vetters";
    public static final String AUDIENCE_EVERYONE = "Everyone";

    public static final String ORGS_MINE = "Mine";
    public static final String ORGS_TC = "TC";
    public static final String ORGS_ALL = "All";

    private static final Set<String> validAudiences =
            new HashSet<>(
                    Arrays.asList(
                            AUDIENCE_TC, AUDIENCE_MANAGERS, AUDIENCE_VETTERS, AUDIENCE_EVERYONE));
    private static final Set<String> validOrgs =
            new HashSet<>(Arrays.asList(ORGS_MINE, ORGS_TC, ORGS_ALL));

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get announcements", description = "Get announcements")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Announcements",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                AnnouncementResponse.class))),
                @APIResponse(responseCode = "503", description = "Not ready yet"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response getAnnouncements(@HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsGuest(session.user)) { // userIsGuest means "is guest or stronger"
            return Response.status(403, "Forbidden").build();
        }
        session.userDidAction();
        if (SurveyMain.isBusted() || !SurveyMain.wasInitCalled() || !SurveyMain.triedToStartUp()) {
            return STError.surveyNotQuiteReady();
        }
        AnnouncementResponse response = new AnnouncementResponse(session.user);
        return Response.ok(response).build();
    }

    @Schema(description = "List of announcements")
    public static final class AnnouncementResponse {
        @Schema(description = "announcements")
        public Announcement[] announcements;

        public AnnouncementResponse(UserRegistry.User user) {
            List<Announcement> announcementList = new ArrayList<>();
            AnnouncementData.get(user, announcementList);
            announcements = announcementList.toArray(new Announcement[0]);
        }
    }

    @Schema(description = "Single announcement")
    public static class Announcement {
        @Schema(description = "announcement id as stored in database")
        public int id;

        @Schema(description = "poster id")
        public int poster;

        @Schema(description = "poster name")
        public String posterName;

        @Schema(description = "date")
        public String date;

        @Schema(description = "subject")
        public String subject;

        @Schema(description = "body")
        public String body;

        @Schema(description = "checked")
        public boolean checked = false;

        @Schema(description = "locales")
        public String locs = null;

        @Schema(description = "orgs")
        public String orgs = null;

        @Schema(description = "audience")
        public String audience = null;

        public Announcement(int id, int poster, String date, String subject, String body) {
            this.id = id;
            this.poster = poster;
            UserRegistry.User posterUser = CookieSession.sm.reg.getInfo(poster);
            this.posterName = (posterUser != null) ? posterUser.name : Integer.toString(id);
            this.date = date;
            this.subject = subject;
            this.body = body;
        }

        public void setFilters(String locales, String orgs, String audience) {
            this.locs = locales;
            this.orgs = orgs;
            this.audience = audience;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Submit an announcement", description = "Submit an announcement")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Announcement submitted (but check result status)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Response.class))),
                @APIResponse(responseCode = "400", description = "Bad request"),
                @APIResponse(
                        responseCode = "401",
                        description = "Authorization required, send a valid session id"),
                @APIResponse(responseCode = "403", description = "Forbidden, no access"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response submitAnnouncement(
            @HeaderParam(Auth.SESSION_HEADER) String sessionString, SubmissionRequest request) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!request.isValid()) {
            return Response.status(400, "Bad request").build();
        }
        // Only TC or Admin can specify orgs other than ORGS_MINE
        if (!UserRegistry.userIsManagerOrStronger(session.user)
                || (!ORGS_MINE.equals(request.orgs)
                        && !UserRegistry.userIsTC(session.user))) { // userIsTC means TC or stronger
            return Response.status(403, "Forbidden").build();
        }
        final AnnouncementSubmissionResponse response = new AnnouncementSubmissionResponse();
        try {
            request.normalize();
            AnnouncementData.submit(request, response, session.user);
        } catch (SurveyException e) {
            throw new RuntimeException(e);
        }
        return Response.ok().entity(response).build();
    }

    public static class SubmissionRequest {
        /**
         * A constructor without parameters prevents serialization error, "No default constructor
         * found"
         */
        public SubmissionRequest() {
            this.subject = "";
            this.body = "";
        }

        @Schema(description = "subject")
        public String subject;

        @Schema(description = "body")
        public String body;

        @Schema(description = "audience")
        public String audience = null;

        @Schema(description = "locales")
        public String locs = null;

        @Schema(description = "orgs")
        public String orgs = null;

        public boolean isValid() {
            return validAudiences.contains(audience) && validOrgs.contains(orgs);
        }

        public void normalize() {
            if (locs != null) {
                String normalized = LocaleNormalizer.normalizeQuietly(locs);
                LocaleSet locSet = LocaleNormalizer.setFromStringQuietly(normalized, null);
                LocaleSet langSet = locSet.combineRegionalVariants();
                locs = langSet.toString();
            }
        }
    }

    public static class AnnouncementSubmissionResponse {

        @Schema(description = "ok, true if successful")
        public boolean ok = false;

        @Schema(description = "error message, empty if successful")
        public String err = "";

        @Schema(description = "id of newly created announcement, zero if none")
        public int id = 0;
    }

    @POST
    @Path("/checkread")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Set already-read status",
            description = "Indicate whether an announcement has been read")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Submitted",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = Response.class))),
                @APIResponse(
                        responseCode = "401",
                        description = "Authorization required, send a valid session id"),
                @APIResponse(responseCode = "403", description = "Forbidden, no access"),
                @APIResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = STError.class))),
            })
    public Response checkRead(
            @HeaderParam(Auth.SESSION_HEADER) String sessionString, CheckReadRequest request) {
        CookieSession session = Auth.getSession(sessionString);
        if (session == null) {
            return Auth.noSessionResponse();
        }
        if (!UserRegistry.userIsGuest(session.user)) { // means guest or stronger
            return Response.status(403, "Forbidden").build();
        }
        final CheckReadResponse response = new CheckReadResponse();
        AnnouncementData.checkRead(request.id, request.checked, response, session.user);
        return Response.ok().entity(response).build();
    }

    public static class CheckReadRequest {

        /**
         * A constructor without parameters prevents serialization error, "No default constructor
         * found"
         */
        public CheckReadRequest() {
            this.id = 0;
            this.checked = false;
        }

        @Schema(description = "id")
        public int id;

        @Schema(description = "checked")
        public boolean checked;
    }

    public static class CheckReadResponse {

        @Schema(description = "ok")
        public boolean ok = false;

        @Schema(description = "err")
        public String err = "";
    }
}
