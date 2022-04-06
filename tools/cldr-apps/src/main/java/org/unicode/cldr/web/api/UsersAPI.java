package org.unicode.cldr.web.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.DBUtils;
import org.unicode.cldr.web.SurveyLog;
import org.unicode.cldr.web.UserRegistry;

@Path("/users")
@Tag(name = "user", description = "APIs for user management")
public class UsersAPI {
    private static final Logger logger = SurveyLog.forClass(UsersAPI.class);
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List Users",
        description = "List users, optionally filtered by organization")
    @APIResponses(
        value = {
            @APIResponse(
                responseCode = "200",
                description = "User list",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserList.class))
            )
        }
    )
    public Response listUsers(
            @HeaderParam(Auth.SESSION_HEADER) String session,
            @QueryParam("org") @Schema(required = false, description = "Organization Filter (ignored unless admin)", example = "Guest") String org,
            @QueryParam("includeLocked") @Schema(required = false, description = "True to include locked users", defaultValue = "false", example = "false") boolean includeLocked
        ) {
        // boilerplate: login
        final CookieSession mySession = Auth.getSession(session);
        if (mySession == null) {
            return Auth.noSessionResponse();
        }
        if (mySession.user == null || !UserRegistry.userCanCreateUsers(mySession.user)) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        if (!UserRegistry.userIsAdmin(mySession.user)) {
            // non-admins can only list their own org
            org = mySession.user.org;
        }
        // collect list of ids
        List<Integer> ids = null;
        try {
            ids = CookieSession.sm.reg.getUserIdList(org, includeLocked);
        } catch(SQLException se) {
            logger.log(java.util.logging.Level.WARNING,
                "Query for org " + org + " failed: " + DBUtils.unchainSqlException(se), se);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new STError(se)).build();
        }
        // now collect the actual users
        Set<UserItem> users = new TreeSet<>();
        for (final int id : ids) {
            UserItem u = CookieSession.sm.reg.getInfo(id).toUserItem();
            users.add(u);
        }

        return Response.ok(new UserList(users)).build();
    }

    public static final class UserList {
        UserList(Collection<UserItem> c) {
            users = c.toArray(new UserItem[c.size()]);
        }
        public UserItem users[];
    }
}
