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
- (id)moduleGUID
{
  return @"d1b3740c-ec53-45c6-8454-8748f91da6ad";
}

// this is generated for your module, please do not change it
- (NSString *)moduleId
{
  return @"appcelerator.encrypteddatabase";
}

- (void)startup
{
  // enable multi-threading
  sqlite3_enable_shared_cache(TRUE);
  kdfIterations = @256000;
  hmacAlgorithm = @3;
}

@synthesize password;

- (id)open:(id)path
{
  ENSURE_SINGLE_ARG(path, NSString);
  AppceleratorEncrypteddatabaseDBProxy *db = [[[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil] autorelease];
  db.password = password;
  [db setKdfIterations:kdfIterations andHmacAlgorithm:hmacAlgorithm];

  [db open:path];
  return db;
}

- (id)cipherUpgrade:(id)path
{
  ENSURE_SINGLE_ARG(path, NSString);
  AppceleratorEncrypteddatabaseDBProxy *db = [[[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil] autorelease];
  db.password = password;
  [db setKdfIterations:kdfIterations andHmacAlgorithm:hmacAlgorithm];

  return [db cipherUpgrade:path];
}

- (NSNumber *)isCipherUpgradeRequired:(id)args
{
  ENSURE_SINGLE_ARG(args, NSString)
  AppceleratorEncrypteddatabaseDBProxy *db = [[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil];
  NSNumber *result = [db isCipherUpgradeRequired:args];
  RELEASE_TO_NIL(db)
  return result;
}
- (id)install:(id)args
{
  ENSURE_ARG_COUNT(args, 2);
  AppceleratorEncrypteddatabaseDBProxy *db = [[[AppceleratorEncrypteddatabaseDBProxy alloc] _initWithPageContext:[self executionContext] args:nil] autorelease];
  db.password = password;
  [db setKdfIterations:kdfIterations andHmacAlgorithm:hmacAlgorithm];
  [db install:[args objectAtIndex:0] name:[args objectAtIndex:1]];
  return db;
}

- (void)shutdown:(id)sender
{
  password = nil;
}

- (void)setHmacKdfIterations:(NSNumber *)iterations
{
  ENSURE_TYPE(iterations, NSNumber);
  if ([iterations integerValue] > 256000) {
    iterations = @256000;
  } else if ([iterations integerValue] < 4000) {
    iterations = @4000;
  }
  kdfIterations = iterations;
}

- (NSNumber *)hmacKdfIterations
{
  return kdfIterations;
}

- (void)setHmacAlgorithm:(NSNumber *)algorithm
{
  ENSURE_TYPE(algorithm, NSNumber);
  hmacAlgorithm = algorithm;
}

- (NSNumber *)hmacAlgorithm
{
  return hmacAlgorithm;
}

MAKE_SYSTEM_PROP(FIELD_TYPE_UNKNOWN, TiFieldTypeUnknown);
MAKE_SYSTEM_PROP(FIELD_TYPE_STRING, TiFieldTypeString);
MAKE_SYSTEM_PROP(FIELD_TYPE_INT, TiFieldTypeInt);
MAKE_SYSTEM_PROP(FIELD_TYPE_FLOAT, TiFieldTypeFloat);
MAKE_SYSTEM_PROP(FIELD_TYPE_DOUBLE, TiFieldTypeDouble);

MAKE_SYSTEM_PROP(HMAC_SHA1, 1);
MAKE_SYSTEM_PROP(HMAC_SHA256, 2);
MAKE_SYSTEM_PROP(HMAC_SHA512, 3);

@end
