/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2020 by Axway, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
package appcelerator.encrypteddatabase;

import android.content.Context;
import android.database.Cursor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;
import org.appcelerator.kroll.KrollInvocation;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.util.TiUrl;

@Kroll.module(name = "Encrypteddatabase", id = "appcelerator.encrypteddatabase")
public class EncrypteddatabaseModule extends KrollModule
{
	private static final String TAG = "TiDatabase";

	@Kroll.constant
	public static final int FIELD_TYPE_UNKNOWN = -1;
	@Kroll.constant
	public static final int FIELD_TYPE_STRING = 0;
	@Kroll.constant
	public static final int FIELD_TYPE_INT = 1;
	@Kroll.constant
	public static final int FIELD_TYPE_FLOAT = 2;
	@Kroll.constant
	public static final int FIELD_TYPE_DOUBLE = 3;

	@Kroll.constant
	public static final int HMAC_SHA1 = TiHashAlgorithmType.SHA1.toTiIntId();
	@Kroll.constant
	public static final int HMAC_SHA256 = TiHashAlgorithmType.SHA256.toTiIntId();
	@Kroll.constant
	public static final int HMAC_SHA512 = TiHashAlgorithmType.SHA512.toTiIntId();

	private String password = null;
	private TiDatabaseSettings dbSettings = new TiDatabaseSettings();

	public EncrypteddatabaseModule()
	{
		super();
		System.loadLibrary("sqlcipher");
	}

	// clang-format off
	@Kroll.getProperty
	@Kroll.method
	public String getPassword()
	// clang-format on
	{
		return (this.password == null) ? TiApplication.getInstance().getAppGUID() : this.password;
	}

	// clang-format off
	@Kroll.setProperty
	@Kroll.method
	public void setPassword(String value)
	// clang-format on
	{
		this.password = value;
	}

	// clang-format off
	@Kroll.setProperty
	public void pageSize(int value)
	// clang-format on
	{
		this.dbSettings.setPageSize(value);
	}

	// clang-format off
	@Kroll.getProperty
	@Kroll.method
	public int getHmacAlgorithm()
	// clang-format on
	{
		return this.dbSettings.getHashAlgorithmType().toTiIntId();
	}

	// clang-format off
	@Kroll.setProperty
	@Kroll.method
	public void setHmacAlgorithm(int value)
	// clang-format on
	{
		TiHashAlgorithmType algorithmType = TiHashAlgorithmType.fromTiIntId(value);
		if (algorithmType == null) {
			String message = "Database property 'hmacAlgorithm' was assigned invalid value: " + value;
			throw new IllegalArgumentException(message);
		}
		this.dbSettings.setHashAlgorithmType(algorithmType);
	}

	// clang-format off
	@Kroll.getProperty
	@Kroll.method
	public int getHmacKdfIterations()
	// clang-format on
	{
		return this.dbSettings.getKdfIterations();
	}

	// clang-format off
	@Kroll.setProperty
	@Kroll.method
	public void setHmacKdfIterations(int value)
	// clang-format on
	{
		if (value < TiDatabaseSettings.MIN_KDF_ITERATIONS) {
			String message = "Database property 'hmacKdfIterations' cannot be set less than "
							 + TiDatabaseSettings.MIN_KDF_ITERATIONS + ". Given: " + value;
			Log.w(TAG, message);
			value = TiDatabaseSettings.MIN_KDF_ITERATIONS;
		}
		this.dbSettings.setKdfIterations(value);
	}

	@Kroll.method
	public TiDatabaseProxy open(String dbName)
	{
		// Validate argument.
		if ((dbName == null) || dbName.isEmpty()) {
			throw new IllegalArgumentException("open() was given a null or empty string.");
		}

		// Attempt to create/open the database.
		File dbFile = TiApplication.getInstance().getDatabasePath(dbName);
		SQLiteDatabase db = openDatabase(dbFile);
		if (db == null) {
			throw new RuntimeException("SQLiteDatabase.openOrCreateDatabase() returned null for name: " + dbName);
		}

		// Return a proxy wrapping the opened database.
		TiDatabaseProxy dbProxy = new TiDatabaseProxy(dbName, db);
		Log.d(TAG, "Opened database: " + dbProxy.getName(), Log.DEBUG_MODE);
		return dbProxy;
	}

