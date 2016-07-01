/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
 
#import "AppceleratorEncrypteddatabaseDBProxy.h"
#import "AppceleratorEncrypteddatabaseResultSetProxy.h"
#import "TiUtils.h"
#import "TiFilesystemFileProxy.h"

@implementation AppceleratorEncrypteddatabaseDBProxy

#pragma mark Internal

@synthesize password;

BOOL isNewDatabase = NO;

-(void)dealloc
{
	[self _destroy];
	RELEASE_TO_NIL(name);
	[super dealloc];
}

-(void)shutdown:(id)sender
{
	if (database!=nil)
	{
		[self performSelector:@selector(close:) withObject:nil];
	}
}
 
-(void)_destroy
{
	WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
	[[NSNotificationCenter defaultCenter] removeObserver:self name:kTiShutdownNotification object:nil];
	[self shutdown:nil];
	[super _destroy];
}

-(void)_configure
{
	WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
	[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(shutdown:) name:kTiShutdownNotification object:nil];
	[super _configure];
}

-(NSString*)dbDir
{
    // See this apple tech note for why this changed: https://developer.apple.com/library/ios/#qa/qa1719/_index.html
    // Apparently following these guidelines is now required for app submission
    
	NSString *rootDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex:0];
	NSString *dbPath = [rootDir stringByAppendingPathComponent:@"Private Documents"];
	NSFileManager *fm = [NSFileManager defaultManager];
	
	BOOL isDirectory;
	BOOL exists = [fm fileExistsAtPath:dbPath isDirectory:&isDirectory];
	
    // Because of sandboxing, this should never happen, but we still need to handle it.
    if (exists && !isDirectory) {
        NSLog(@"[WARN] Recreating file %@... should be a directory and isn't.", dbPath);
        [fm removeItemAtPath:dbPath error:nil];
        exists = NO;
    }

	// create folder, and migrate the old one if necessary    
	if (!exists) 
	{
        isNewDatabase = YES;
        [fm createDirectoryAtPath:dbPath withIntermediateDirectories:YES attributes:nil error:nil];
    }
    
    // Migrate any old data if available
    NSString* oldRoot = [NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString* oldPath = [oldRoot stringByAppendingPathComponent:@"database"];
    BOOL oldCopyExists = [fm fileExistsAtPath:oldPath isDirectory:&isDirectory];
    if (oldCopyExists && isDirectory) {
        NSDirectoryEnumerator* contents = [fm enumeratorAtPath:oldPath];
        //This gives relative paths. So create full path before moving
        for (NSString* oldFile in contents) {
            [fm moveItemAtPath:[oldPath stringByAppendingPathComponent:oldFile] toPath:[dbPath stringByAppendingPathComponent:oldFile] error:nil];
        }
        // Remove the old copy after migrating everything
        [fm removeItemAtPath:oldPath error:nil];
        isNewDatabase = NO;
    }
	
	return dbPath;
}

-(BOOL)needCipherMigrate:(NSString*)name_
{
    NSString *rootDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *dbPath = [rootDir stringByAppendingPathComponent:@"Private Documents"];
    NSFileManager *fm = [NSFileManager defaultManager];
    NSString* versionFile = [[dbPath stringByAppendingPathComponent:name_] stringByAppendingPathExtension:@"version"];
    BOOL version131Exists = [fm fileExistsAtPath:versionFile];
    if (version131Exists) {
        //already installed using 1.3.1 and above.
        return NO;
    }
    //create version file for 1.3.1 and above.
    NSString *version = @"1.3.1";
    [fm createFileAtPath:versionFile contents:[version dataUsingEncoding:NSUTF8StringEncoding] attributes:nil];
    if (isNewDatabase) {
        return NO;
    }
    //this app is upgraded from an older module. Needs migration.
    return YES;
}

