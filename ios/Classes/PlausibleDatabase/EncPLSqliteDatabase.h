/*
 * Copyright (c) 2008 Plausible Labs Cooperative, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of any contributors
 *    may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#import <sqlite3.h>
#import "AppModule.h"
#include "ThirdpartyNS.h"

extern NSString *EncPLSqliteException;

@interface EncPLSqliteDatabase : NSObject <EncPLDatabase> {
@private
    /** Path to the database file. */
    NSString *_path;
    NSString *_tempPath;
    NSString *_password;
    BOOL _encrypted;
    
    /** Underlying sqlite database reference. */
    sqlite3 *_sqlite;
}

@property (nonatomic, retain) NSNumber *cipherVersion;
@property (nonatomic, retain) NSNumber *oldCipherVersion;

+ (id) databaseWithPath: (NSString *) dbPath;

- (id) initWithPath: (NSString*) dbPath andPassword: (NSString*) password;

- (id) initWithPath: (NSString*) dbPath andPassword: (NSString*) password withTempPath: (NSString*) tempPath;

- (BOOL) open;
- (BOOL) openAndReturnError: (NSError **) error;
- (BOOL) openAndMigrate: (NSError **) error;

- (int64_t) lastInsertRowId;

- (NSString *) path;
- (sqlite3 *) sqliteDB;

@end

#ifdef EncPL_DB_PRIVATE

@interface EncPLSqliteDatabase (EncPLSqliteDatabaseLibraryPrivate)

- (int) lastErrorCode;
- (NSString *) lastErrorMessage;

- (BOOL) populateError: (NSError **) result withErrorCode: (EncPLDatabaseError) errorCode
           description: (NSString *) localizedDescription queryString: (NSString *) queryString;

@end

#endif
