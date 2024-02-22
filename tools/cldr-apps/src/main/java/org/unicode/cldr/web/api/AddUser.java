package org.unicode.cldr.web.api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.unicode.cldr.util.EmailValidator;
import org.unicode.cldr.util.LocaleNames;
import org.unicode.cldr.util.LocaleNormalizer;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.web.CookieSession;
import org.unicode.cldr.web.UserRegistry;
import org.unicode.cldr.web.UserRegistry.User;

@Path("/adduser")
@Tag(name = "adduser", description = "Add a new Survey Tool user")
public class AddUser {
    private enum AddUserError {
        BAD_NAME,
        BAD_EMAIL,
        BAD_ORG,
        BAD_LEVEL,
        DUP_EMAIL,
        UNKNOWN
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Add User", description = "This handles a request to add a new user")
    @APIResponses(
            value = {
                @APIResponse(
                        responseCode = "200",
                        description = "Results of AddUser request",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation = AddUserResponse.class,
                                                        example =
                                                                "{\"email\":\"test@example.com\",\"userId\":2526}"))),
                @APIResponse(responseCode = "403", description = "Forbidden"),
            })
    public Response addUser(
            AddUserRequest request, @HeaderParam(Auth.SESSION_HEADER) String sessionString) {
        try {
            CookieSession session = Auth.getSession(sessionString);
            if (session == null) {
                return Auth.noSessionResponse();
            }
            if (!UserRegistry.userCanCreateUsers(session.user)) {
                return Response.status(403, "Forbidden").build();
            }
            session.userDidAction();
            AddUserResponse response = doAddNewUser(request, session);
            return Response.ok(response, MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            return Response.status(500, "An exception occurred").entity(e).build();
        }
    }

    @Schema(description = "Response for AddUser query")
    public final class AddUserResponse {
        /**
         * Success
         *
         * @param userId = the new user's id
         * @param email = the new user's normalized email
         */
        public AddUserResponse(int userId, String email) {
            this.userId = userId;
            this.email = email;
            this.err = null;
        }

        /**
         * Failure
         *
         * @param err the error message
         */
        public AddUserResponse(AddUserError err) {
            this.userId = null;
            this.email = null;
            this.err = err;
        }

        @Schema(description = "User ID of successfully created new user")
        public Integer userId;

        @Schema(description = "Normalized e-mail of new user")
        public String email;

        @Schema(description = "Error code")
        public AddUserError err;
    }

    private AddUserResponse doAddNewUser(AddUserRequest request, CookieSession session) {
        String new_locales = getNewUserLocales(request.locales);
        String new_org = getNewUserOrg(request.org, session.user);
        if ((request.name == null) || (request.name.length() <= 0)) {
            return new AddUserResponse(AddUserError.BAD_NAME);
        } else if (!EmailValidator.passes(request.email)) {
            return new AddUserResponse(AddUserError.BAD_EMAIL);
        } else if (new_org == null || new_org.length() <= 0) {
            return new AddUserResponse(AddUserError.BAD_ORG);
        }
        UserRegistry reg = CookieSession.sm.reg;
        User u = reg.new User(UserRegistry.NO_USER);
        u.name = request.name;
        u.email = request.email;
        u.org = new_org;
        u.userlevel = request.level;
        u.locales = new_locales;
        u.setPassword(UserRegistry.makePassword());
        if (request.level < 0 || !reg.canSetUserLevel(session.user, u, request.level)) {
            return new AddUserResponse(AddUserError.BAD_LEVEL);
        }
        return reallyAdd(reg, u);
    }

    private String getNewUserLocales(String requestLocales) {
        requestLocales = LocaleNormalizer.normalizeQuietly(requestLocales);
        if (requestLocales.isEmpty()) {
            return LocaleNames.MUL;
        }
        return requestLocales;
    }

    private String getNewUserOrg(String requestOrg, User me) {
        if (!UserRegistry.userCreateOtherOrgs(me)) {
            return me.org; // if not admin, must create user in the same org
        }
        try {
            if (Organization.fromString(requestOrg) != null) {
                return requestOrg;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    private AddUserResponse reallyAdd(UserRegistry reg, User u) {
        User registeredUser = reg.newUser(null, u);
        if (registeredUser == null || registeredUser.id <= 0) {
            /*
             * We could test for duplicate e-mail earlier, but there's a slight chance
             * two admins would try to create the same user at almost the same time, so
             * we need to test here anyway, after the attempt has failed
             */
            if (reg.get(u.email) != null) { // already exists
                return new AddUserResponse(AddUserError.DUP_EMAIL);
            } else {
                return new AddUserResponse(AddUserError.UNKNOWN);
            }
        } else {
            /*
             * Success
             */
            return new AddUserResponse(registeredUser.id, registeredUser.email);
        }
    }
}
