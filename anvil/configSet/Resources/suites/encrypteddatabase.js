/*
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

module.exports = new function() {
	var finish;
	var valueOf;
	var EncryptedDatabase;
	this.init = function(testUtils) {
		finish = testUtils.finish;
		valueOf = testUtils.valueOf;
		EncryptedDatabase = require('appcelerator.encrypteddatabase');
		EncryptedDatabase.password = 'this is my awesome password';
	};

	this.name = 'encrypteddatabase';

	// ---------------------------------------------------------------
	// DB Proxy Tests taken from Ti.Database
	// ---------------------------------------------------------------

	this.testModuleMethodsAndConstants = function(testRun) {
		valueOf(testRun, EncryptedDatabase).shouldNotBeNull();
		valueOf(testRun, EncryptedDatabase).shouldBeObject();
		valueOf(testRun, EncryptedDatabase.open).shouldBeFunction();
		valueOf(testRun, EncryptedDatabase.install).shouldBeFunction();
		
		valueOf(testRun, EncryptedDatabase.FIELD_TYPE_STRING).shouldNotBeNull();
		valueOf(testRun, EncryptedDatabase.FIELD_TYPE_INT).shouldNotBeNull();
		valueOf(testRun, EncryptedDatabase.FIELD_TYPE_FLOAT).shouldNotBeNull();
		valueOf(testRun, EncryptedDatabase.FIELD_TYPE_DOUBLE).shouldNotBeNull();

		finish(testRun);
	}

	this.testDatabaseMethods = function(testRun) {
		var db = EncryptedDatabase.open("Test");
		try {
			valueOf(testRun, db).shouldNotBeNull();
			valueOf(testRun, db).shouldBeObject();
			valueOf(testRun, db.close).shouldBeFunction();
			valueOf(testRun, db.execute).shouldBeFunction();
			valueOf(testRun, db.getLastInsertRowId).shouldBeFunction();
			valueOf(testRun, db.getName).shouldBeFunction();
			valueOf(testRun, db.getRowsAffected).shouldBeFunction();
			valueOf(testRun, db.remove).shouldBeFunction();
		
			// Properties
			valueOf(testRun, db.lastInsertRowId).shouldBeNumber();
			valueOf(testRun, db.name).shouldBeString();
			valueOf(testRun, db.name).shouldBe("Test");
			valueOf(testRun, db.rowsAffected).shouldBeNumber();
		} finally {
			db.close();
		}

		finish(testRun);
	}

	// https://appcelerator.lighthouseapp.com/projects/32238-titanium-mobile/tickets/2147-android-pragma-and-non-select-statements-return-null-from-tidatabasedbexecute-instead-of-resultset
	this.testDatabaseLH2147 = function(testRun) {
		var db = EncryptedDatabase.open("Test");
		try {
			valueOf(testRun, db).shouldNotBeNull();
		
			var rs = db.execute("drop table if exists Test");
			valueOf(testRun, rs).shouldBeNull();

			rs = db.execute("create table if not exists Test(row text)");
			valueOf(testRun, rs).shouldBeNull();
		
			rs = db.execute("pragma table_info(Test)");
			valueOf(testRun, rs).shouldNotBeNull();
			valueOf(testRun, rs.fieldCount).shouldBeGreaterThan(0);
			rs.close();
		
			rs = db.execute("select * from Test");
			valueOf(testRun, rs).shouldNotBeNull();
			valueOf(testRun, rs.getFieldCount()).shouldBe(1);
			valueOf(testRun, rs.rowCount).shouldBe(0);
			rs.close();
		} finally {
			db.close();
			db.remove();
		}
		
		var f = Ti.Filesystem.getFile("file:///data/data/org.appcelerator.titanium.testharness/databases/Test");
		valueOf(testRun, f.exists()).shouldBeFalse();

		finish(testRun);
	}

	this.testDatabaseInsert = function(testRun) {
		var db = EncryptedDatabase.open("Test");
		try {
			valueOf(testRun, db).shouldNotBeNull();
		
			var rs = db.execute("drop table if exists Test");
			valueOf(testRun, rs).shouldBeNull();

			rs = db.execute("create table if not exists Test(row text)");
			valueOf(testRun, rs).shouldBeNull();

			db.execute("insert into Test(row) values(?)", "My TestRow");
		
			rs = db.execute("select * from Test");
			valueOf(testRun, rs).shouldNotBeNull();
			valueOf(testRun, rs.isValidRow()).shouldBe(true);
			valueOf(testRun, rs.getFieldCount()).shouldBe(1);
			valueOf(testRun, rs.rowCount).shouldBe(1);
			valueOf(testRun, rs.getField(0)).shouldBe("My TestRow");
			rs.close();
		} finally {
			db.close();
			db.remove();
		}
		
		var f = Ti.Filesystem.getFile("file:///data/data/org.appcelerator.titanium.testharness/databases/Test");
		valueOf(testRun, f.exists()).shouldBeFalse();

		finish(testRun);
	}

	this.testDatabaseCount = function(testRun) {
		var testRowCount = 100;
		var db = EncryptedDatabase.open('Test');
		try {
			valueOf(testRun, db).shouldNotBeNull();
			
			var rs = db.execute("drop table if exists data");
			valueOf(testRun, rs).shouldBeNull();
			
			db.execute('CREATE TABLE IF NOT EXISTS data (id INTEGER PRIMARY KEY, val TEXT)');
			for (var i = 1; i <= testRowCount; i++) {
			    db.execute('INSERT INTO data (val) VALUES(?)','our value:' + i);
			}

		    rs = db.execute("SELECT * FROM data");
		    var rowCount = rs.rowCount;
		    var realCount = 0;
		    while (rs.isValidRow()) {
		        realCount += 1;
		        rs.next();
		    }
			rs.close();
			
		    valueOf(testRun, realCount).shouldBe(testRowCount);
		    valueOf(testRun, rowCount).shouldBe(testRowCount);
		    valueOf(testRun, rowCount).shouldBe(realCount);
		} finally {
			db.close();
			db.remove();
		}

		finish(testRun);
	}

	this.testDatabaseRollback = function(testRun) {
		var db = EncryptedDatabase.open('Test');
		var testRowCount = 30;
		try {
			valueOf(testRun, db).shouldNotBeNull();
			
			var rs = db.execute("drop table if exists data");
			valueOf(testRun, rs).shouldBeNull();
			
			db.execute('CREATE TABLE IF NOT EXISTS data (id INTEGER PRIMARY KEY, val TEXT)');
			
			db.execute('BEGIN TRANSACTION');
			for (var i = 1; i <= testRowCount; i++) {
			    db.execute('INSERT INTO data (val) VALUES(?)','our value:' + i);
			}
			rs = db.execute("SELECT * FROM data");
		    valueOf(testRun, rs.rowCount).shouldBe(testRowCount);
			rs.close();
			
			db.execute('ROLLBACK TRANSACTION');
		
			rs = db.execute("SELECT * FROM data");
			valueOf(testRun, rs.rowCount).shouldBe(0);
			rs.close();
		
			db.execute('drop table if exists data');
		} finally {
			db.close();
			db.remove();
		}

		finish(testRun);
	}

	this.testDatabaseSavepointRollback = function(testRun) {
		var db = EncryptedDatabase.open('Test');
		var testRowCount = 30;
		try {
			valueOf(testRun, db).shouldNotBeNull();
			
			var rs = db.execute("drop table if exists data");
			valueOf(testRun, rs).shouldBeNull();
			
			// Devices with Android API Levels before 8 don't support savepoints causing
			// a false failure on those devices. Try and detect and only do
			// this complex test if savepoints work.
			var savepointSupported = true;
			try {
				db.execute('SAVEPOINT test');
				db.execute('RELEASE SAVEPOINT test');

				// Android 4.1 introduced a bug with savepoint rollbacks:
				// http://code.google.com/p/android/issues/detail?id=38706
				if (Ti.Platform.osname == 'android' && Ti.Platform.Android.API_LEVEL >= 16) {
					savepointSupported = false;
				}
			} catch (E) {
				savepointSupported = false;
			}

			if (savepointSupported) {
				db.execute('BEGIN DEFERRED TRANSACTION');
				db.execute('CREATE TABLE IF NOT EXISTS data (id INTEGER PRIMARY KEY, val TEXT)');
				db.execute('SAVEPOINT FOO');
				for (var i = 1; i <= testRowCount; i++) {
				    db.execute('INSERT INTO data (val) VALUES(?)','our value:' + i);
				}
				db.execute('ROLLBACK TRANSACTION TO SAVEPOINT FOO');
				db.execute('COMMIT TRANSACTION');
			
				rs = db.execute("SELECT * FROM data");
				valueOf(testRun, rs.rowCount).shouldBe(0);
				rs.close();
			
				db.execute('BEGIN TRANSACTION');
				db.execute('drop table if exists data');
				db.execute('ROLLBACK TRANSACTION');
			
				rs = db.execute("SELECT * FROM data");
				valueOf(testRun, rs).shouldNotBeNull();
				rs.close();
			}
		} finally {
			db.close();
			db.remove();
		}

		finish(testRun);
	}

	// https://appcelerator.lighthouseapp.com/projects/32238-titanium-mobile/tickets/2917-api-doc-dbexecute
	this.testDatabaseLH2917 = function(testRun) {
		var db = EncryptedDatabase.open('Test'),
		    rowCount = 10,
				resultSet, i, counter;

		
		valueOf(testRun, db).shouldBeObject();
		valueOf(testRun, resultSet).shouldBeUndefined();
		valueOf(testRun, i).shouldBeUndefined();
		valueOf(testRun, counter).shouldBeUndefined();

		try {
			db.execute('CREATE TABLE IF NOT EXISTS stuff (id INTEGER, val TEXT)');
			db.execute('DELETE FROM stuff'); //clear table of all existing data

		  //test that the execute method works with and without an array as the second argument

			for(i = 1; i <= rowCount / 2; ++i) {
				 db.execute('INSERT INTO stuff (id, val) VALUES(?, ?)', i, 'our value' + i);
			}

			while(i <= rowCount) {
				 db.execute('INSERT INTO stuff (id, val) VALUES(?, ?)', [i, 'our value' + i]);
				 ++i;
			}

			resultSet = db.execute('SELECT * FROM stuff');
			
			valueOf(testRun, resultSet).shouldNotBeNull();
			valueOf(testRun, resultSet).shouldBeObject();
			valueOf(testRun, resultSet.rowCount).shouldBe(rowCount);

			counter = 1;
			while(resultSet.isValidRow()) {
				valueOf(testRun, resultSet.fieldByName('id')).shouldBe(counter);
			  valueOf(testRun, resultSet.fieldByName('val')).shouldBe('our value' + counter);
			  ++counter;

				resultSet.next();
			}
			resultSet.close()
		} catch(e) {
			Titanium.API.debug('error occurred: ' + e);
		} finally {
			db.close();
			db.remove();
	 	}

		finish(testRun);
	}

	//https://appcelerator.lighthouseapp.com/projects/32238/tickets/3393-db-get-api-extended-to-support-typed-return-value
	this.testTypedGettersAndSetters = function(testRun) {
		var db   = EncryptedDatabase.open('Test'),
		rowCount = 10,
		resultSet = null,
		i, counter, current_float, float_factor = 0.5555;

		var isAndroid = (Ti.Platform.osname === 'android');
		valueOf(testRun, db).shouldBeObject();

		try {
			counter = 1;
			i = 1;
			
			db.execute('CREATE TABLE IF NOT EXISTS stuff (id INTEGER, f REAL, val TEXT)');
			db.execute('DELETE FROM stuff;'); //clear table of all existing data
	
			var insert_float;
			while(i <= rowCount) {
			   insert_float = float_factor * i;
				 db.execute('INSERT INTO stuff (id, f, val) VALUES(?, ?, ?)', [i, insert_float, 'our value' + i]);
				 ++i;
			}
			
			resultSet = db.execute('SELECT id, f, val FROM stuff');
			
			valueOf(testRun, resultSet).shouldNotBeNull();
			valueOf(testRun, resultSet).shouldBeObject();
			valueOf(testRun, resultSet.rowCount).shouldBe(rowCount);

			while(resultSet.isValidRow()) {
				
				current_float = counter * float_factor;
				
				valueOf(testRun, resultSet.fieldByName('id', EncryptedDatabase.FIELD_TYPE_INT)).shouldBe(resultSet.field(0, EncryptedDatabase.FIELD_TYPE_INT));
				valueOf(testRun, resultSet.fieldByName('id', EncryptedDatabase.FIELD_TYPE_INT)).shouldBe(counter);
				
			  	valueOf(testRun, resultSet.fieldByName('id', EncryptedDatabase.FIELD_TYPE_INT)).shouldBe(counter);
				valueOf(testRun, resultSet.fieldByName('id', EncryptedDatabase.FIELD_TYPE_INT)).shouldBe(counter);

				valueOf(testRun, resultSet.fieldByName('f', EncryptedDatabase.FIELD_TYPE_INT)).shouldBe(resultSet.field(1, EncryptedDatabase.FIELD_TYPE_INT));
				valueOf(testRun, resultSet.fieldByName('f', EncryptedDatabase.FIELD_TYPE_INT)).shouldBe(parseInt(counter * float_factor));
				
				var f_val = resultSet.fieldByName('f', EncryptedDatabase.FIELD_TYPE_FLOAT);
 	  			valueOf(testRun, Math.floor(Math.round(f_val * 10000))/10000).shouldBe(current_float);
				valueOf(testRun, resultSet.fieldByName('f', EncryptedDatabase.FIELD_TYPE_DOUBLE)).shouldBe(current_float);
				
				valueOf(testRun, resultSet.fieldByName('val', EncryptedDatabase.FIELD_TYPE_STRING)).shouldBe('our value' + counter);
				valueOf(testRun, resultSet.fieldByName('id', EncryptedDatabase.FIELD_TYPE_STRING)).shouldBe(counter.toString());
				valueOf(testRun, resultSet.fieldByName('f', EncryptedDatabase.FIELD_TYPE_STRING)).shouldBe(current_float.toString());
				
				
				// WARNING: On iOS, the following functions throw an uncaught exception - 
				
					valueOf(testRun, function() {
						resultSet.fieldByName('val', EncryptedDatabase.FIELD_TYPE_INT);
					}).shouldThrowException();
				
					valueOf(testRun, function() {
						resultSet.fieldByName('val', EncryptedDatabase.FIELD_TYPE_DOUBLE);
					}).shouldThrowException();
				
					valueOf(testRun, function() {
						resultSet.fieldByName('val', EncryptedDatabase.FIELD_TYPE_FLOAT);
					}).shouldThrowException();
				
					valueOf(testRun, function() {
						resultSet.field(2, EncryptedDatabase.FIELD_TYPE_DOUBLE);
					}).shouldThrowException();
				
					valueOf(testRun, function() {
						resultSet.field(2, EncryptedDatabase.FIELD_TYPE_FLOAT);
					}).shouldThrowException();
				
					valueOf(testRun, function() {
						resultSet.field(2, EncryptedDatabase.FIELD_TYPE_INT);
					}).shouldThrowException();

			  ++counter;

				resultSet.next();
			}
			
		} finally {
			if(null != db) {
				db.close();
			}

			if(null != resultSet) {
				resultSet.close();
			}
		}

		finish(testRun);
	}

	this.testDatabaseExceptions = function(testRun) {
		var isAndroid = (Ti.Platform.osname === 'android');
			valueOf(testRun,  function() { EncryptedDatabase.open("fred://\\"); }).shouldThrowException();
			var db = null;
			try {
				db = EncryptedDatabase.open('Test');
			
				valueOf(testRun,  function() { 
					EncryptedDatabase.execute("select * from notATable"); 
				}).shouldThrowException();
			
				db.execute('CREATE TABLE IF NOT EXISTS stuff (id INTEGER, val TEXT)');
				db.execute('INSERT INTO stuff (id, val) values (1, "One")');
				
				valueOf(testRun,  function() {
					db.execute('SELECT * FROM idontexist');
				}).shouldThrowException();
				
				var rs = db.execute("SELECT id FROM stuff WHERE id = 1");
				
				valueOf(testRun,  function() {
					rs.field(2);
				}).shouldThrowException();
	
				valueOf(testRun,  function() {
					rs.field(2);
				}).shouldThrowException();
			
				valueOf(testRun,  function() {
					rs.fieldName(2);
				}).shouldThrowException();
			
				if (rs != null) {
					rs.close();
				}
			} finally {
				if (db != null) {
					db.close();
				db.remove();
				}
			}

		finish(testRun);
	}

	// ---------------------------------------------------------------
	// EncryptedDatabase specific test(s)
	// ---------------------------------------------------------------

	this.testReadEncryptedDatabaseWithTiDatabase = function(testRun) {		
		var db = EncryptedDatabase.open('mydb2');

		db.execute('CREATE TABLE IF NOT EXISTS people (name TEXT, phone_number TEXT, city TEXT)');
		db.execute('DELETE FROM people');

		var personArray = ['Paul', '020 7000 0000', 'London'];
		db.execute('INSERT INTO people (name, phone_number, city) VALUES (?, ?, ?)', personArray);

		var rows = db.execute('SELECT rowid,name,phone_number,city FROM people');

		while (rows.isValidRow()) {
			valueOf(testRun, rows.field(1)).shouldBeString();
			rows.next();
		}

		rows.close();
		db.close();

		// Reading an encrypted database with Ti.Database should throw an exception
		db = Ti.Database.open('mydb2');

		valueOf(testRun,  function() { 
			db.execute('SELECT rowid,name,phone_number,city FROM people');
		}).shouldThrowException();

		db.close();

		finish(testRun);
	};

	// Populate the array of tests based on the 'hammer' convention
	this.tests = require('hammer').populateTests(this);
};