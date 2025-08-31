/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2020 by Axway, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
package appcelerator.encrypteddatabase;

import java.util.Properties;
import net.zetetic.database.sqlcipher.SQLiteConnection;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;

/**
 * Stores database settings such as hashing alogorithm, KDF iterations, and SQLite page size.
 * These settings are needed to encrypt/decrypt a database.
 */
public class TiDatabaseSettings
{
	public static final int MIN_KDF_ITERATIONS = 4000;
	public static final int MIN_PAGE_SIZE = 512;
	public static final int MAX_PAGE_SIZE = 65536;

	private static final String PROPERTY_NAME_HASH_ALGORITHM = "cipher_hmac_algorithm";
	private static final String PROPERTY_NAME_KDF_ITERATIONS = "kdf_iter";
	private static final String PROPERTY_NAME_PAGE_SIZE = "cipher_page_size";

	/** The hash algorithm usd such as SHA1, SHA256, or SHA512. */
	private TiHashAlgorithmType hashAlgorithmType;

	/** The KDF (Key Derivation Function) iterations to be used. */
	private int kdfIterations;

	/** SQLite page size in bytes to be used. Must be a power of 2. */
	private int pageSize;

	/** Creates a new object defaulting to SQLCipher v4 library's default encryption settings. */
	public TiDatabaseSettings()
	{
		this.hashAlgorithmType = TiHashAlgorithmType.SHA512;
		this.kdfIterations = 256000;
		this.pageSize = 4096;
	}

	/**
	 * Determines if given database settings match this object's settings.
	 * @param settings Reference to settings to be compared with this object's settings. Can be null.
	 * @return
	 * Returns true if given settings match this object's settings.
	 * <p>
	 * Returns false if given null or if settings do not match.
	 */
	public boolean equals(TiDatabaseSettings settings)
	{
		// Not equal if given null.
		if (settings == null) {
			return false;
		}

		// Not equal if member variables differ.
		if ((hashAlgorithmType != settings.hashAlgorithmType) || (kdfIterations != settings.kdfIterations)
			|| (pageSize != settings.pageSize)) {
			return false;
		}

		// The given settings match this object's settings.
		return true;
	}

	/**
	 * Determines if given object matches this object.
	 * @param value Reference to the object to be compared with this object. Can be null.
	 * @return
	 * Returns true if given object is of type TiDatabaseSettings and its settings match this object.
	 * <p>
	 * Returns false if given null, if argument is not of type TiDatabaseSettings,
	 * or if given object's settings don't match this object's settings.
	 */
	@Override
	public boolean equals(Object value)
	{
		if (value instanceof TiDatabaseSettings) {
			return equals((TiDatabaseSettings) value);
		}
		return false;
	}

	/**
	 * Gets an integer hash code for this object.
	 * @return Returns this object's hash code.
	 */
	@Override
	public int hashCode()
	{
		int hashCode = hashAlgorithmType.hashCode();
		hashCode ^= kdfIterations;
		hashCode ^= pageSize;
		return hashCode;
	}

	/**
	 * Gets the hashing algorithm to be used such as SHA1, SHA256, or SHA512.
	 * @return Returns SHA1, SHA256, or SHA512. Will never return null.
	 */
	public TiHashAlgorithmType getHashAlgorithmType()
	{
		return this.hashAlgorithmType;
	}

	/**
	 * Sets the hashing algorithm to be used by the database.
	 * @param type Hashing algorithm such as SHA1, SHA256, or SHA512. Will default to SHA512 if given null.
	 */
	public void setHashAlgorithmType(TiHashAlgorithmType type)
	{
		if (type == null) {
			type = TiHashAlgorithmType.SHA512;
		}
		this.hashAlgorithmType = type;
	}

	/**
	 * Gets the number of iterations to be used by the KDF (Key Derivation Function).
	 * @return Returns the number of iterations to be used.
	 */
	public int getKdfIterations()
	{
		return this.kdfIterations;
	}

