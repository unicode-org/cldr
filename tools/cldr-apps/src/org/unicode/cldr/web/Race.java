//  Copyright 2006-2008 IBM. All rights reserved.

package org.unicode.cldr.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LDMLUtilities;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.util.XPathParts;
import org.unicode.cldr.web.Vetting.Status;

/**
 * This class represents a particular item that can be voted for, a single
 * "contest" if you will.
 * 
 * It is being updated to use the VoteResolver interface, 
 * for now, we calculate two things in parallel.
 */
public class Race {
    
    private VoteResolver<Integer> resolver; // allocate new with locale = new VoteResolver<Integer>();

    /**
     * 
     */
    private final Vetting vet;

    /**
     * @param vetting
     */
    Race(Vetting vetting, CLDRLocale locale) {
        vet = vetting;
        resolver = new VoteResolver<Integer>();
        resolver.setEstablishedFromLocale(locale.getBaseName());
    }

    // The vote of a particular organization for this item.
    class Organization implements Comparable {
        String name; // org's name
        Chad vote = null; // the winning item: -1 for unknown
        int strength = 0; // strength of the vote
        public boolean dispute = false;
        public boolean checked = false; // checked the vote yet?

        Set<Chad> votes = new TreeSet<Chad>(); // the set of chads

        public Organization(String name) {
            this.name = name;
        }

        /**
         * Factory function - create an existing Vote
         */
        public Organization(Chad existingItem) {
            name = Vetting.EXISTING_ITEM_NAME;
            vote = existingItem;
            strength = Vetting.EXISTING_VOTE;
        }

        // we have some interest in this race
        public void add(Chad c) {
            votes.add(c);
        }

        // this is the org's default vote.
        public void setDefaultVote(Chad c, int withStrength) {
            // votes.add(c); // so it is in the list
            vote = c; // and it is the winning vote
            strength = withStrength;
        }


        public int compareTo(Object o) {
            if (o == this) {
                return 0;
            }
            Organization other = (Organization) o;
            if (other.strength > strength) {
                return 1;
            } else if (other.strength < strength) {
                return -1;
            } else {
                return name.compareTo(other.name);
            }
        }
    }

    // All votes for a particular item
    class Chad implements Comparable {
        public int xpath = -1;
        public int full_xpath = -1;
        public Set<UserRegistry.User> voters = new HashSet<UserRegistry.User>(); // Set
                                                                                    // of
                                                                                    // users
                                                                                    // which
                                                                                    // voted
                                                                                    // for
                                                                                    // this.
        public Set<Organization> orgs = new TreeSet<Organization>(); // who
                                                                        // voted
                                                                        // for
                                                                        // this?
        public int score = 0;
        public String value = null;

        public String refs = null;

        public Set<Organization> orgsDefaultFor = null; // if non-null: this
                                                        // Chad is the default
                                                        // choice for the org[s]
                                                        // involved, iff they
                                                        // didn't already vote.

        boolean checkedDisqualified = false;
        boolean disqualified = false;

        public Chad(int xpath, int full_xpath, String value) {
            this.xpath = xpath;
            this.full_xpath = full_xpath;
            this.value = value;

        }

        public boolean isDisqualified() {
            if(checkedDisqualified) {
                return disqualified;
            } else {
                return checkDisqualified();
            }
        }

        boolean checkDisqualified() {
            if (disqualified) {
                return true;
            }
            if (value == null) { // non existent item. ignore.
                checkedDisqualified=true;
                disqualified=false;
                return false;
            }
            disqualified = Race.this.vet.test(locale, xpath, full_xpath, value);
            checkedDisqualified=true;
            if (disqualified && !Vetting.MARK_NO_DISQUALIFY) {
                score = 0;
                // if(/*sm.isUnofficial && */ base_xpath==83422) {
                // System.err.println("DISQ: " + locale + ":"+xpath + "
                // ("+base_xpath+") - " + value);
                // }
            }
            return disqualified;
        }

        /**
         * Call this if the item is a default vote for an organization
         */
        public void addDefault(Organization org) {
            // do NOT add to 'orgs'
            if (orgsDefaultFor == null) {
                orgsDefaultFor = new HashSet<Organization>();
            }
            orgsDefaultFor.add(org);
        }

        public void add(Organization votes) {
            orgs.add(votes);
            if (!(disqualified && !Vetting.MARK_NO_DISQUALIFY) && value != null) {
                score += votes.strength;
            }
        }

