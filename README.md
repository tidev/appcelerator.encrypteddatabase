# Appcelerator Encrypted Database

This is the Appcelerator Encrypted Database Module for Titanium.

Interested in contributing? Read the [contributors/committer's](https://wiki.appcelerator.org/display/community/Home) guide.

## How to build

### iOS

For iOS you have to build SQLCipher by hand first:
```
$ cd ~/Documents/code
$ git clone https://github.com/sqlcipher/sqlcipher.git
$ cd sqlcipher
$ ./configure --with-crypto-lib=none
$ make sqlite3.c
```
(source https://www.zetetic.net/sqlcipher/ios-tutorial/#option-1-source-integration)

and then put the sqlite3.c/sqlite3.h file into the ios folder. After that you can compile the module with `ti build -p ios -b`.

### Android

Update the version in `build.gradle` and run `ti build -p android -b`

## Legal

This module is Copyright (c) 2010-present by Appcelerator, Inc. All Rights Reserved. Usage of this module is subject to
the Terms of Service agreement with Appcelerator, Inc.  
