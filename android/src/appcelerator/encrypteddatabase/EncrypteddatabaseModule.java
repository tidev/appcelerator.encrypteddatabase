/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
package appcelerator.encrypteddatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.appcelerator.kroll.KrollInvocation;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiFileProxy;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.util.TiUrl;

import android.content.Context;
import android.content.SharedPreferences;

import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

@Kroll.module(name = "Encrypteddatabase", id = "appcelerator.encrypteddatabase")
public class EncrypteddatabaseModule extends KrollModule
{
	private static final String TAG = "TiDatabase";
	private static final String MODULE_PREFERENCES_NAME = "appcelerator.encrypteddatabase";
	private String password = null;

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

	public EncrypteddatabaseModule()
	{
		super();
		SQLiteDatabase.loadLibs(TiApplication.getAppCurrentActivity());
	}

	// clang-format off
	@Kroll.getProperty
	@Kroll.method
	public String getPassword()
	// clang-format on
	{
		return password == null ? TiApplication.getInstance().getAppGUID() : password;
	}

	// clang-format off
	@Kroll.setProperty
	@Kroll.method
	public void setPassword(String value)
	// clang-format on
	{
		password = value;
	}

	@Kroll.method
	public TiDatabaseProxy open(Object file)
	{
		// Attempt to create/open the given database file/name.
		TiDatabaseProxy dbp = null;
		if (file instanceof TiFileProxy) {
			// File support is read-only for now. The NO_LOCALIZED_COLLATORS
			// flag means the database doesn't have Android metadata (i.e.
			// vanilla)
			TiFileProxy tiFile = (TiFileProxy) file;
			String absolutePath = tiFile.getBaseFile().getNativeFile().getAbsolutePath();
			Log.d(TAG, "Opening database from filesystem: " + absolutePath);

			SQLiteDatabase db = SQLiteDatabase.openDatabase(
				absolutePath, getPassword(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS,
				new MigrationHook());
			if (db != null) {
				dbp = new TiDatabaseProxy(db);
			} else {
				throw new RuntimeException("SQLiteDatabase.openDatabase() returned null for path: " + absolutePath);
			}
		} else if (file instanceof String) {
			String name = (String) file;
			File dbPath = TiApplication.getInstance().getDatabasePath(name);
			dbPath.getParentFile().mkdirs();
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbPath, getPassword(), null, new MigrationHook());
			if (db != null) {
				dbp = new TiDatabaseProxy(name, db);
			} else {
				throw new RuntimeException("SQLiteDatabase.openOrCreateDatabase() returned null for name: " + name);
			}
		} else if (file != null) {
			throw new IllegalArgumentException("open() argument must be of type 'String' or 'File'.");
		} else {
			throw new IllegalArgumentException("open() was given a null argument.");
		}

		// Return a proxy to the opened database.
		Log.d(TAG, "Opened database: " + dbp.getName(), Log.DEBUG_MODE);
		return dbp;
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

		} catch (SQLException e) {
			String msg = "Error installing database: " + name + " msg=" + e.getMessage();
			Log.e(TAG, msg, e);
			throw e;
		} catch (IOException e) {
			String msg = "Error installing database: " + name + " msg=" + e.getMessage();
			Log.e(TAG, msg, e);
			throw e;
		}
	}

	private static class MigrationHook implements SQLiteDatabaseHook {
		/**
		 * Called immediately before opening the database.
		 * @param database The database being opened.
		 */
		@Override
		public void preKey(SQLiteDatabase database)
		{
		}

		/**
		 * Called immediately after opening the database.
		 * @param database The database that was opened.
		 */
		@Override
		public void postKey(SQLiteDatabase database)
		{
			// Fetch the database's file path.
			String dbFilePath = database.getPath();
			if (dbFilePath == null) {
				dbFilePath = "";
			}

			// Fetch the library version we last opened the database with.
			SharedPreferences preferencesReader = null;
			String lastVersionString = null;
			try {
				preferencesReader = TiApplication.getInstance().getSharedPreferences(
					MODULE_PREFERENCES_NAME, Context.MODE_PRIVATE);
				lastVersionString = preferencesReader.getString(dbFilePath, null);
			} catch (Exception ex) {
				Log.e(TAG, "Failed to read version from shared preferences.", ex);
			}

			// Determine if we need to migrate the database.
			boolean isMigrationNeeded = false;
			if (lastVersionString == null) {
				// Version string not found in preferences.
				// This might be the first time we tracked the version or DB file was moved/copied.
				isMigrationNeeded = true;
			} else if (compareMajorVersionStrings(lastVersionString, SQLiteDatabase.SQLCIPHER_ANDROID_VERSION) < 0) {
				// The database file is older than the library we're using.
				isMigrationNeeded = true;
			}

			// Migrate the database file if needed.
			// Note: This is expensive since it involves creating a temporary database file.
			if (isMigrationNeeded) {
				database.rawExecSQL("PRAGMA cipher_migrate;");
			}

			// Store the library version we're using for the next time we open it.
			if ((preferencesReader != null) && (dbFilePath != null) && !dbFilePath.isEmpty()) {
				try {
					SharedPreferences.Editor preferencesWriter = preferencesReader.edit();
					preferencesWriter.putString(dbFilePath, SQLiteDatabase.SQLCIPHER_ANDROID_VERSION);
					preferencesWriter.commit();
				} catch (Exception ex) {
					Log.e(TAG, "Failed to write version to shared preferences.", ex);
				}
			}
		}

		/**
		 * Compares the major version components of the 2 given version strings.
		 * @param versionString1 The version string to be compared with versionString2. Can be null.
		 * @param versionString2 The version string to be compared with versionString1. Can be null.
		 * @return
		 * Returns zero if arguments match.
		 * Returns a positive number if 1st argument is greater than 2nd argument.
		 * Returns a negative number if 1st argument is less than 2nd argument.
		 */
		private static int compareMajorVersionStrings(String versionString1, String versionString2)
		{
			// Compare references first.
			if (versionString1 == versionString2) {
				return 0;
			} else if ((versionString1 != null) && (versionString2 == null)) {
				return 1;
			} else if ((versionString1 == null) && (versionString2 != null)) {
				return -1;
			}

			// Compare major version components between the 2 version strings.
			return parseMajorVersionIntFrom(versionString1) - parseMajorVersionIntFrom(versionString2);
		}

		/**
		 * Extracts the major version components of the given version string and returns it as an integer.
		 * @param versionString The version string to be extracted. Can be null.
		 * @return Returns the extracted major version value or zero if failed to extract.
		 */
		private static int parseMajorVersionIntFrom(String versionString)
		{
			int value = 0;
			try {
				if (versionString != null) {
					int index = versionString.indexOf('.');
					if (index >= 0) {
						versionString = versionString.substring(0, index);
					}
					if (!versionString.isEmpty()) {
						value = Integer.parseInt(versionString);
					}
				}
			} catch (Exception ex) {
			}
			return value;
		}
	};
}
