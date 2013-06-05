/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"
#import <sqlite3.h>
#import "EncPlausibleDatabase.h"

@interface AppceleratorEncrypteddatabaseDBProxy : TiProxy {
@protected
	NSString *name;
	EncPLSqliteDatabase *database;
	NSMutableArray *statements;
}

@property(nonatomic,readonly) NSString *name;
@property(nonatomic,readonly) NSNumber *rowsAffected;
@property(nonatomic,readonly) NSNumber *lastInsertRowId;
@property(nonatomic,readwrite,retain) NSString* password;

-(void)open:(NSString*)name;
-(void)install:(NSString*)path name:(NSString*)name;
-(id)execute:(id)args;
-(void)close:(id)args;
-(void)remove:(id)args;

#pragma mark Internal

-(void)removeStatement:(EncPLSqliteResultSet*)statement;
-(EncPLSqliteDatabase*)database;

@end