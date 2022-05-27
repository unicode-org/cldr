package org.unicode.cldr.web;

import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.LruMap;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.VettingViewer.UsersChoice;
import org.unicode.cldr.util.VettingViewer.VoteStatus;
import org.unicode.cldr.util.VoteResolver;
import org.unicode.cldr.web.UserRegistry.User;

public class STUsersChoice implements UsersChoice<Organization> {
    private final SurveyMain sm;

    public STUsersChoice(final SurveyMain msm) {
        this.sm = msm;
    }

    @Override
    public String getWinningValueForUsersOrganization(CLDRFile cldrFile, String path, Organization user) {
        CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
        BallotBox<User> ballotBox = getBox(sm, loc);
        return ballotBox.getResolver(path).getOrgVote(user);
    }

    @Override
    public VoteStatus getStatusForUsersOrganization(CLDRFile cldrFile, String path, Organization orgOfUser) {
        CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
        BallotBox<User> ballotBox = getBox(sm, loc);
        return ballotBox.getResolver(path).getStatusForOrganization(orgOfUser);
    }

    @Override
    public VoteResolver<String> getVoteResolver(CLDRLocale loc, String path) {
        BallotBox<User> ballotBox = getBox(sm, loc);
        return ballotBox.getResolver(path);
    }

    @Override
    public boolean userDidVote(int userId, CLDRLocale loc, String path) {
        BallotBox<User> ballotBox = getBox(sm, loc);
        return ballotBox.userDidVote(sm.reg.getInfo(userId), path);
    }

    private LruMap<CLDRLocale, BallotBox<UserRegistry.User>> ballotBoxes = new LruMap<>(8);

    private BallotBox<UserRegistry.User> getBox(SurveyMain sm, CLDRLocale loc) {
        BallotBox<User> box = ballotBoxes.get(loc);
        if (box == null) {
            box = sm.getSTFactory().ballotBoxForLocale(loc);
            ballotBoxes.put(loc, box);
        }
        return box;
    }
}
