/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiModule.h"
#import "AppceleratorEncrypteddatabaseDBProxy.h"

typedef enum {
	TiFieldTypeUnknown = -1,
	TiFieldTypeString,
	TiFieldTypeInt,
	TiFieldTypeFloat,
	TiFieldTypeDouble
} AppceleratorDatabaseFieldType;

@interface AppceleratorEncrypteddatabaseModule : TiModule 
{
}

@property(nonatomic,readwrite,retain) NSString* password;

@end