	@Kroll.method
	public TiDatabaseProxy install(KrollInvocation invocation, String url, String name) throws IOException
	{
		try {
			// TiContext tiContext = invocation.getTiContext();
			Context ctx = TiApplication.getInstance();
			for (String dbname : ctx.databaseList()) {
				if (dbname.equals(name)) {
					return open(name);
				}
			}
			// open an empty one to get the full path and then close and delete
			// it
			File dbPath = ctx.getDatabasePath(name);

			Log.d(TAG, "db path is = " + dbPath, Log.DEBUG_MODE);
			Log.d(TAG, "db url is = " + url, Log.DEBUG_MODE);

			TiUrl tiUrl = TiUrl.createProxyUrl(invocation.getSourceUrl());
			String path = TiUrl.resolve(tiUrl.baseUrl, url, null);

			TiBaseFile srcDb = TiFileFactory.createTitaniumFile(path, false);

			Log.d(TAG, "new url is = " + url, Log.DEBUG_MODE);

			InputStream is = null;
			OutputStream os = null;

			byte[] buf = new byte[8096];
			int count = 0;
			try {
				is = new BufferedInputStream(srcDb.getInputStream());
				os = new BufferedOutputStream(new FileOutputStream(dbPath));

				while ((count = is.read(buf)) != -1) {
					os.write(buf, 0, count);
				}
			} finally {
				try {
					is.close();
				} catch (Exception ig) {
				}
				try {
					os.close();
				} catch (Exception ig) {
				}
			}

			return open(name);

		} catch (Exception e) {
			String msg = "Error installing database: " + name + " msg=" + e.getMessage();
			Log.e(TAG, msg, e);
			throw e;
		}
	}

	/**
	 * Attempts to open the given database file using the SQLCipher library.
	 * <p>
	 * Will automatically migrate the database if member variable "dbSettings" has different encryption settings
	 * compared to what's currently by the given database. This will always happen when opening a database
	 * created by an older SQLCipher library version. Such as opening a SQLCipher v3 created DB with v4.
	 * @param dbFile The database file to be opened. Can be null.
	 * @return
	 * Returns a reference to the created/opened database if successfully.
	 * Returns null if given a null argument or if unable to open the library.
	 */
	private SQLiteDatabase openDatabase(File dbFile)
	{
		// Validate argument.
		if (dbFile == null) {
			return null;
		}

		// Create the directory tree if it doesn't already exist.
		dbFile.getParentFile().mkdirs();

		// Fetcht the password key used to encrypted/decrypt the database.
		String dbPassword = getPassword();

		// Load existing database's encryption settings from properties file if it exists.
		// Note: This module started creating this file as of v4.1.0.
		TiDatabaseSettings dbOldSettings = null;
		File propertiesFile = new File(dbFile.getAbsolutePath() + ".appc.properties");
		try {
			if (dbFile.exists() && propertiesFile.exists()) {
				try (FileInputStream stream = new FileInputStream(propertiesFile)) {
					Properties properties = new Properties();
					properties.load(stream);
					dbOldSettings = TiDatabaseSettings.from(properties);
				}
			}
		} catch (Exception ex) {
		}

		// If the database file already exists, then attempt to open it.
		// Note: We'll guess at the encryption settings as a fallback in case loaded settings don't work.
		SQLiteDatabase database = null;
		if (dbFile.exists()) {
			// First, attempt to use the settings we stored to properties file.
			if (dbOldSettings != null) {
				database = openDatabase(dbFile, dbPassword, dbOldSettings.toDatabaseHook());
			}

			// If above failed, attempt to open it as a standard unencrypted SQLite 3 database.
			// Note: This allows us to migrate from a JavaScript Ti.Database created file.
			if (database == null) {
				dbOldSettings = null;
				if (isUnencryptedDatabase(dbFile)) {
					database = SQLiteDatabase.openOrCreateDatabase(dbFile, "", null, null, null);
				}
			}

			// If above failed, attempt to open using SQLCipher v4 default settings.
			if (database == null) {
				dbOldSettings = TiDatabaseSettings.fromSQLCipherV4();
				database = openDatabase(dbFile, dbPassword, dbOldSettings.toDatabaseHook());
			}

			// If above failed, attempt to open using SQLCipher v3 default settings.
			if (database == null) {
				dbOldSettings = TiDatabaseSettings.fromSQLCipherV3();
				database = openDatabase(dbFile, dbPassword, dbOldSettings.toDatabaseHook());
			}

			// If above failed, attempt to open using SQLCipher v2 default settings.
			if (database == null) {
				dbOldSettings = TiDatabaseSettings.fromSQLCipherV2();
				database = openDatabase(dbFile, dbPassword, dbOldSettings.toDatabaseHook());
			}

			// If above failed, attempt to open using SQLCipher v4 settings and SHA256.
			if (database == null) {
				dbOldSettings = TiDatabaseSettings.fromSQLCipherV4();
				dbOldSettings.setHashAlgorithmType(TiHashAlgorithmType.SHA256);
				database = openDatabase(dbFile, dbPassword, dbOldSettings.toDatabaseHook());
			}

			// If above failed, attempt to open using SQLCipher v4 settings and SHA1.
			if (database == null) {
				dbOldSettings = TiDatabaseSettings.fromSQLCipherV4();
				dbOldSettings.setHashAlgorithmType(TiHashAlgorithmType.SHA1);
				database = openDatabase(dbFile, dbPassword, dbOldSettings.toDatabaseHook());
			}
		}

		// If we've successfully opened the database above, check if it needs to be migrated.
		if ((database != null) && !dbSettings.equals(dbOldSettings)) {
			// Export the opened database to a separate file using this module's current settings.
			Log.i(TAG, "Migrating database: " + dbFile.getName());
			String dbMigratedFilePath = dbFile.getAbsolutePath() + ".appc.migrated";
			File dbMigratedFile = new File(dbMigratedFilePath);
			database.execSQL("ATTACH DATABASE ? AS migrated KEY ?;", new Object[] { dbMigratedFilePath, dbPassword });
			if (dbOldSettings == null) {
				database.rawExecSQL("PRAGMA migrated.key = '" + dbPassword.replace("'", "''") + "';");
			}
			dbSettings.applyTo(database, "migrated");
			database.rawExecSQL("SELECT sqlcipher_export('migrated');");
			database.rawExecSQL("DETACH DATABASE migrated;");

			// Close the old database so that we can replace it with the migrated one.
			database.close();
			database = null;

			// Replace the old database file with the newly migrated one.
			boolean wasSuccessful = dbFile.delete();
			if (!wasSuccessful) {
				throw new RuntimeException("Database migration failed to delete old file: " + dbFile.getName());
			}
			wasSuccessful = dbMigratedFile.renameTo(dbFile);
			if (!wasSuccessful) {
				throw new RuntimeException("Database migration failed to overwrite file: " + dbFile.getName());
			}
		}

		// Create database or re-open migrated database.
		// Also write its encryption settings to properties file so that we'll know how to re-open it later.
		if (database == null) {
			database = SQLiteDatabase.openOrCreateDatabase(dbFile, dbPassword, null, null, dbSettings.toDatabaseHook());
			if (database != null) {
				try (FileOutputStream stream = new FileOutputStream(propertiesFile)) {
					dbSettings.toProperties().store(stream, "");
					stream.flush();
				} catch (Exception ex) {
					Log.e(TAG, "Failed to write properties file for database: " + dbFile.getName(), ex);
				}
			}
		}

		// Returns a reference to the opened database.
		return database;
	}