        public void vote(UserRegistry.User user) {
            voters.add(user);
        }

        public int compareTo(Object o) {
            if (o == this) {
                return 0;
            }
            Chad other = (Chad) o;

            if (other.xpath == xpath) {
                return 0;
            }

            if (value == null) {
                if (other.value == null) {
                    return 0;
                } else {
                    return "".compareTo(other.value);
                }
            } else {
                if (other.value == null) {
                    return 1;
                } else {
                    return value.compareTo(other.value);
                }
            }
        }
    }

    // Race variables
    public int base_xpath;
    public CLDRLocale locale;
    public Hashtable<Integer, Chad> chads = new Hashtable<Integer, Chad>();
    public Hashtable<String, Chad> chadsByValue = new Hashtable<String, Chad>();
    public Hashtable<String, Chad> chadsByValue2 = new Hashtable<String, Chad>();
    public Hashtable<String, Organization> orgVotes = new Hashtable<String, Organization>();
    public Set<Chad> disputes = new TreeSet<Chad>();

    // winning info
    public Chad winner = null;
    public Chad Nchad=null, Ochad = null;
    public Status status = Status.INDETERMINATE;
    public VoteResolver.Status vrstatus = VoteResolver.Status.missing;
    public Chad existing = null; // existing vote
    public Status existingStatus = Status.INDETERMINATE;
    int nexthighest = 0;
    public boolean hadDisqualifiedWinner = false; // at least one of the
                                                    // winners disqualified
    public boolean hadOtherError = false; // had an error on a missing item
                                            // (coverage or collision?)
    int id; // for writing
    public Set<String> refConflicts = new HashSet<String>(); // had any items
                                                                // that differ
                                                                // only in refs?

    /* reset all */
    public void clear() {
        chads.clear();
        chadsByValue.clear();
        chadsByValue2.clear();
        orgVotes.clear();
        disputes.clear();
        winner = null;
        refConflicts.clear();
        existing = null;
        base_xpath = -1;
        nexthighest = 0;
        hadDisqualifiedWinner = false;
        hadOtherError = false;
        resolver.clear();
    }

    /* Reset this for a new item */
    public void clear(int base_xpath, CLDRLocale locale) {
        clear(base_xpath, locale, -1);
    }

    /* Reset this for a new item */
    public void clear(int base_xpath, CLDRLocale locale, int id) {
        clear();
        this.base_xpath = base_xpath;
        this.locale = locale;
        this.id = id;
        
    }

    /**
     * calculate the optimal item, if any recalculate any items
     */
    public int optimal() throws SQLException {
        gatherVotes();

        Chad optimal = calculateWinner();

        if (optimal == null) {
            return -1;
        } else {
            return optimal.xpath;
        }
    }

    /* check for errors */
//    boolean recountIfHadDisqualified() {
//        boolean hadDisqualified = false;
//        for (Chad c : chads.values()) {
//            if (c.checkDisqualified()) {
//                hadDisqualified = true;
//            }
//        }
//        if (winner == null) {
//            /*
//             * if(test(locale, base_xpath, base_xpath, null)) { // check the
//             * base item - i.e. coverage... hadOtherError = true; }
//             */
//            return hadDisqualified || hadOtherError;
//        }
//        if (!winner.disqualified) {
//            return hadDisqualified;
//        }
//
//        if (!Vetting.MARK_NO_DISQUALIFY) {
//            // actually remove winner, set to 0, etc.
//            hadDisqualifiedWinner = true;
//            winner = null; // no winner
//            nexthighest = 0;
////            calculateOrgVotes();
//
//            calculateWinner();
//        }
//        return hadDisqualified;
//    }

