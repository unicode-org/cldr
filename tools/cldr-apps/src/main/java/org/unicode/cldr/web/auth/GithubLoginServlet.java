package org.unicode.cldr.web.auth;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.unicode.cldr.web.*;
import org.unicode.cldr.web.ClaGithubList.SignEntry;
import org.unicode.cldr.web.ClaGithubList.SignStatus;

/** redirect back into the FE */
public class GithubLoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = SurveyLog.forClass(GithubLoginServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        final String code = request.getParameter("code");
        // TODO: getParameter("state")
        if (code != null && !code.isBlank()) {
            final LoginSession ls = GithubLoginFactory.getInstance().forCode(code);
            if (ls != null && ls.isValid()) {
                logger.finest("Github Code: " + code + " => ls " + ls);
                CookieSession cs = WebContext.peekSession(request);
                if (cs == null) {
                    logger.severe("Github Code " + code + " but no session!");
                    response.sendRedirect(
                            "v?error=nosession"); // will just send people to the front page.
                } else {
                    LoginManager.getInstance().setLoginSession(cs, ls);

                    if (updateSignatoryStatus(cs, ls)) {
                        response.sendRedirect("v?signed=github&github=" + ls.getId() + "#cla");
                    } else {
                        response.sendRedirect("v#cla"); // FE will detect this case
                    }
                }
            } else {
                logger.fine("no code, ignoring");
                response.sendRedirect("v?error=github#cla"); // err processing code
            }
        } else {
            logger.fine("no code, ignoring");
            response.sendRedirect("v?error=login#cla"); // err, no code
        }
    }

    /**
     * handle logic where a github ID has been added. If the user is a signatory, we record it as a
     * valid CLA. Otherwise we just continue. The front end can detect that GitHub is present but
     * NOT signed.
     *
     * @return true if CLA newly marked as signed
     */
    private boolean updateSignatoryStatus(CookieSession cs, LoginSession ls) {
        SignEntry e = ClaGithubList.getInstance().getSignEntry(ls.getId());
        logger.info("For user " + ls.getId() + " sign status is " + e);
        // can't update user if logged out
        if (cs.user == null) {
            logger.severe(
                    "Signatory status for "
                            + ls
                            + " but user logged out of ST for session "
                            + cs.id);
            return false;
        }

        if (cs.user.claSigned) {
            // already signed, won't unsign!
            logger.info(
                    "For GH user "
                            + ls.getId()
                            + " - OK login but CLA already signed for "
                            + cs.user.email
                            + " #"
                            + cs.user.id);
            return false;
        }

        if (e == null || e.getSignStatus() != SignStatus.signed) {
            return false; // not signed, so nothing to update
        }

        ClaSignature cla = new ClaSignature();
        cla.name = e.name;
        cla.signed = e.getSignedAt();
        cla.employer = e.employer;
        cla.email = e.email;
        cla.github = e.user_name;
        cs.user.signCla(cla);
        logger.info(
                "Marked as signed from GitHub: "
                        + cs.user.email
                        + " #"
                        + cs.user.id
                        + ", via "
                        + ls
                        + ", sign status is "
                        + e);
        return true;
    }
}
