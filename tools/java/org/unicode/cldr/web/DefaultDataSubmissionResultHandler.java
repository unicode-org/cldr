/**
 * 
 */
package org.unicode.cldr.web;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.Vetting.DataSubmissionResultHandler;

/**
 * @author srl
 *
 */
public class DefaultDataSubmissionResultHandler implements
		DataSubmissionResultHandler {
	WebContext ctx;
	DefaultDataSubmissionResultHandler(WebContext ctx) {
		this.ctx = ctx;
	}
	
	protected void handleHadError(DataRow p) {
		
	}
	
	// These are the default behaviors when a pod is processed.
	
	public void handleResultCount(int j) {
        ctx.println("<br> You submitted data or vote changes, and " + j + " results were updated. As a result, your items may show up under the 'priority' or 'proposed' categories.<br>");
	}

	public void handleRemoveItem(DataRow p, CandidateItem item,
			boolean removingVote) {
		ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>:  Removing alternate \""+item.value+
				"\" ("+item.altProposed+")<br>");
		if(removingVote) {
			ctx.println(" <i>Also, removing your vote for it</i><br>");
		}
	}

	public void handleNoPermission(DataRow p, CandidateItem item,
			String what) {
		handleHadError(p);
		ctx.println(" <p class='ferrbox'>Warning: You don't have permission to "+what+" this item: " +"<tt class='codebox'>"+ p.displayName +"</tt>" + ".</p>");
	}

	public void handleRemoveVote(DataRow p, UserRegistry.User voter, CandidateItem item) {
        ctx.print("<tt title='#"+p.base_xpath+"' class='codebox'>"+ p.displayName +"</tt>:  Removing vote for <span title='#"+item.xpathId+"'>"+"\""+item.value+
                "\" ("+item.altProposed+")</span> by " + voter.toHtml(ctx.session.user) +  "<br>");
	}

	public void handleEmptyChangeto(DataRow p) {
		handleHadError(p);
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.println(ctx.iconHtml("stop","empty value")+ " value was left empty. <!-- Use 'remove' to request removal. --><br>");
	}

	public void warnAcceptedAsVoteFor(DataRow p, CandidateItem item) {
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.println(ctx.iconHtml("warn","duplicate")+" This value was already entered, accepting your vote for " + 
            ((item.altProposed==null)?" the current item. <br>":(" the proposal <tt>"+item.altProposed+"</tt>.<br>")));
	}

	public void warnAlreadyVotingFor(DataRow p, CandidateItem item) {
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.println(ctx.iconHtml("warn","duplicate")+" Your current vote is already for " + 
            ((item.altProposed==null)?" the current item ":(" the proposal <tt>"+item.altProposed+"</tt> "))+" which has the same value.<br>");
	}

	public void handleNewValue(DataRow p, String choice_v, boolean hadFailures) {
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.print("&nbsp;&nbsp; New value: <b>" + choice_v +"</b> ");
        if(!hadFailures) {
            ctx.println(" "+ctx.iconHtml("okay","new")+" <br>");
        } else {
            ctx.println("<br><b>This item had test failures, but was added.</b><br>");
        }
	}

	public void handleError(DataRow p, CheckStatus status,
			String choice_v) {
		handleHadError(p);
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.print("&nbsp;&nbsp; Value: <b>" + choice_v +"</b>  ");
        String cls = SurveyMain.shortClassName(status.getCause());
        ctx.printHelpLink("/"+cls,"<!-- help with -->"+cls, true);
        if(status.getType().equals(status.errorType)) {
            ctx.print(ctx.iconHtml("stop",cls));
        } else {
            ctx.print(ctx.iconHtml("warn",cls));
        }
        ctx.println(" "+ status.toString() + "<br>" );
	}

	public void handleRemoved(DataRow p) {
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.print(" <i>(removed)</i><br>");
	}

	public void handleVote(DataRow p, int oldVote, int newVote) {
		if(newVote == p.base_xpath) {
            ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
            ctx.println("<!-- Registering vote for "+p.base_xpath+" - "+p.getLocale()+":" + p.base_xpath+" (base_xpath) replacing " + oldVote + " --> " + 
                    ctx.iconHtml("okay","voted")+" Vote accepted. <br>");
		} else if(newVote == -1) {
            ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
            ctx.println("<!-- Registering vote for "+p.base_xpath+" - "+p.getLocale()+":-1 replacing " + oldVote + " -->" + 
                ctx.iconHtml("okay","voted")+" Removing vote. <br>");
		} else {
            ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
            ctx.println("<!-- Registering vote for "+p.base_xpath+" - "+p.getLocale()+":" + newVote + " replacing " + oldVote + " --> " + 
                    ctx.iconHtml("okay","voted")+" Vote accepted. <br>");
		}
	}

	public void handleUnknownChoice(DataRow p, String choice) {
		handleHadError(p);
        ctx.print("<tt class='codebox'>"+ p.displayName +"</tt>: ");
        ctx.println(ctx.iconHtml("stop","unknown")+"<tt title='"+p.getLocale()+":"+p.base_xpath+"' class='codebox'>" + p.displayName + "</tt> Note: <i>" + choice + "</i> not supported yet or item not found. <br>");
	}

	public boolean rejectErrorItem(DataRow p) {
		return false; // don't reject normally.
	}	
}