    private Chad getChad(int vote_xpath, int full_xpath, String value) {
        String valueForLookup = (value != null) ? value : Vetting.EMPTY_STRING;
        String nonEmptyValue = valueForLookup;

        String full_xpath_string = vet.sm.xpt.getById(full_xpath);
        String theReferences = null;
        if (full_xpath_string.indexOf(LDMLConstants.REFERENCES) >= 0) {
            XPathParts xpp = new XPathParts(null, null);
            xpp.initialize(full_xpath_string);
            String lelement = xpp.getElement(-1);
            // String eAlt = xpp.findAttributeValue(lelement,
            // LDMLConstants.ALT);
            theReferences = xpp.findAttributeValue(lelement, LDMLConstants.REFERENCES);
            if (theReferences != null) {
                // disambiguate it from the other value
                valueForLookup = valueForLookup + " [" + theReferences + "]";
                // if(value==null) {
                // value = "";
                // }
                // value = value + "&nbsp;<i title='This item has a
                // Reference.'>(reference)</i>";
            }
        }

        Chad valueChad = chadsByValue.get(valueForLookup); // merge equivalent values
        if (valueChad != null) {
            return valueChad;
        } else {
            Chad otherChad = chadsByValue2.get(nonEmptyValue);
            if (otherChad != null && otherChad.refs != theReferences) {
                refConflicts.add(nonEmptyValue);
            }
        }

        Integer vote_xpath_int = new Integer(vote_xpath);
        Chad c = chads.get(vote_xpath_int);
        if (c == null) {
            c = new Chad(vote_xpath, full_xpath, value);
            chads.put(vote_xpath_int, c);
            chadsByValue.put(valueForLookup, c);
            chadsByValue2.put(nonEmptyValue, c);
            c.refs = theReferences;
        }
        return c;
    }

    private Organization getOrganization(String org) {
        Organization theirOrg = orgVotes.get(org);
        if (theirOrg == null) {
            theirOrg = new Organization(org);
            orgVotes.put(org, theirOrg);
        }
        return theirOrg;
    }

    private org.unicode.cldr.util.VoteResolver.Organization getVROrg(String name) {
	return org.unicode.cldr.util.VoteResolver.Organization.fromString(name);
    }
 
    /**
     * Get a map of xpath to score for this org.
     */
    public Map<Integer, Long> getOrgToVotes(String org) {
	return resolver.getOrgToVotes(getVROrg(org));
    }

    /**
     * Get the last release xpath
     */
    public int getLastReleaseXpath() {
	return resolver.getLastReleaseValue();
    }

    /**
     * Get the last release status
     */
    public Status getLastReleaseStatus() {
	return Status.toStatus(resolver.getLastReleaseStatus());
    }

    private final void existingVote(int vote_xpath, int full_xpath, String value) {
        vote(null, vote_xpath, full_xpath, value);
    }

    private final void defaultVote(String org, int vote_xpath, int full_xpath, String value) {
        Chad c = getChad(vote_xpath, full_xpath, value);
        c.addDefault(getOrganization(org));
    }

    private void vote(UserRegistry.User user, int vote_xpath, int full_xpath, String value) {
        // add this vote to the chads, or create one if not present
        Chad c = getChad(vote_xpath, full_xpath, value);

        if (user != null) {
            c.vote(user);

            // add the chad to the set of orgs' chads
            Organization theirOrg = getOrganization(user.voterOrg()); // we use the VR organization code
            theirOrg.add(c);
            if(!c.isDisqualified()) {
                resolver.add(c.xpath, user.id); /* use chad's xpath- for collision calculation. */
            }
        } else {
            // "existing" vote
            existing = c;
            
            /* load values from c = could be a collision. */
            vote_xpath = c.xpath;
            full_xpath = c.full_xpath;

            if (vote_xpath == full_xpath && vote_xpath == base_xpath) { // shortcut:
                                                                        // base=full
                                                                        // means
                                                                        // it is
                                                                        // confirmed.
                existingStatus = Status.APPROVED;
            } else {
                String fullpathstr = vet.sm.xpt.getById(full_xpath);
                // xpp.clear();
                XPathParts xpp = new XPathParts(null, null);
                xpp.initialize(fullpathstr);
                String lelement = xpp.getElement(-1);
                String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);
                if (eDraft == null || eDraft.equals("approved")) {
                    existingStatus = Status.APPROVED;
                } else {
                    existingStatus = Status.UNCONFIRMED;
                }
            }
        }
    }

    /**
     * @returns number of votes counted, including abstentions
     */
    private int gatherVotes() throws SQLException {
        VoteResolver.setVoterToInfo(vet.sm.reg.getVoterToInfo());

        // set status of 'last release' (base) data
        
        vet.queryVoteForBaseXpath.setString(1, locale.toString());
        vet.queryVoteForBaseXpath.setInt(2, base_xpath);
        vet.queryValue.setString(1, locale.toString());

        // Add the base xpath. It may come in as data later, and there may be no other values (and no votes).
        // If the base xpath wins, and there's no matching chad, the winning result will be 'null'.
        resolver.add(base_xpath);
        
        ResultSet rs;

        /**
         * Get the existing item.
         */
        vet.queryValue.setInt(2, base_xpath);
        rs = vet.queryValue.executeQuery();
        if (rs.next()) {
            String itemValue = DBUtils.getStringUTF8(rs, 1);
            int origXpath = rs.getInt(2);
            existingVote(base_xpath, origXpath, itemValue);
            
            // get draft status for VR
            String fullpathstr = vet.sm.xpt.getById(origXpath);
            // xpp.clear();
            XPathParts xpp = new XPathParts(null, null);
            xpp.initialize(fullpathstr);
            String lelement = xpp.getElement(-1);
            String eDraft = xpp.findAttributeValue(lelement, LDMLConstants.DRAFT);
            VoteResolver.Status newStatus = VoteResolver.Status.valueOf(
                        VoteResolver.fixBogusDraftStatusValues(eDraft));
            // 'last release' here means, whatever is in CVS
            resolver.setLastRelease(base_xpath, newStatus );
        } else {
            // 'last release' is missing
            resolver.setLastRelease(base_xpath, VoteResolver.Status.missing );
        }
        
        // Now, fetch all votes for this path.
        rs = vet.queryVoteForBaseXpath.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
            int submitter = rs.getInt(1);
            int vote_xpath = rs.getInt(2);
            /* String vote_value = rs.getString(4); */
            if (vote_xpath == -1) {
                continue; // abstention
            }

            vet.queryValue.setInt(2, vote_xpath);
            ResultSet crs = vet.queryValue.executeQuery();
            int orig_xpath = vote_xpath;
            String itemValue = null;
            if (crs.next()) {
                itemValue = DBUtils.getStringUTF8(crs, 1);
                orig_xpath = crs.getInt(2);
            

                UserRegistry.User u = vet.sm.reg.getInfo(submitter);
            	vote(u, vote_xpath, orig_xpath, itemValue);
            }
        }


        // Check for default votes

