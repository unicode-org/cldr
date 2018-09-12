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

    /**
     * mailBucket: mail waiting to go out. Hashmap: Integer(userid) -> String
     * body-of-mail-to-send
     *
     * This way, users only get one mail per service.
     *
     * this function sends out mail waiting in the buckets.
     *
     * @param vetting
     *            TODO
     * @param mailBucket
     *            map of mail going out already. (IN)
     * @param title
     *            the title of this mail
     * @return number of mails sent.
     */
    // int sendBucket(Vetting vetting, Map mailBucket, String title) {
    // int n =0;
    // String from = survprops.getProperty("CLDR_FROM","nobody@example.com");
    // String smtp = survprops.getProperty("CLDR_SMTP",null);
    // // System.err.println("FS: " + from + " | " + smtp);
    // boolean noMail = (smtp==null);
    //
    // ///*srl*/ noMail = true;
    //
    // for(Iterator li = mailBucket.keySet().iterator();li.hasNext();) {
    // Integer user = (Integer)li.next();
    // String s = (String)mailBucket.get(user);
    // User u = reg.getInfo(user.intValue());
    //
    // if(!UserRegistry.userIsTC(u)) {
    // s =
    // "Note: If you have questions about this email,  instead of replying here,\n "
    // +
    // "please contact your CLDR-TC representiative for your organization ("+u.org+").\n"+
    // "You can find the TC users listed near the top if you click '[List "+u.org+" Users] in the SurveyTool,\n"
    // +
    // "Or, at http://www.unicode.org/cldr/apps/survey?do=list\n"+
    // "If you are unable to contact them, then you may reply to this email. Thank you.\n\n\n"+s;
    // }
    //
    //
    // if(!noMail) {
    // MailSender.sendMail(smtp,null,null,from,u.email, title, s);
    // } else {
    // System.err.println("--------");
    // System.err.println("- To  : " + u.email);
    // System.err.println("- Subj: " + title);
    // System.err.println("");
    // System.err.println(s);
    // }
    // n++;
    // if((n%50==0)) {
    // System.err.println("Vetter.MailBucket: sent email " + n +
    // "/"+mailBucket.size());
    // }
    // }
    // return n;
    // }

    public static boolean getOrgDisputeCount(String voterOrg, CLDRLocale locale, int xpathId) {
        // TODO Auto-generated method stub
        return false;
    }
}
