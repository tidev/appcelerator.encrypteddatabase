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

import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

@Kroll.module(name = "Encrypteddatabase", id = "appcelerator.encrypteddatabase")
public class EncrypteddatabaseModule extends KrollModule
{
	private static final String TAG = "TiDatabase";
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
	@Kroll.getProperty
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

		// Migrate database if necessary.
		final SQLiteDatabaseHook migrationHook = new SQLiteDatabaseHook() {
			public void preKey(SQLiteDatabase database)
			{
			}
			public void postKey(SQLiteDatabase database)
			{
				database.rawExecSQL("PRAGMA cipher_migrate;");
			}
		};

		if (file instanceof TiFileProxy) {
			// File support is read-only for now. The NO_LOCALIZED_COLLATORS
			// flag means the database doesn't have Android metadata (i.e.
			// vanilla)
			TiFileProxy tiFile = (TiFileProxy) file;
			String absolutePath = tiFile.getBaseFile().getNativeFile().getAbsolutePath();
			Log.d(TAG, "Opening database from filesystem: " + absolutePath);

			SQLiteDatabase db = SQLiteDatabase.openDatabase(
				absolutePath, getPassword(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS,
				migrationHook);
			if (db != null) {
				dbp = new TiDatabaseProxy(db);
			} else {
				throw new RuntimeException("SQLiteDatabase.openDatabase() returned null for path: " + absolutePath);
			}
		} else if (file instanceof String) {
			String name = (String) file;
			File dbPath = TiApplication.getInstance().getDatabasePath(name);
			dbPath.getParentFile().mkdirs();
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbPath, getPassword(), null, migrationHook);
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
}