	/**
	 * Sets the number of iteration to be used by the KDF (Key Derivation Function).
	 * The highe the number, the more secure it will be at the cost of performance.
	 * @param value The number iterations to use. Must be greater or equal to "MIN_KDF_ITERATIONS".
	 */
	public void setKdfIterations(int value)
	{
		if (value < MIN_KDF_ITERATIONS) {
			value = MIN_KDF_ITERATIONS;
		}
		this.kdfIterations = value;
	}

	/**
	 * Gets the SQLite page size in bytes to be used when storing data to file.
	 * @return Returns the SQLite page size in bytes.
	 */
	public int getPageSize()
	{
		return this.pageSize;
	}

	/**
	 * Sets the SQLite page size in bytes to be used when storing data to file.
	 * @param value The size in bytes. Must be at least 512 bytes and a power of 2.
	 */
	public void setPageSize(int value)
	{
		// Make sure argument is a power of 2 and within SQLCipher's documented range.
		if (value < MIN_PAGE_SIZE) {
			value = MIN_PAGE_SIZE;
		} else if (value > MAX_PAGE_SIZE) {
			value = MAX_PAGE_SIZE;
		} else {
			for (int nextValidValue = MIN_PAGE_SIZE; nextValidValue <= MAX_PAGE_SIZE; nextValidValue <<= 1) {
				if (value <= nextValidValue) {
					if (value < nextValidValue) {
						value = nextValidValue;
					}
					break;
				}
			}
		}
		this.pageSize = value;
	}

	/**
	 * Creates an event hook that can be passed to the SQLCipher database class' open methods.
	 * <p>
	 * Used to apply this object's settings to a database while it is being opened.
	 * These settings are needed to decrypt it correctly or else you'll be unable to query it.
	 * @return Returns a SQLCipher event hook object.
	 */
	public SQLiteDatabaseHook toDatabaseHook()
	{
		return new SQLiteDatabaseHook() {
			@Override
			public void preKey(SQLiteConnection connection)
			{
			}

			@Override
			public void postKey(SQLiteConnection connection)
			{
			}
		};
	}

	/**
	 * Applies this object's settings to the given database via SQL PRAGMA statements.
	 * @param database
	 * The database this object's settings will be applied to. Database must be opened.
	 * If given null, then this method will do nothing.
	 */
	public void applyTo(SQLiteDatabase database)
	{
		applyTo(database, "");
	}

	/**
	 * Applies this object's settings to the given database via SQL PRAGMA statements.
	 * @param database
	 * The database this object's settings will be applied to. Database must be opened.
	 * If given null, then this method will do nothing.
	 * @param dbName
	 * The name of the attached database to apply these settings to. Only applicable if you have
	 * at least 2 databases attached.
	 * <p>
	 * Can be null or empty string, in which case the settings are applied globally.
	 */
	public void applyTo(SQLiteDatabase database, String dbName)
	{
		// Validate.
		if (database == null) {
			return;
		}

		// Make sure database name argument is a valid string.
		// Append a "." to it so it can be used like this: "PRAGMA MyDatabase.cipher_page_size = 4096"
		if (dbName != null) {
			dbName = dbName.trim();
			if (!dbName.isEmpty()) {
				dbName += ".";
			}
		} else {
			dbName = "";
		}

		// Apply this object's encryption settings to the given database.
		String hmacAlgorithmStringId = this.hashAlgorithmType.toCipherHmacStringId();
		String kdfAlgorithmStringId = this.hashAlgorithmType.toCipherKdfStringId();
		database.rawExecSQL("PRAGMA " + dbName + "cipher_page_size = " + this.pageSize + ";");
		database.rawExecSQL("PRAGMA " + dbName + "kdf_iter = " + this.kdfIterations + ";");
		database.rawExecSQL("PRAGMA " + dbName + "cipher_hmac_algorithm = " + hmacAlgorithmStringId + ";");
		database.rawExecSQL("PRAGMA " + dbName + "cipher_kdf_algorithm = " + kdfAlgorithmStringId + ";");
	}