-(NSNumber*)isCipherUpgradeRequired:(id)args
{
	ENSURE_SINGLE_ARG(args, NSString)
	return NUMBOOL([self needCipherMigrate:args]);
}
-(NSString*)generateTempPath
{
    NSString *rootDir = [NSSearchPathForDirectoriesInDomains(NSLibraryDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *dbPath = [rootDir stringByAppendingPathComponent:@"Private Documents"];
    NSFileManager *fm = [NSFileManager defaultManager];
    
   return [dbPath stringByAppendingPathComponent:@"temp.db"];
}

-(BOOL)replaceOldCopy: (NSString *)oldPath withNewCopy: (NSString *)newPath
{
    if (oldPath == nil || newPath == nil) {
        NSLog(@"[ERROR] cannot copy with empty paths");
        return NO;
    }
    NSFileManager *fm = [NSFileManager defaultManager];
    NSError *error = nil;
    [fm removeItemAtPath:oldPath error:&error];
    if (error != nil) {
        [self throwException:@"Error removing old database file" subreason:[error description] location:CODELOCATION];
        return NO;
    }
    [fm moveItemAtPath:newPath toPath:oldPath error:&error];
    if (error != nil) {
        [self throwException:@"Error overwriting old database file" subreason:[error description] location:CODELOCATION];
        return NO;
    }
    return YES;
    
}

-(NSString*)dbPath:(NSString*)name_
{
	NSString *dbDir = [self dbDir];
	return [[dbDir stringByAppendingPathComponent:name_] stringByAppendingPathExtension:@"sql"];
}

-(NSDictionary*)cipherUpgrade:(NSString*)name_
{
	name = [name_ retain];
	NSString *path = [self dbPath:name];
	if (![self needCipherMigrate: name]) {
		return [[NSDictionary alloc] initWithObjectsAndKeys:
				[NSNumber numberWithBool:NO],@"success",
				[NSNumber numberWithBool:YES],@"skip",
				[NSNumber numberWithInt:0],@"code", @"",@"error", nil];
	}
	NSString *tempPath = [self generateTempPath];
	database = [[EncPLSqliteDatabase alloc] initWithPath:path andPassword:password withTempPath:tempPath];
	if (![database openAndMigrate:nil]) {
		[self throwException:@"Couldn't open database and migrate" subreason:name_ location:CODELOCATION];
		RELEASE_TO_NIL(database);
		return [[NSDictionary alloc] initWithObjectsAndKeys:
				[NSNumber numberWithBool:NO],@"success",
				[NSNumber numberWithBool:NO],@"skip",
				[NSNumber numberWithInt:-1],@"code", @"Couldn't open database and migrate",@"error", nil];
	}
	[self replaceOldCopy:path withNewCopy:tempPath];
	[database close];
	return [[NSDictionary alloc] initWithObjectsAndKeys:
			[NSNumber numberWithBool:YES],@"success",
			[NSNumber numberWithBool:NO],@"skip",
			[NSNumber numberWithInt:0],@"code", @"",@"error", nil];
}

-(void)open:(NSString*)name_
{
	name = [name_ retain];
    NSString *path = [self dbPath:name];
    if (![self needCipherMigrate: name]) {
        database = [[EncPLSqliteDatabase alloc] initWithPath:path andPassword:password];
        if (![database open]) {
            [self throwException:@"Couldn't open database" subreason:name_ location:CODELOCATION];
            RELEASE_TO_NIL(database);
        }
        return;
    }
	//we need to migrate here
    NSString *tempPath = [self generateTempPath];
    database = [[EncPLSqliteDatabase alloc] initWithPath:path andPassword:password withTempPath:tempPath];
    if (![database openAndMigrate:nil]) {
        [self throwException:@"Couldn't open database and migrate" subreason:name_ location:CODELOCATION];
        RELEASE_TO_NIL(database);
        return;
    }
	//close the old database
	[database close];
	RELEASE_TO_NIL(database);
	//copy new database over old one
    [self replaceOldCopy:path withNewCopy:tempPath];
	//now open the new database
	database = [[EncPLSqliteDatabase alloc] initWithPath:path andPassword:password];
	if (![database open]) {
		[self throwException:@"Couldn't open database" subreason:name_ location:CODELOCATION];
		RELEASE_TO_NIL(database);
	}
}

-(void)removeStatement:(EncPLSqliteResultSet*)statement
{
	[statement close];
	if (statements!=nil)
	{
		[statements removeObject:statement];
	}
}

-(id)execute:(id)args
{
	ENSURE_TYPE(args, NSArray);
    
	NSString *sql = [[args objectAtIndex:0] stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
	
	NSError *error = nil;
	EncPLSqlitePreparedStatement *statement = (EncPLSqlitePreparedStatement *) [database prepareStatement:sql error:&error];
	if (error!=nil)
	{
		[self throwException:@"invalid SQL statement" subreason:[error description] location:CODELOCATION];
	}
	
	if([args count] > 1) {
		NSArray *params = [args objectAtIndex:1];
        
		if(![params isKindOfClass:[NSArray class]]) {
            params = [args subarrayWithRange:NSMakeRange(1, [args count]-1)];
		}
        
		[statement bindParameters:params];
	}
	
	EncPLSqliteResultSet *result = (EncPLSqliteResultSet*) [statement executeQuery];
	
	if ([[result fieldNames] count]==0)
	{
		[result next]; // we need to do this to make sure lastInsertRowId and rowsAffected work
		[result close];
		return [NSNull null];
	}
	
	if (statements==nil)
	{
		statements = [[NSMutableArray alloc] initWithCapacity:5];
	}
	
	[statements addObject:result];
	
	AppceleratorEncrypteddatabaseResultSetProxy *proxy = [[[AppceleratorEncrypteddatabaseResultSetProxy alloc] initWithResults:result database:self pageContext:[self pageContext]] autorelease];
    
	return proxy;
}

-(void)install:(NSString*)path name:(NSString*)name_
{
	BOOL isDirectory;
	NSFileManager *fm = [NSFileManager defaultManager];
	NSURL *url = [TiUtils toURL:path proxy:self];
	path = [url path];
	
#if TARGET_IPHONE_SIMULATOR
	//TIMOB-6081. Resources are right now symbolic links when running in simulator) so the copy method
	//of filemanager just creates a link to the original resource.
	//Resolve the symbolic link if running in simulator
	NSError *pathError = nil;
	NSDictionary *attributes = [fm attributesOfItemAtPath:path error:&pathError];
	if (pathError != nil) 
	{
		[self throwException:@"Could not retrieve attributes" subreason:[pathError description] location:CODELOCATION];
	}
	NSString *fileType = [attributes valueForKey:NSFileType];
	if ([fileType isEqual:NSFileTypeSymbolicLink])
	{
		pathError = nil;
		path = [fm destinationOfSymbolicLinkAtPath:path error:&pathError];
		
		if (pathError != nil) 
		{
			[self throwException:@"Could not resolve symbolic link" subreason:[pathError description] location:CODELOCATION];
		}		
	}
	
#endif
	
	BOOL exists = [fm fileExistsAtPath:path isDirectory:&isDirectory];
	if (!exists)
	{
		[self throwException:@"invalid database install path" subreason:path location:CODELOCATION];
	}
	
	// get the install path
	NSString *installPath = [self dbPath:name_];
	
	// see if we have already installed the DB
	exists = [fm fileExistsAtPath:installPath isDirectory:&isDirectory];
	if (!exists)
	{
		NSError *error = nil;
		// install it by copying it
		[fm copyItemAtPath:path toPath:installPath error:&error];
		if (error!=nil)
		{
			[self throwException:@"couldn't install database" subreason:[error description] location:CODELOCATION];
		}
	}
	
	[self open:name_];
}

-(void)close:(id)args
{
	if (statements!=nil)
	{
		for (EncPLSqliteResultSet *result in statements)
		{
			[result close];
		}
		RELEASE_TO_NIL(statements);
	}
	if (database!=nil)
	{
		if ([database goodConnection])
		{
			@try 
			{
				[database close];
			}
			@catch (NSException * e) 
			{
				NSLog(@"[WARN] attempting to close database, returned error: %@",e);
			}
		}
		RELEASE_TO_NIL(database);
	}
}

-(void)remove:(id)args
{
	NSString *dbPath = [self dbPath:name];
	[[NSFileManager defaultManager] removeItemAtPath:dbPath error:nil];
}

-(NSNumber*)lastInsertRowId
{
	if (database!=nil)
	{
		return NUMINT([database lastInsertRowId]);
	}
	return NUMINT(0);
}

-(NSNumber*)rowsAffected
{
	if (database!=nil)
	{
		return NUMINT(sqlite3_changes([database sqliteDB]));
	}
	return NUMINT(0);
}

-(NSString*)name
{
	return name;
}
-(TiFilesystemFileProxy*)file
{
	return [[TiFilesystemFileProxy alloc] initWithFile:[self dbPath:name]];
}

#pragma mark Internal
-(EncPLSqliteDatabase*)database
{
	return database;
}

@end