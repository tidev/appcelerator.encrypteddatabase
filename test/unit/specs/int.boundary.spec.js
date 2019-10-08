let db;

describe('appcelerator.encrypteddatabase', function () {

	it('can be required', () => {
		db = require('appcelerator.encrypteddatabase');
		expect(db).toBeDefined();
		// Must use a static password, karma re-generates new project each time
		// and default is to use app guid, which will change each time.
		db.password = 'test123';
	});

	// Integer boundary tests.
	// Verify we can read/write largest/smallest 64-bit int values supported by JS number type.
	it('db read/write integer boundaries', function () {
		const MAX_SIGNED_INT32 = 2147483647;
		const MIN_SIGNED_INT32 = -2147483648;
		const MAX_SIGNED_INT16 = 32767;
		const MIN_SIGNED_INT16 = -32768;
		const rows = [
			Number.MAX_SAFE_INTEGER,
			MAX_SIGNED_INT32 + 1,
			MAX_SIGNED_INT32,
			MAX_SIGNED_INT16 + 1,
			MAX_SIGNED_INT16,
			1,
			0,
			-1,
			MIN_SIGNED_INT16,
			MIN_SIGNED_INT16 - 1,
			MIN_SIGNED_INT32,
			MIN_SIGNED_INT32 - 1,
			Number.MIN_SAFE_INTEGER
		];

		const dbConnection = db.open('int_test.db');
		dbConnection.execute('CREATE TABLE IF NOT EXISTS intTable(id INTEGER PRIMARY KEY, intValue INTEGER);');
		dbConnection.execute('DELETE FROM intTable;');
		for (let index = 0; index < rows.length; index++) {
			dbConnection.execute('INSERT INTO intTable (id, intValue) VALUES (?, ?);', index, rows[index]);
		}
		const resultSet = dbConnection.execute('SELECT id, intValue FROM intTable ORDER BY id');
		expect(resultSet.rowCount).toEqual(rows.length);
		for (let index = 0; resultSet.isValidRow(); resultSet.next(), index++) {
			expect(parseInt(resultSet.field(1))).toEqual(rows[index]);
		}
		dbConnection.close();
	});
});