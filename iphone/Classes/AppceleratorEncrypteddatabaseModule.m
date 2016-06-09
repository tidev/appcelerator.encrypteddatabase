/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
 
#import "AppceleratorEncrypteddatabaseModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"

@implementation AppceleratorEncrypteddatabaseModule

#pragma mark Internal

// this is generated for your module, please do not change it
-(id)moduleGUID
{
	return @"d1b3740c-ec53-45c6-8454-8748f91da6ad";
}

// this is generated for your module, please do not change it
-(NSString*)moduleId
{
	return @"appcelerator.encrypteddatabase";
}

-(void)startup
{
	// enable multi-threading
	sqlite3_enable_shared_cache(TRUE);
}

@synthesize password;

-(id)open:(id)path
{
	ENSURE_SINGLE_ARG(path,NSString);
	AppceleratorEncrypteddatabaseDBProxy *db = [[[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil] autorelease];
    db.password = password;
	[db open:path];
	return db;
}

-(id)cipherUpgrade:(id)path
{
	ENSURE_SINGLE_ARG(path,NSString);
	AppceleratorEncrypteddatabaseDBProxy *db = [[[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil] autorelease];
	db.password = password;
	return [db cipherUpgrade:path];
}

-(id)install:(id)args
{
	ENSURE_ARG_COUNT(args,2);
	AppceleratorEncrypteddatabaseDBProxy *db = [[[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil] autorelease];
    db.password = password;
	[db install:[args objectAtIndex:0] name:[args objectAtIndex:1]];
	return db;
}

-(void)shutdown:(id)sender
{
    password = nil;
}

#define DB_CONSTANT(name, num) \
-(id)name {\
return NUMINT(num);\
}

DB_CONSTANT(FIELD_TYPE_UNKNOWN, TiFieldTypeUnknown)
DB_CONSTANT(FIELD_TYPE_STRING, TiFieldTypeString)
DB_CONSTANT(FIELD_TYPE_INT, TiFieldTypeInt)
DB_CONSTANT(FIELD_TYPE_FLOAT, TiFieldTypeFloat)
DB_CONSTANT(FIELD_TYPE_DOUBLE, TiFieldTypeDouble);

@end
