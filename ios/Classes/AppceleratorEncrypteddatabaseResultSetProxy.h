/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
 
#import "TiProxy.h"

#import "EncPlausibleDatabase.h"

@class AppceleratorEncrypteddatabaseDBProxy;

@interface AppceleratorEncrypteddatabaseResultSetProxy : TiProxy {
@private
	AppceleratorEncrypteddatabaseDBProxy *database;
	EncPLSqliteResultSet *results;
	BOOL validRow;
	int rowCount;
}

-(id)initWithResults:(EncPLSqliteResultSet*)results database:(AppceleratorEncrypteddatabaseDBProxy*)database pageContext:(id<TiEvaluator>)context;

@property(nonatomic,readonly) NSNumber *rowCount;
@property(nonatomic,readonly) NSNumber *validRow;

@end