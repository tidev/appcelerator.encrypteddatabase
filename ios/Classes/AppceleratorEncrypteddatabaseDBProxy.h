/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */

#import "EncPlausibleDatabase.h"
#import "TiProxy.h"
#import <sqlite3.h>

@interface AppceleratorEncrypteddatabaseDBProxy : TiProxy {
  @protected
  NSString *name;
  EncPLSqliteDatabase *database;
  NSMutableArray *statements;
}

@property (nonatomic, readonly) NSString *name;
@property (nonatomic, readonly) NSNumber *rowsAffected;
@property (nonatomic, readonly) NSNumber *lastInsertRowId;
@property (nonatomic, readwrite, retain) NSString *password;

- (NSDictionary *)cipherUpgrade:(NSString *)name_;
- (NSNumber *)isCipherUpgradeRequired:(id)args;
- (void)open:(NSString *)name;
- (void)install:(NSString *)path name:(NSString *)name;
- (id)execute:(id)args;
- (void)close:(id)args;
- (void)remove:(id)args;

#pragma mark Internal

- (void)removeStatement:(EncPLSqliteResultSet *)statement;
- (EncPLSqliteDatabase *)database;

@end