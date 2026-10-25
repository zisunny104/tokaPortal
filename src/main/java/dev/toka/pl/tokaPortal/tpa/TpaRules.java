package dev.toka.pl.tokaPortal.tpa;

/** The numbers and the tiers of /tpa that need no server: how long a request lasts and who may skip the asking. */
public final class TpaRules {
    public static final int REQUEST_MIN = 15, REQUEST_MAX = 30, REQUEST_DEFAULT = 20;
    public static final int FORCE_MIN = 3, FORCE_MAX = 60, FORCE_DEFAULT = 10;
    public static final int INVISIBLE_MIN = 1, INVISIBLE_MAX = 600, INVISIBLE_DEFAULT = 30;
    public static final int DND_DEFAULT_MINUTES = 30;

    /** NORMAL asks the target, FORCE tells the target and goes after a delay, SILENT goes at once without a word. */
    public enum Tier { NORMAL, FORCE, SILENT }

    private TpaRules() {
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /** The seconds a request waits: 15 to 30. */
    public static int requestSeconds(int configured) {
        return clamp(configured, REQUEST_MIN, REQUEST_MAX);
    }

    /** The seconds before a forced teleport happens: 3 to 60. */
    public static int forceDelay(int configured) {
        return clamp(configured, FORCE_MIN, FORCE_MAX);
    }

    /** The seconds the traveller of a silent teleport stays invisible: 1 to 600. */
    public static int invisibleSeconds(int configured) {
        return clamp(configured, INVISIBLE_MIN, INVISIBLE_MAX);
    }

    /** The minutes a normal player may keep do-not-disturb on; at least one. */
    public static int dndMinutes(int configured) {
        return Math.max(1, configured);
    }

    /** The top tier: the operator, the node tokaportal.tpa.silent, or the administrator identity that always had the instant teleport. */
    public static boolean silent(boolean operator, boolean silentNode, boolean adminIdentity) {
        return operator || silentNode || adminIdentity;
    }

    /** The high tier: the node tokaportal.tpa.force, or the sponsor identity that always had the teleport without consent. */
    public static boolean force(boolean forceNode, boolean sponsorIdentity) {
        return forceNode || sponsorIdentity;
    }

    public static Tier tier(boolean silent, boolean force) {
        return silent ? Tier.SILENT : force ? Tier.FORCE : Tier.NORMAL;
    }
}