	/**
	 * Creates a properties object storing this object's settings.
	 * The returned properties are intended to be saved to file and later restored via this class' from() method.
	 * @return Returns a Java properties object storing this object's settings.
	 */
	public Properties toProperties()
	{
		Properties properties = new Properties();
		properties.setProperty(PROPERTY_NAME_HASH_ALGORITHM, this.hashAlgorithmType.toCipherHmacStringId());
		properties.setProperty(PROPERTY_NAME_KDF_ITERATIONS, Integer.toString(this.kdfIterations));
		properties.setProperty(PROPERTY_NAME_PAGE_SIZE, Integer.toString(this.pageSize));
		return properties;
	}

	/**
	 * Creates a new TiDatabaseSetting instance initialized with the settings loaded from given properties.
	 * Given properties are expected to come from this class' toProperties() method.
	 * @param properties The properties to load settings from. Can be null.
	 * @return
	 * Returns a new instance initialized with the setting loaded from given properties.
	 * Returns null if given a null argument.
	 */
	public static TiDatabaseSettings from(Properties properties)
	{
		// Validate argument.
		if (properties == null) {
			return null;
		}

		// Load settings from give properties.
		TiDatabaseSettings settings = new TiDatabaseSettings();
		{
			String value = properties.getProperty(PROPERTY_NAME_HASH_ALGORITHM);
			TiHashAlgorithmType algorithmType = TiHashAlgorithmType.fromCipherHmacStringId(value);
			if (algorithmType != null) {
				settings.setHashAlgorithmType(algorithmType);
			}
		}
		{
			Integer value = parseInt(properties.getProperty(PROPERTY_NAME_KDF_ITERATIONS));
			if (value != null) {
				settings.setKdfIterations(value.intValue());
			}
		}
		{
			Integer value = parseInt(properties.getProperty(PROPERTY_NAME_PAGE_SIZE));
			if (value != null) {
				settings.setPageSize(value.intValue());
			}
		}
		return settings;
	}

	/**
	 * Creates a new instance initialized to use SQLCipher v4 library's default settings.
	 * @return Returns a new database settings object.
	 */
	public static TiDatabaseSettings fromSQLCipherV4()
	{
		return new TiDatabaseSettings();
	}

	/**
	 * Creates a new instance initialized to use SQLCipher v3 library's default settings.
	 * @return Returns a new database settings object.
	 */
	public static TiDatabaseSettings fromSQLCipherV3()
	{
		TiDatabaseSettings settings = new TiDatabaseSettings();
		settings.setHashAlgorithmType(TiHashAlgorithmType.SHA1);
		settings.setKdfIterations(64000);
		settings.setPageSize(1024);
		return settings;
	}

	/**
	 * Creates a new instance initialized to use SQLCipher v2 library's default settings.
	 * @return Returns a new database settings object.
	 */
	public static TiDatabaseSettings fromSQLCipherV2()
	{
		TiDatabaseSettings settings = new TiDatabaseSettings();
		settings.setHashAlgorithmType(TiHashAlgorithmType.SHA1);
		settings.setKdfIterations(4000);
		settings.setPageSize(1024);
		return settings;
	}

	/**
	 * Safely parses an integer from the given string without exceptions.
	 * @param text The string to parse. Can be null or invalid.
	 * @return
	 * Returns an Integer object providing the value parsed from the string.
	 * Returns null if argument is null, empty, or contains non-numeric characters.
	 */
	private static Integer parseInt(String text)
	{
		try {
			if ((text != null) && !text.isEmpty()) {
				return Integer.valueOf(text);
			}
		} catch (Exception ex) {
		}
		return null;
	}
}
