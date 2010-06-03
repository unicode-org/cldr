package org.unicode.cldr.web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.web.DataSection.DataRow;
import org.unicode.cldr.web.DataSection.DataRow.CandidateItem;
import org.unicode.cldr.web.UserRegistry.User;
import org.unicode.cldr.web.Vetting.DataSubmissionResultHandler;

public class SummarizingSubmissionResultHandler implements
		DataSubmissionResultHandler {
	
	/**
	 * Print a log of each item's status change.
	 */
	static final boolean DEBUG = false;
	
	boolean hadErrors = false;
	public enum ItemStatus { ITEM_UNKNOWN, ITEM_GOOD, ITEM_BAD };
	public class ItemInfo {
		ItemStatus status = ItemStatus.ITEM_UNKNOWN;
		String description = null;
		Set<CheckStatus> errors = null;
		String d = null;
		private String proposedValue;
		public ItemStatus getStatus() {
			return status;
		}
		public String getDescription() {
			return description;
		}
		public Set<CheckStatus> getErrors() {
			return errors;
		}
		ItemInfo(String d) {
			this.d = d;
			this.status = ItemStatus.ITEM_UNKNOWN;
		}
		ItemInfo() {
			this.status = ItemStatus.ITEM_UNKNOWN;
		}
		void setError(CheckStatus status) {
			if(DEBUG) System.out.println(this + " - setError(status:"+status.getMessage()+")");
			setStatus(ItemStatus.ITEM_BAD);
			hadErrors = true;
			if(errors==null) { 
				errors= new TreeSet<CheckStatus>();
			}
			errors.add(status);
		}
		void setError() {
			if(DEBUG) System.out.println(this + " - setError()");
			setStatus(ItemStatus.ITEM_BAD);
			hadErrors = true;
		}
		void setError(String what) {
			setError();
			append(what);
		}
		void setOK() {
			if(DEBUG) System.out.println(this + " - setOK()");
			setStatus(ItemStatus.ITEM_GOOD);
		}
		void setOK(String what) {
			setOK();
			append(what);
		}
		private void setStatus(ItemStatus newStatus) {
			if(status == ItemStatus.ITEM_UNKNOWN) {
				status = newStatus;
			} else {
				assertStatus(newStatus);
			}
		}
		private void assertStatus(ItemStatus expectedStatus) {
			if(status != expectedStatus) {
				throw new InternalError(this.toString() + ": Expected status " + expectedStatus.name() + " but was " + status.name());
			}
		}
		void append(String what) {
			if(DEBUG) System.out.println(this + " - append("+what+")");
			if(what==null) return;
			if(description == null) {
				description=what;
			} else {
				description = description + ", " + what;
			}
		}
		public String toString() {
			StringBuffer sb = new StringBuffer();
			if(d!=null) {
				sb.append("{ItemInfo: "+d+" ");
			} else {
				sb.append("{ItemInfo ");
			}
			sb.append(getStatus() + ", " + getDescription() + "}");
			return sb.toString();
		}
		/**
		 * Record a proposed value.
		 * @param v
		 */
		public void setProposed(String v) {
			if(DEBUG) System.out.println(this + " - spv("+v+")");
			this.proposedValue = v;
		}
		/**
		 * @return the proposedValue
		 */
		public String getProposedValue() {
			return proposedValue;
		}
	}

	private ItemInfo getInfo(DataRow d) {
		return getInfo(d.xpath());
	}
	private ItemInfo getInfo(String xpath) {
		ItemInfo is = itemHash.get(xpath);
		if(is ==null) {
			is = new ItemInfo(xpath);
			itemHash.put(xpath, is);
		}
		return is;
	}
	Map<String, ItemInfo> itemHash = new HashMap<String, ItemInfo>();

	protected void setError(DataRow d, String what) {
		getInfo(d).setError(what);
	}
	protected void setOK(DataRow d, String what) {
		getInfo(d).setOK(what);
	}
	protected void setError(DataRow d, CheckStatus what) {
		getInfo(d).setError(what);
	}
	/**
	 * Were any errors encountered?
	 */
	public boolean hadErrors() {
		return hadErrors;
	}
	
	public ItemInfo  infoFor(DataRow p) {
		return infoFor(p.xpath());
	}
	public ItemInfo infoFor(String xpath) {
		return itemHash.get(xpath);
	}

	public void handleEmptyChangeto(DataRow p) {
		setError(p, "no data given");
	}

	public void handleError(DataRow p, CheckStatus status, String choice_v) {
		setError(p, "error on '"+choice_v+"'");
		setError(p, status);
	}

	public void handleNewValue(DataRow p, String choice_v, boolean hadFailures) {
		if(!hadFailures) {
			setOK(p, "New value: '"+choice_v+"'");
		} else {
			setError(p, "New value (with errors): '"+choice_v+"'");
		}
	}

	public void handleNoPermission(DataRow p, CandidateItem optionalItem,
			String string) {
		setError(p, "No permission: " + string);
	}

	public void handleRemoveItem(DataRow p, CandidateItem item, boolean b) {
		// non event
	}

	public void handleRemoveVote(DataRow p, User voter, CandidateItem item) {
		// non event
	}

	public void handleRemoved(DataRow p) {
		//nonevnta
	}

	public void handleResultCount(int j) {
		// non event
	}

	public void handleUnknownChoice(DataRow p, String choice) {
		setError(p, "Internal error, bad choice: " + choice);
	}

	public void handleVote(DataRow p, int oldVote, int base_xpath) {
		// non event
	}

	public void warnAcceptedAsVoteFor(DataRow p, CandidateItem item) {
		// non event
	}

	public void warnAlreadyVotingFor(DataRow p, CandidateItem item) {
		// non event
	}
	public boolean rejectErrorItem(DataRow p) {
		return true;
	}
	
	public void handleProposedValue(DataRow p, String choice_v) {
		getInfo(p).setProposed(choice_v);
	}	
}
