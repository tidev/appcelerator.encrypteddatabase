const dbobj = require('appcelerator.encrypteddatabase');

var win = Ti.UI.createWindow({ backgroundColor: 'white' });
var btn = Ti.UI.createButton({ title: 'Trigger' });

btn.addEventListener('click', accessDatabase);

win.add(btn);
win.open();

function accessDatabase() {
	dbobj.password = 'secret';

	Ti.API.info('Opening DB ...');
	const instance = dbobj.open('test.db');

	instance.execute('CREATE TABLE IF NOT EXISTS user(name string, phone string);');
	instance.execute('insert into user (name, phone) values("oz", json(\'{"cell":"+491765", "home":"+498973"}\'));');

	const dataToInsertHandle = instance.execute('select user.phone from user where user.name==\'oz\'');
	const result = dataToInsertHandle.isValidRow() ? dataToInsertHandle.fieldByName('phone') : null;

	alert('Fetched data: ' + result);
	Ti.API.info('Closing DB ...');
	instance.close();
}
