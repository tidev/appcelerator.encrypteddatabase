/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
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