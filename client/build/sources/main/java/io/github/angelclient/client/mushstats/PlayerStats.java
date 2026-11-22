package io.github.angelclient.client.mushstats;

public class PlayerStats {
    private final String username;
    private final int wins;
    private final int losses;
    private final int finalKills;
    private final int fkdr;
    private final int bedsBroken;
    private final int level;
    private final long xp;
    private final String levelFormatString;
    private final int levelColorInt;
    private final int winstreak;
    private final String accountType;
    private final long firstLogin;
    private final boolean isDataAvailable;

    public PlayerStats(String username, int wins, int losses, int finalKills, int fkdr, int bedsBroken, int level, long xp, String levelFormatString, int levelColorInt, int winstreak, String accountType, long firstLogin, boolean isDataAvailable) {
        this.username = username;
        this.wins = wins;
        this.losses = losses;
        this.finalKills = finalKills;
        this.fkdr = fkdr;
        this.bedsBroken = bedsBroken;
        this.level = level;
        this.xp = xp;
        this.levelFormatString = levelFormatString;
        this.levelColorInt = levelColorInt;
        this.winstreak = winstreak;
        this.accountType = accountType;
        this.firstLogin = firstLogin;
        this.isDataAvailable = isDataAvailable;
    }

    public PlayerStats(String username, int wins, int losses, int finalKills, int fkdr, int bedsBroken, int level, long xp, String levelFormatString, int levelColorInt, int winstreak, String accountType, long firstLogin) {
        this(username, wins, losses, finalKills, fkdr, bedsBroken, level, xp, levelFormatString, levelColorInt, winstreak, accountType, firstLogin, true);
    }

    public String getUsername() {
        return this.username;
    }

    public int getWins() {
        return this.wins;
    }

    public int getLosses() {
        return this.losses;
    }

    public int getFinalKills() {
        return this.finalKills;
    }

    public int getFkdr() {
        return this.fkdr;
    }

    public int getBedsBroken() {
        return this.bedsBroken;
    }

    public int getLevel() {
        return this.level;
    }

    public long getXP() {
        return this.xp;
    }

    public String getLevelFormatString() {
        return this.levelFormatString;
    }

    public int getLevelColorInt() {
        return this.levelColorInt;
    }

    public int getWinstreak() {
        return this.winstreak;
    }

    public String getAccountType() {
        return this.accountType;
    }

    public long getFirstLogin() {
        return this.firstLogin;
    }

    public boolean isDataAvailable() {
        return this.isDataAvailable;
    }

    public float getFKDRValue() {
        return this.fkdr / 100.0f;
    }

    public float getWLRValue() {
        if (this.losses == 0) {
            if (this.wins > 0) {
                return this.wins;
            }
            return 0.0f;
        }
        return this.wins / this.losses;
    }
}