	/**
	 * Creates or opens a database at the given location and with the given encryption settings.
	 * This method will test if encryption settings are correct by attempting to read a row.
	 * @param dbFile The file location to create/open the database at. Can be null.
	 * @param dbPassword
	 * The password key used to encrypt/decrypt the database.
	 * Set to empty string to not encrypt the database.
	 * @param dbHook
	 * Optional listener used to provide encryption settings via its postkey() method. Can be null.
	 * @return
	 * Returns a database reference if successfully created/opened and verified that given encryption settings work.
	 * <p>
	 * Returns null if given invalid arguments, failed to access file path, if file is not an encrypted database,
	 * or if given encryption settings fail to decrypt referenced database.
	 */
	private SQLiteDatabase openDatabase(File dbFile, String dbPassword, SQLiteDatabaseHook dbHook)
	{
		SQLiteDatabase database = null;
		boolean isValidDatabase = false;
		try {
			// Create or open the database using encryption settings provided by dbPassword and dbHook.
			// Note: The only way to know if encryption settings are correct is to attempt to read a row.
			database = SQLiteDatabase.openOrCreateDatabase(dbFile, dbPassword, null, null, dbHook);
			if (database != null) {
				try (Cursor cursor = database.rawQuery("PRAGMA user_version;", new String[] {})) {
					isValidDatabase = (cursor != null);
				}
			}
		} catch (Exception ex) {
		} finally {
			// Close database if we've failed to decrypt its contents. (ie: Encryption settings are wrong.)
			if ((database != null) && !isValidDatabase) {
				try {
					database.close();
				} catch (Exception ex) {
				}
				database = null;
			}
		}
		return database;
	}

	/**
	 * Determines if referenced file is a standard unencrypted SQLite 3 database.
	 * This is done by checking for a standard plain text "SQLite format 3" in it.
	 * @param dbFile The file to be analyzed. Can be null.
	 * @return
	 * Returns true if given file references an unencrypted SQLite 3 database.
	 * <p>
	 * Returns false if given null, file not found, or found file is not an unencrypted SQLite 3 database.
	 */
	private boolean isUnencryptedDatabase(File dbFile)
	{
		boolean isUnencrypted = false;
		try (FileInputStream stream = new FileInputStream(dbFile)) {
			final String SQLITE3_HEADER_NAME = "SQLite format 3";
			byte[] byteBuffer = new byte[SQLITE3_HEADER_NAME.length()];
			if (stream.read(byteBuffer) == SQLITE3_HEADER_NAME.length()) {
				String fileHeaderString = new String(byteBuffer, StandardCharsets.UTF_8);
				if (fileHeaderString.equals(SQLITE3_HEADER_NAME)) {
					isUnencrypted = true;
				}
			}
		} catch (Exception ex) {
		}
		return isUnencrypted;
	}
}
