package eu.veldsoft.vitdiscomp;

/**
 * 
 * @author Todor Balabanov
 */
enum TimePeriod {

	/**
	 * No time period at all.
	 */
	NO(0, ""),

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
	 * 
	 * @param minutes
	 * @return
	 */
	static TimePeriod get(int minutes) {
		for (TimePeriod item : TimePeriod.values()) {
			if (item.minutes == minutes) {
				return item;
			}
		}

		return null;
	}

	/**
	 * 
	 */
	private int minutes;

	/**
	 * 
	 */
	private String name;

	/**
	 * 
	 * @param minutes
	 * @param name
	 */
	private TimePeriod(int minutes, String name) {
		this.minutes = minutes;
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return name;
	}
}
