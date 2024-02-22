package org.unicode.cldr.util;

public class VotelessUsersChoice implements VettingViewer.UsersChoice<Organization> {

    final PathHeader.Factory phf = PathHeader.getFactory();
    static final VoterInfoList noUsers = new VoterInfoList();

    @Override
    public String getWinningValueForUsersOrganization(
            CLDRFile cldrFile, String path, Organization org) {
        final CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
        return getVoteResolver(cldrFile, loc, path).getOrgVote(org);
    }

    @Override
    public VoteResolver.VoteStatus getStatusForUsersOrganization(
            CLDRFile cldrFile, String path, Organization org) {
        final CLDRLocale loc = CLDRLocale.getInstance(cldrFile.getLocaleID());
        return getVoteResolver(cldrFile, loc, path).getStatusForOrganization(org);
    }

    @Override
    public VoteResolver<String> getVoteResolver(CLDRFile cldrFile, CLDRLocale loc, String path) {
        final VoteResolver<String> r = new VoteResolver<>(noUsers);
        r.setLocale(loc, phf.fromPath(path));

        final String baileyValue = cldrFile.getBaileyValue(path, null, null);
        r.setBaileyValue(baileyValue);

        final XMLSource xmlSource = cldrFile.dataSource;
        final String baselineValue =
                xmlSource.isHere(path) ? xmlSource.getValueAtDPath(path) : null;
        final VoteResolver.Status status = VoteResolver.calculateStatus(cldrFile, path);
        r.setBaseline(baselineValue, status);

        return r;
    }

    @Override
    public boolean userDidVote(int userId, CLDRLocale loc, String path) {
        return false;
    }

    @Override
    public VoteType getUserVoteType(int userId, CLDRLocale loc, String path) {
        return VoteType.NONE;
    }
}
