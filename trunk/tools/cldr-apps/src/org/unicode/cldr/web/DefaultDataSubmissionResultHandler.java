/**
 *
 */
package org.unicode.cldr.web;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;

/**
 * @author srl
 *
 */
public class DefaultDataSubmissionResultHandler implements DataSubmissionResultHandler {
    WebContext ctx;

    DefaultDataSubmissionResultHandler(WebContext ctx) {
        this.ctx = ctx;
    }

    protected void handleHadError(DataRow p) {

    }

    // These are the default behaviors when a pod is processed.

    @Override
    public void handleResultCount(int j) {
        ctx.println("<br> You submitted data or vote changes, and " + j
            + " results were updated. As a result, your items may show up under the 'priority' or 'proposed' categories.<br>");
    }

    @Override
    public void handleRemoveItem(DataRow p, CandidateItem item, boolean removingVote) {
        /*
         * TODO: CandidateItem.altProposed is a constant string "n/a". Is there any reason to use it here?
         */
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>:  Removing alternate \"" + item.getValue() + "\" ("
            + CandidateItem.altProposed + ")<br>");
        if (removingVote) {
            ctx.println(" <i>Also, removing your vote for it</i><br>");
        }
    }

    @Override
    public void handleNoPermission(DataRow p, CandidateItem item, String what) {
        handleHadError(p);
        ctx.println(" <p class='ferrbox'>Warning: You don't have permission to " + what + " this item: " + "<tt class='codebox'>"
            + p.getDisplayName() + "</tt>" + ".</p>");
    }

    @Override
    public void handleRemoveVote(DataRow p, UserRegistry.User voter, CandidateItem item) {
        /*
         * TODO: CandidateItem.altProposed is a constant string "n/a". Is there any reason to use it here?
         */
        ctx.print("<tt title='#" + p.getXpathId() + "' class='codebox'>" + p.getDisplayName()
            + "</tt>:  Removing vote for <span title='#" + p.getXpathId() + "'>" + "\"" + item.getValue() + "\" ("
            + CandidateItem.altProposed + ")</span> by " + voter.toHtml(ctx.session.user) + "<br>");
    }

    @Override
    public void handleEmptyChangeto(DataRow p) {
        handleHadError(p);
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        ctx.println(ctx.iconHtml("stop", "empty value") + " value was left empty. <!-- Use 'remove' to request removal. --><br>");
    }

    public void warnAcceptedAsVoteFor(DataRow p, CandidateItem item) {
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        /*
         * TODO: CandidateItem.altProposed is a constant string "n/a". Is there any reason to use it here?
         * It is never null.
         */
        ctx.println(ctx.iconHtml("warn", "duplicate")
            + " This value was already entered, accepting your vote for "
            + ((CandidateItem.altProposed == null) ? " the current item. <br>"
                : (" the proposal <tt>" + CandidateItem.altProposed + "</tt>.<br>")));
    }

    public void warnAlreadyVotingFor(DataRow p, CandidateItem item) {
        /*
         * TODO: CandidateItem.altProposed is a constant string "n/a". Is there any reason to use it here?
         * It is never null.
         */
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        ctx.println(ctx.iconHtml("warn", "duplicate") + " Your current vote is already for "
            + ((CandidateItem.altProposed == null) ? " the current item " : (" the proposal <tt>" + CandidateItem.altProposed + "</tt> "))
            + " which has the same value.<br>");
    }

    @Override
    public void handleNewValue(DataRow p, String choice_v, boolean hadFailures) {
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        ctx.print("&nbsp;&nbsp; New value: <b>" + choice_v + "</b> ");
        if (!hadFailures) {
            ctx.println(" " + ctx.iconHtml("okay", "new") + " <br>");
        } else {
            ctx.println("<br><b>This item had test failures, but was added.</b><br>");
        }
    }

    @Override
    public void handleError(DataRow p, CheckStatus status, String choice_v) {
        handleHadError(p);
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        ctx.print("&nbsp;&nbsp; Value: <b>" + choice_v + "</b>  ");
        String cls = SurveyMain.shortClassName(status.getCause());
        ctx.printHelpLink("/" + cls, "<!-- help with -->" + cls, true);
        if (status.getType().equals(CheckStatus.errorType)) {
            ctx.print(ctx.iconHtml("stop", cls));
        } else {
            ctx.print(ctx.iconHtml("warn", cls));
        }
        ctx.println(" " + status.toString() + "<br>");
    }

    @Override
    public void handleRemoved(DataRow p) {
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        ctx.print(" <i>(removed)</i><br>");
    }

    @Override
    public void handleVote(DataRow p, String oldVote, String newVote) {
        if (newVote == null) {
            ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
            ctx.println("<!-- Registering vote for " + p.getXpath() + " - " + p.getLocale() + ":-1 replacing " + oldVote + " -->"
                + ctx.iconHtml("okay", "voted") + " Removing vote. <br>");
        } else {
            ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
            ctx.println("<!-- Registering vote for " + p.getXpath() + " - " + p.getLocale() + ":" + newVote + " replacing "
                + oldVote + " --> " + ctx.iconHtml("okay", "voted") + " Vote accepted. <br>");
        }
    }

    @Override
    public void handleUnknownChoice(DataRow p, String choice) {
        handleHadError(p);
        ctx.print("<tt class='codebox'>" + p.getDisplayName() + "</tt>: ");
        ctx.println(ctx.iconHtml("stop", "unknown") + "<tt title='" + p.getLocale() + ":" + p.getXpath() + "' class='codebox'>"
            + p.getDisplayName() + "</tt> Note: <i>" + choice + "</i> not supported yet or item not found. <br>");
    }

    @Override
    public boolean rejectErrorItem(DataRow p) {
        return false; // don't reject normally.
    }

    @Override
    public void handleProposedValue(DataRow p, String choice_v) {
        // no-op
    }
}
