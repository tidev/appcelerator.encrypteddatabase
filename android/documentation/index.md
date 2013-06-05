# Appcelerator Encrypted Database Module

## Description
Encrypt your SQLite client side databases. Uses the same API as Ti.Database, so it can be swapped in with only trivial code changes.

## Accessing the Module
To access this module from JavaScript, you would do the following:

	var EncryptedDatabase = require("appcelerator.encrypteddatabase");

The EncryptedDatabase variable is a reference to the Module object.	

## Reference

### EncryptedDatabase.password

The password to use when opening or installing databases. After setting this, any subsequent calls to "open" or "install" on this module
will be encrypted. Defaults to the current application's GUID, as defined in the tiapp.xml.

### EncryptedDatabase.setPassword(string val)

### string EncryptedDatabase.getPassword()

## Usage
See example.

## Author
Dawson Toth

## Module History
View the [change log](changelog.html) for this module.

## Feedback and Support
Please direct all questions, feedback, and concerns to [info@appcelerator.com](mailto:info@appcelerator.com?subject=Encrypted%20Database%20Module).

## License
Copyright(c) 2010-2013 by Appcelerator, Inc. All Rights Reserved. Please see the LICENSE file included in the distribution for further details.