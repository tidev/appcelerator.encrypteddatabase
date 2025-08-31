/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2016 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
package appcelerator.encrypteddatabase;

import android.database.Cursor;
import android.database.SQLException;
import android.os.Build;
import java.util.HashMap;
import net.zetetic.database.sqlcipher.SQLiteCursor;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.util.TiConvert;

@Kroll.proxy(parentModule = EncrypteddatabaseModule.class)
public class TiResultSetProxy extends KrollProxy
{
	private static final String TAG = "TiResultSet";

	protected Cursor rs;
	protected String lastException;
	protected HashMap<String, Integer> columnNames; // workaround case-sensitive
													// matching in Google's
													// implementation

	public TiResultSetProxy(Cursor rs)
	{
		super();

		this.rs = rs;
		String[] names = rs.getColumnNames();
		this.columnNames = new HashMap<String, Integer>(names.length);
		for (int i = 0; i < names.length; i++) {
			columnNames.put(names[i].toLowerCase(), i);
		}
	}

	@Kroll.method
	public void close()
	{
		if (rs != null && !rs.isClosed()) {
			Log.d(TAG, "Closing database cursor", Log.DEBUG_MODE);
			rs.close();
		} else {
			Log.w(TAG, "Calling close on a closed cursor.", Log.DEBUG_MODE);
		}
	}

	@Kroll.method
	public Object field(Object[] args)
	{
		return internalGetField(args);
	}

	@Kroll.method
	public Object getField(Object[] args)
	{
		return internalGetField(args);
	}

	private Object internalGetField(Object[] args)
	{
		int index = -1;
		int type = EncrypteddatabaseModule.FIELD_TYPE_UNKNOWN;
		if (args.length >= 1) {
			if (args[0] instanceof Number) {
				index = TiConvert.toInt(args[0]);
			} else {
				(new IllegalArgumentException("Expected int column index as first parameter was "
											  + args[0].getClass().getSimpleName()))
					.printStackTrace();
				throw new IllegalArgumentException("Expected int column index as first parameter was "
												   + args[0].getClass().getSimpleName());
			}
		}
		if (args.length == 2) {
			if (args[1] instanceof Number) {
				type = TiConvert.toInt(args[1]);
			} else {
				throw new IllegalArgumentException("Expected int field type as second parameter was "
												   + args[1].getClass().getSimpleName());
			}
		}

		return internalGetField(index, type);
	}

	private Object internalGetField(int index, int type)
	{
		if (rs == null) {
			Log.w(TAG, "Attempted to get field value when no result set is available.");
			return null;
		}
		boolean outOfBounds = (index >= rs.getColumnCount());
		Object result = null;
		boolean fromString = false;

		try {
			if (rs instanceof SQLiteCursor) {
				final SQLiteCursor cursor = (SQLiteCursor) rs;
				final int cursorType = cursor.getType(index);

				switch (cursorType) {
					case Cursor.FIELD_TYPE_NULL:
						result = null;
						break;
					case Cursor.FIELD_TYPE_INTEGER:
						// No long field type, longs are set as integer type.
						// Using getLong() to preserve accuracy in these cases.
						result = cursor.getLong(index);
						break;
					case Cursor.FIELD_TYPE_FLOAT:
						// No double field type, doubles are set as float type.
						// Using getDouble() to preserve accuracy in these cases.
						result = cursor.getDouble(index);
						break;
					case Cursor.FIELD_TYPE_BLOB:
						result = TiBlob.blobFromData(cursor.getBlob(index));
						break;
					case Cursor.FIELD_TYPE_STRING:
					default:
						fromString = true;
				}
			} else {
				fromString = true;
			}
			if (fromString) {
				result = rs.getString(index);
			}
			if (outOfBounds && Build.VERSION.SDK_INT >= 11) {
				// TIMOB-4515: Column number doesn't exist, yet no exception
				// occurred. This is known to happen in Honeycomb. So
				// we'll throw instead. We throw the same exception type that
				// Android would.
				throw new IllegalStateException("Requested column number " + index + " does not exist");
			}
		} catch (RuntimeException e) {
			// Both SQLException and IllegalStateException (exceptions known to occur
			// in this block) are RuntimeExceptions and since we anyway re-throw
			// and log the same error message, we're just catching all RuntimeExceptions.
			Log.e(TAG, "Exception getting value for column " + index + ": " + e.getMessage(), e);
			throw e;
		}

		switch (type) {
			case EncrypteddatabaseModule.FIELD_TYPE_STRING:
				if (!(result instanceof String)) {
					result = TiConvert.toString(result);
				}
				break;
			case EncrypteddatabaseModule.FIELD_TYPE_INT:
				if (!(result instanceof Integer) && !(result instanceof Long)) {
					result = TiConvert.toInt(result);
				}
				break;
			case EncrypteddatabaseModule.FIELD_TYPE_FLOAT:
				if (!(result instanceof Float)) {
					result = TiConvert.toFloat(result);
				}
				break;
			case EncrypteddatabaseModule.FIELD_TYPE_DOUBLE:
				if (!(result instanceof Double)) {
					result = TiConvert.toDouble(result);
				}
				break;
		}
		return result;
	}

