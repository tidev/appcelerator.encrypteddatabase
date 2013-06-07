# Appcelerator Encrypted Database Module

## Description
Provides transparent, secure 256-bit AES encryption of SQLite database files.

## Accessing the Module
To access this module from JavaScript, you would do the following:

	var EncryptedDatabase = require("appcelerator.encrypteddatabase");

The EncryptedDatabase variable is a reference to the Module object.	

## Reference

This module inherits from _[Titanium.Database][]_, and has the same methods and properties. It also has the following property and methods:

### string EncryptedDatabase.password
The password to use when opening or installing databases. After setting this, any subsequent calls to "open" or "install" on this module
will be encrypted. Defaults to the current application's GUID, as defined in the tiapp.xml.

### void EncryptedDatabase.setPassword(string val)
Synonymous with setting the "password" property to a value.

### string EncryptedDatabase.getPassword()
Synonymous with getting the "password" property.

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

[Titanium.Database]: http://docs.appcelerator.com/titanium/latest/#!/api/Titanium.Database