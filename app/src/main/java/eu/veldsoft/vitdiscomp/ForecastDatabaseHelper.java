package eu.veldsoft.vitdiscomp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * Database helper class.
 *
 * @author Todor Balabanov
 */
class ForecastDatabaseHelper extends SQLiteOpenHelper {
	/**
	 * Rates table columns description class.
	 *
	 * @author Todor Balabanov
	 */
	public static abstract class RatesColumns implements BaseColumns {
		public static final String TABLE_NAME = "rates";
		public static final String COLUMN_NAME_SYMBOL = "symbol";
		public static final String COLUMN_NAME_PERIOD = "period";
		public static final String COLUMN_NAME_TIME = "time";
		public static final String COLUMN_NAME_OPEN = "open";
		public static final String COLUMN_NAME_LOW = "low";
		public static final String COLUMN_NAME_HIGH = "high";
		public static final String COLUMN_NAME_CLOSE = "close";
		public static final String COLUMN_NAME_VOLUME = "volume";
	}

	/**
	 * ANN table columns description class.
	 *
	 * @author Todor Balabanov
	 */
	public static abstract class ANNsColumns implements BaseColumns {
		public static final String TABLE_NAME = "anns";
		public static final String COLUMN_NAME_SYMBOL = "symbol";
		public static final String COLUMN_NAME_PERIOD = "period";
		public static final String COLUMN_NAME_NEURONS = "";
		public static final String COLUMN_NAME_ACTIVITIES = "";
		public static final String COLUMN_NAME_WEIGHTS = "";
	}

	/**
	 * Database integer version.
	 */
	public static final int DATABASE_VERSION = 1;

	/**
	 * Database file name.
	 */
	public static final String DATABASE_NAME = "Forecast.db";

	/**
	 * Create rates table SQL patter.
	 */
	private static final String SQL_CREATE_RATES = "CREATE TABLE " + RatesColumns.TABLE_NAME + " (" + RatesColumns._ID
			  + " INTEGER NOT NULL," + RatesColumns.COLUMN_NAME_SYMBOL + " TEXT, " + RatesColumns.COLUMN_NAME_PERIOD
			  + " INTEGER, " + RatesColumns.COLUMN_NAME_TIME + " TEXT, " + RatesColumns.COLUMN_NAME_OPEN + " TEXT, "
			  + RatesColumns.COLUMN_NAME_LOW + " TEXT, " + RatesColumns.COLUMN_NAME_HIGH + " TEXT, "
			  + RatesColumns.COLUMN_NAME_CLOSE + " TEXT, " + RatesColumns.COLUMN_NAME_VOLUME + " TEXT PRIMARY KEY ("
			  + RatesColumns.COLUMN_NAME_SYMBOL + ", " + RatesColumns.COLUMN_NAME_PERIOD + "))";

	/**
	 * Create ANNs table SQL patter.
	 */
	private static final String SQL_CREATE_ANNS = "CREATE TABLE " + ANNsColumns.TABLE_NAME + " (" + ANNsColumns._ID
			  + " INTEGER PRIMARY KEY," + ANNsColumns.COLUMN_NAME_SYMBOL + " TEXT, " + ANNsColumns.COLUMN_NAME_PERIOD
			  + " INTEGER, " + ANNsColumns.COLUMN_NAME_NEURONS + " TEXT, " + ANNsColumns.COLUMN_NAME_ACTIVITIES
			  + " TEXT, " + ANNsColumns.COLUMN_NAME_WEIGHTS + " TEXT, FOREIGN KEY (" + ANNsColumns.COLUMN_NAME_SYMBOL
			  + ") REFERENCES " + RatesColumns.TABLE_NAME + "(" + RatesColumns.COLUMN_NAME_SYMBOL + "), FOREIGN KEY ("
			  + ANNsColumns.COLUMN_NAME_PERIOD + ") REFERENCES " + RatesColumns.TABLE_NAME + "("
			  + RatesColumns.COLUMN_NAME_PERIOD + "))";

	/**
	 * Drop rates table SQL pattern.
	 */
	static final String SQL_DELETE_RATES = "DROP TABLE IF EXISTS " + RatesColumns.TABLE_NAME;

	/**
	 * Drop rates table SQL pattern.
	 */
	static final String SQL_DELETE_ANNS = "DROP TABLE IF EXISTS " + ANNsColumns.TABLE_NAME;

	/**
	 * Constructor.
	 *
	 * @param context Context of database helper usage.
	 */
	public ForecastDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_RATES);
		db.execSQL(SQL_CREATE_ANNS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(SQL_DELETE_ANNS);
		db.execSQL(SQL_DELETE_RATES);
		onCreate(db);
	}
}
