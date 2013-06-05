var EncryptedDatabase = require('appcelerator.encrypteddatabase');
EncryptedDatabase.password = 'this is my awesome password';

var db = EncryptedDatabase.open('mydb2');
Ti.API.info(db.file.nativePath);

db.execute('CREATE TABLE IF NOT EXISTS people (name TEXT, phone_number TEXT, city TEXT)');
db.execute('DELETE FROM people');


var personArray = ['Paul', '020 7000 0000', 'London'];
db.execute('INSERT INTO people (name, phone_number, city) VALUES (?, ?, ?)', personArray);

var rows = db.execute('SELECT rowid,name,phone_number,city FROM people');

while (rows.isValidRow()) {
	alert('Person ---> ROWID: ' + rows.fieldByName('rowid')
		+ ', name:' + rows.field(1)
		+ ', phone_number: ' + rows.fieldByName('phone_number')
		+ ', city: ' + rows.field(3));
	rows.next();
}


rows.close();
db.close();