	@Kroll.method
	public Object fieldByName(Object[] args)
	{
		return internalGetFieldByName(args);
	}

	@Kroll.method
	public Object getFieldByName(Object[] args)
	{
		return internalGetFieldByName(args);
	}

	private Object internalGetFieldByName(Object[] args)
	{
		String name = null;
		int type = EncrypteddatabaseModule.FIELD_TYPE_UNKNOWN;
		if (args.length >= 1) {
			if (args[0] instanceof String) {
				name = (String) args[0];
			} else {
				throw new IllegalArgumentException("Expected string column name as first parameter"
												   + args[0].getClass().getSimpleName());
			}
		}
		if (args.length == 2) {
			if (args[1] instanceof Number) {
				type = TiConvert.toInt(args[1]);
			} else {
				throw new IllegalArgumentException("Expected int field type as second parameter"
												   + args[1].getClass().getSimpleName());
			}
		}

		return internalGetFieldByName(name, type);
	}

	private Object internalGetFieldByName(String fieldName, int type)
	{
		Object result = null;
		if (rs != null) {
			try {
				Integer ndx = columnNames.get(fieldName.toLowerCase());
				if (ndx != null)
					result = internalGetField(ndx.intValue(), type);
			} catch (SQLException e) {
				String msg = "Field name " + fieldName + " not found. msg=" + e.getMessage();
				Log.e(TAG, msg);
				throw e;
			}
		}

		return result;
	}

	// clang-format off
	@Kroll.getProperty
	@Kroll.method
	public int getFieldCount()
	// clang-format on
	{
		if (rs != null) {
			try {
				return rs.getColumnCount();
			} catch (SQLException e) {
				Log.e(TAG, "No fields exist");
				throw e;
			}
		}

		return 0;
	}

	@Kroll.method
	public String fieldName(int index)
	{
		return getFieldName(index);
	}

	@Kroll.method
	public String getFieldName(int index)
	{
		if (rs != null) {
			try {
				return rs.getColumnName(index);
			} catch (SQLException e) {
				Log.e(TAG, "No column at index: " + index);
				throw e;
			}
		}
		return null;
	}

	// clang-format off
	@Kroll.getProperty
	@Kroll.method
	public int getRowCount()
	// clang-format on
	{
		if (rs != null) {
			return rs.getCount();
		}

		return 0;
	}

	@Kroll.method
	public boolean isValidRow()
	{
		boolean valid = false;
		if (rs != null && !rs.isClosed() && !rs.isAfterLast()) {
			valid = true;
		}
		return valid;
	}

	@Kroll.method
	public boolean next()
	{
		if (isValidRow()) {
			return rs.moveToNext();
		} else {
			Log.w(TAG, "Ignoring next, current row is invalid.");
		}
		return false;
	}
}
