package eu.veldsoft.vitosha.trade.communication;

/**
 * Time series fixed time periods.
 *
 * @author Todor Balabanov
 */
public enum TimePeriod {

    /**
     * No time period at all.
     */
    NONE(0, ""),

    /**
     * One minute.
     */
    M1(1, "M1"),

    /**
     * Five minutes.
     */
    M5(5, "M5"),

    /**
     * Fifteen minutes.
     */
    M15(15, "M15"),

    /**
     * Thirty minutes.
     */
    M30(30, "M30"),

    /**
     * One hour.
     */
    H1(60, "H1"),

    /**
     * Four hours.
     */
    H4(240, "H4"),

    /**
     * One day.
     */
    D1(1440, "D1"),

    /**
     * One week.
     */
    W1(10080, "W1"),

    /**
     * One month.
     */
    MN1(43200, "MN1");

    /**
     * Time period as number of minutes.
     */
    private int minutes;

    /**
     * Time period as text description.
     */
    private String name;

    /**
     * Constructor with all parameters.
     *
     * @param minutes Minutes as numbers.
     * @param name    Interval as name.
     */
    private TimePeriod(int minutes, String name) {
        this.minutes = minutes;
        this.name = name;
    }

    /**
     * Factory function for object reference from time interval.
     *
     * @param minutes Time interval in minutes.
     * @return Time period as object.
     * @throws RuntimeException Rise exception if there is no such time interval in minutes.
     */
    public static TimePeriod value(int minutes) throws RuntimeException {
        for (TimePeriod item : TimePeriod.values()) {
            if (item.minutes == minutes) {
                return item;
            }
        }

        // TODO Report exception.

        return NONE;
    }

    /**
     * Factory function for object reference from form interval name.
     *
     * @param name Time period name.
     * @return Time period as object.
     * @throws RuntimeException Rise exception if there is no such time interval in minutes.
     */
    public static TimePeriod value(String name) throws RuntimeException {
        for (TimePeriod item : TimePeriod.values()) {
            if (name.equals(item.name) == true) {
                return item;
            }
        }

        // TODO Report exception.

        return NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return name;
    }
}
