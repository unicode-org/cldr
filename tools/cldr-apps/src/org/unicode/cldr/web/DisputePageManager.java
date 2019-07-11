package org.unicode.cldr.web;

import org.unicode.cldr.util.CLDRLocale;

/**
 * TODO: this object exists just to capture the following code from the
 * being-removed Vetting object.
 *
 * @author srl
 *
 */
public class DisputePageManager {
    static void doDisputed(WebContext ctx) {
        ctx.sm.printHeader(ctx, "Disputed Items Page");
        ctx.sm.printUserTableWithHelp(ctx, "/DisputedItems");

        ctx.println("<a href='" + ctx.url() + "'>Return to SurveyTool</a><hr>");

        // doOrgDisputePage(ctx);

        ctx.addQuery(SurveyMain.QUERY_DO, "disputed");

        ctx.println("<h2>Disputed Items</h2>");

        ctx.println("<i>TODO</i>");

        // doDisputePage(ctx);

        ctx.sm.printFooter(ctx);
    }

    public static int getOrgDisputeCount(WebContext ctx) {
        // TODO Auto-generated method stub
        // ctx.session.user.voterOrg(),locale
        return 0;
    }

    public static boolean getOrgDisputeCount(String voterOrg, CLDRLocale locale, int xpathId) {
        // TODO Auto-generated method stub
        return false;
    }
}
