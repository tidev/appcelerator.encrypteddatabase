/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
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
-(id)cipherUpgrade:(id)path;
-(NSNumber*)isCipherUpgradeRequired:(id)args;
@end