//        // Google: (proposed-x650)
//        vet.googData.setString(1, locale);
//        vet.googData.setInt(2, base_xpath);
//        rs = vet.googData.executeQuery(); // select xpath,origxpath,value from
//                                            // CLDR_DATA where
//                                            // alt_type='proposed-x650' and
//                                            // locale='af' and base_xpath=194130
//        if (rs.next()) {
//            int vote_xpath = rs.getInt(1);
//            int origXpath = rs.getInt(2);
//            String value = SurveyMain.getStringUTF8(rs, 3);
//            defaultVote("Google", vote_xpath, origXpath, value);
//        }

        // Now, add ALL other possible items.
        
        Map<Chad,Integer> possibles = new HashMap<Chad,Integer>(); // checking if it is disqualified could load other bundles, leading to contention on dataByBase's RS
       
      //  synchronized(vet) {
          if(dbb>5) {
        	  vet.sm.busted("dbb isn't 1, it's " + dbb + " !");
          }
	        if(++dbb!=1) {
	        	throw new InternalError("Going in: dbb isn't 1, it's " + dbb);
	        }
	        
	        
	        vet.dataByBase.setString(1, locale.toString());
	        vet.dataByBase.setInt(2, base_xpath);
	        rs = vet.dataByBase.executeQuery();
	        while (rs.next()) {
	            int xpath = rs.getInt(1);
	            int origXpath = rs.getInt(2);
	            // 3 : alt_type
	            String value = DBUtils.getStringUTF8(rs, 4);
	            Chad c = getChad(xpath, origXpath, value);
	            possibles.put(c,xpath);
//	            if(c.xpath != xpath) {
//	            	throw new InternalError("Chad has xpath " + c.xpath+" but supposed to be + " + xpath);
//	            }
	        }

	        if(--dbb!=0) {
	        	throw new InternalError("Going in: dbb isn't 0, it's " + dbb);
	        }
    //    }

        for(Map.Entry<Race.Chad,Integer> e : possibles.entrySet()) {        	
	        if(!e.getKey().isDisqualified()) {
	            resolver.add(e.getValue());
	        }
        }
        
        return count;
    }
    
    static int dbb = 0;
    

    
    /**
     * Calculate the winning item of the race. May be null if n/a.
     */
    private Chad calculateWinner() {
        int winningXpath = resolver.getWinningValue();
	Integer NXpath = resolver.getNValue();
	Integer OXpath = resolver.getOValue();

        vrstatus = resolver.getWinningStatus();
        status = Status.toStatus(vrstatus); // convert from VR status
        winner = chads.get(winningXpath);
	if(NXpath != null) {
	    Nchad = chads.get(NXpath);
	}
	if(OXpath != null) {
	    Ochad = chads.get(OXpath);
	}
//        System.out.println(resolver.toString() + " \n - resolved, winner: " + winner + " Found:"+(winner!=null));
        return winner;
    }
    
    public String resolverToString() {
        return resolver.toString() + "\n"+
            "WinningXpath: " +resolver.getWinningValue()+ " "+resolver.getWinningStatus()+"\n";
    }

    /**
     * This function is called to update the CLDR_OUTPUT and CLDR_ORGDISPUTE
     * tables. assumes a lock on (conn) held by caller.
     * 
     * @return type
     */
    public int updateDB() throws SQLException {
        // First, zap old data
        int rowsUpdated = 0;

        // zap old CLDR_OUTPUT rows
        vet.outputDelete.setString(1, locale.toString());
        vet.outputDelete.setInt(2, base_xpath);
        rowsUpdated += vet.outputDelete.executeUpdate();

        // zap orgdispute
        vet.orgDisputeDelete.setString(1, locale.toString());
        vet.orgDisputeDelete.setInt(2, base_xpath);
        rowsUpdated += vet.orgDisputeDelete.executeUpdate();

        vet.outputInsert.setString(1, locale.toString());
        vet.outputInsert.setInt(2, base_xpath);
        vet.orgDisputeInsert.setString(2, locale.toString());
        vet.orgDisputeInsert.setInt(3, base_xpath);
        // Now, IF there was a valid result, store it.
        // outputInsert = prepareStatement("outputInsert", // loc, basex, outx,
        // outFx, datax
        // outputInsert: #1 locale, #2 basex, #3 OUTx (the "user" xpath, i.e.,
        // no alt confirmed), #4 outFx #5 DATAx (where the data really lives.
        // For the eventual join that will find the data.)
        String baseString = vet.sm.xpt.getById(base_xpath);

        XPathParts xpp = new XPathParts(null, null);
        xpp.clear();
        xpp.initialize(baseString);
        String lelement = xpp.getElement(-1);
        String eAlt = xpp.findAttributeValue(lelement, LDMLConstants.ALT);

        String alts[] = LDMLUtilities.parseAlt(eAlt);
        String altvariant = null;
        String baseNoAlt = baseString;

        if (alts[0] != null) { // it has an alt, so deconstruct it.
            altvariant = alts[0];
            baseNoAlt = vet.sm.xpt.removeAlt(baseString);
        }

        if (winner != null) {
            int winnerPath = base_xpath; // shortcut - this IS the base
                                            // xpath.

            String baseFString = vet.sm.xpt.getById(winner.full_xpath);
            String baseFNoAlt = baseFString;
            if (alts[0] != null) {
                baseFNoAlt = vet.sm.xpt.removeAlt(baseFString);
            }

            baseFNoAlt = vet.sm.xpt.removeAttribute(baseFNoAlt, "draft");
            baseFNoAlt = vet.sm.xpt.removeAttribute(baseFNoAlt, "alt");

            int winnerFullPath = vet.makeXpathId(baseFNoAlt, altvariant, null, status);

            if (winner.disqualified && Vetting.MARK_NO_DISQUALIFY) {
                winnerFullPath = vet.makeXpathId(baseFNoAlt, altvariant, "proposed-x555", status);
            }

            vet.outputInsert.setInt(3, winnerPath); // outputxpath = base, i.e.
                                                    // no alt/proposed.
            vet.outputInsert.setInt(4, winnerFullPath); // outputFullxpath =
                                                        // base, i.e. no
                                                        // alt/proposed.
            vet.outputInsert.setInt(5, winner.xpath); // data = winner.xpath
            vet.outputInsert.setInt(6, status.intValue());
            rowsUpdated += vet.outputInsert.executeUpdate();
        }

        // add any other items
        int jsernum = 1000; // starting point for x proposed designation
        for (Chad other : chads.values()) {
            // skip if:
            if (other == winner || (other.disqualified && !Vetting.MARK_NO_DISQUALIFY)
                    || other.voters == null || other.voters.isEmpty()) { 
                // skip the winner and any disqualified or no-vote items.
                continue;
            }
            jsernum++;
            Status proposedStatus = Status.UNCONFIRMED;
            String altproposed = "proposed-x" + jsernum;
            int aPath = vet.makeXpathId(baseNoAlt, altvariant, altproposed, Status.INDETERMINATE);

            String baseFString = vet.sm.xpt.getById(other.full_xpath);
            String baseFNoAlt = baseFString;
            if (alts[0] != null) {
                baseFNoAlt = vet.sm.xpt.removeAlt(baseFString);
            }
            baseFNoAlt = vet.sm.xpt.removeAttribute(baseFNoAlt, "draft");
            baseFNoAlt = vet.sm.xpt.removeAttribute(baseFNoAlt, "alt");
            int aFullPath = vet.makeXpathId(baseFNoAlt, altvariant, altproposed, proposedStatus);

            // otherwise, show it under something.
            vet.outputInsert.setInt(3, aPath); // outputxpath = base, i.e. no
                                                // alt/proposed.
            vet.outputInsert.setInt(4, aFullPath); // outputxpath = base, i.e.
                                                    // no alt/proposed.
            vet.outputInsert.setInt(5, other.xpath); // data = winner.xpath
            vet.outputInsert.setInt(6, proposedStatus.intValue());
            rowsUpdated += vet.outputInsert.executeUpdate();
        }

        // update disputes, if any
//        for (Organization org : orgVotes.values()) {
//            if (org.dispute) {
//                vet.orgDisputeInsert.setString(1, org.name);
//                rowsUpdated += vet.orgDisputeInsert.executeUpdate();
//            }
//        }
        for(VoteResolver.Organization org : resolver.getConflictedOrganizations()) {
//            Organization sorg = Organization.fromOrganization(org); // convert from foreign format
            vet.orgDisputeInsert.setString(1, org.name()); // use foreign format
            rowsUpdated += vet.orgDisputeInsert.executeUpdate();
        }

        // Now, update the vote results
        int resultXpath = -1;
        int type = 0;
        if (winner != null) {
            resultXpath = winner.xpath;
        }

        // Examine the results
        if (resultXpath != -1) {
            if (resolver.isDisputed()/* && (status != Status.APPROVED)*/) { 
             // if it  was  approved  anyways,  then  it's  not  makred  as disputed.
                type = Vetting.RES_DISPUTED;
            } else {
                type = Vetting.RES_GOOD;
            }
        } else {
            if (!chads.isEmpty()) {
                type = Vetting.RES_INSUFFICIENT;
            } else {
                type = Vetting.RES_NO_VOTES;
            }
        }
        if (hadDisqualifiedWinner || hadOtherError) {
            type = Vetting.RES_ERROR;
        }
        if (id == -1) {
            // Not an existing vote: insert a new one
            // insert
            vet.insertResult.setInt(2, base_xpath);
            if (resultXpath != -1) {
                vet.insertResult.setInt(3, resultXpath);
            } else {
                vet.insertResult.setNull(3, java.sql.Types.SMALLINT); // no
                                                                        // fallback.
            }
            vet.insertResult.setInt(4, type);
//            System.err.println(this.locale.toString()+"-"+base_xpath+"++");

            int res = vet.insertResult.executeUpdate();
            // no commit
            if (res != 1) {
                throw new RuntimeException(locale + ":" + base_xpath + "=" + resultXpath + " ("
                        + Vetting.typeToStr(type) + ") - insert failed.");
            }
            id = -2; // unknown id
        } else {
            // existing vote: get the old one
            // ... for now, just do an update
            // update CLDR_RESULT set
            // vote_xpath=?,type=?,modtime=CURRENT_TIMESTAMP where id=?

            if (resultXpath != -1) {
                vet.updateResult.setInt(1, resultXpath);
            } else {
                vet.updateResult.setNull(1, java.sql.Types.SMALLINT); // no fallback.
            }
            vet.updateResult.setInt(2, type);
            vet.updateResult.setInt(3, base_xpath);
            vet.updateResult.setString(4, locale.toString());
            int res = vet.updateResult.executeUpdate();
            if (res != 1) {
                throw new RuntimeException(locale + ":" + base_xpath + "@" + id + "=" + resultXpath
                        + " (" + Vetting.typeToStr(type) + ") - update failed.");
            }
        }
        return type;
    }
    
    public Chad getOrgVote(String organization) {
        Integer v = resolver.getOrgVote(VoteResolver.Organization.valueOf(organization));
        if(v==null) return null;
        return chads.get(v);
    }
}
