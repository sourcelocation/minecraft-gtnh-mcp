package dev.gtnhmcp.core;

/** How an amount of something is counted, and how that count reads before its name. */
public enum Unit {

    ITEMS {

        public String count(long amount) {
            return amount == 1 ? "" : amount + "× ";
        }
    },

    MILLIBUCKETS {

        public String count(long amount) {
            return amount + " mB ";
        }
    };

    /** {@code "24× "}, {@code "144 mB "}; empty for a single item. */
    public abstract String count(long amount);
}
