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
	};

	this.name = 'encrypteddatabase';

	// Test that module is loaded
	this.testModule = function(testRun) {
		// Verify that the module is defined
		valueOf(testRun, EncryptedDatabase).shouldBeObject();

		EncryptedDatabase.password = 'this is my awesome password';

		var db = EncryptedDatabase.open('mydb2');
		valueOf(testRun, db.file.nativePath).shouldBeString();

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

		finish(testRun);
	};

	// Populate the array of tests based on the 'hammer' convention
	this.tests = require('hammer').populateTests(this);
};