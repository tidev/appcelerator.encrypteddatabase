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

#import "EncPlausibleDatabase.h"
#import "TiBlob.h"

#pragma mark Parameter Strategy

/**
 * @internal
 * Parameter fetching strategy.
 */
@protocol EncPLSqliteParameterStrategy

/**
 * Return the number of available parameters
 */
- (int) count;

/**
 * Return the value for the given parameter. May
 * return nil if the parameter is unavailable,
 * and NSNull if the parameter's value is null.
 */
- (id) valueForParameter: (int) parameterIndex;

@end

/**
 * @internal
 * NSArray parameter strategy.
 */
@interface EncPLSqliteArrayParameterStrategy : NSObject <EncPLSqliteParameterStrategy> {
@private
    NSArray *_values;
}
@end

@implementation EncPLSqliteArrayParameterStrategy

- (id) initWithValues: (NSArray *) values {
    if ((self = [super init]) == nil)
        return nil;

    _values = [values retain];

    return self;
}

- (void) dealloc {
    [_values release];
    [super dealloc];
}

- (int) count {
    return [_values count];
}

- (id) valueForParameter: (int) parameterIndex {
    /* Arrays are zero-index, sqlite is 1-indexed, so adjust the index
     * for the array */
    return [_values objectAtIndex: parameterIndex - 1];
}

@end


/**
 * @internal
 * NSDictionary parameter strategy.
 */
@interface EncPLSqliteDictionaryParameterStrategy : NSObject <EncPLSqliteParameterStrategy> {
@private
    sqlite3_stmt *_sqlite_stmt;
    NSDictionary *_values;
}
@end

@implementation EncPLSqliteDictionaryParameterStrategy

/* Memory warning -- sqlite_stmt is a borrowed weak reference */
- (id) initWithStatement: (sqlite3_stmt *) sqlite_stmt values: (NSDictionary *) values {
    if ((self = [super init]) == nil)
        return nil;

    _sqlite_stmt = sqlite_stmt;
    _values = [values retain];

    return self;
}

- (void) dealloc {
    [_values release];
    [super dealloc];
}

- (int) count {
    return [_values count];
}

- (id) valueForParameter: (int) parameterIndex {
    const char *sqlite_name;

    /* Fetch the parameter name. */
    sqlite_name = sqlite3_bind_parameter_name(_sqlite_stmt, parameterIndex);
    
    /* If there is no name, or if it's blank, we can't retrieve the value. */
    if (sqlite_name == NULL || *sqlite_name == '\0')
        return NULL;

    /* Fetch the value, stripping the initial ':' characeter. */
    assert(*sqlite_name != '\0'); // checked above.
    return [_values objectForKey: [NSString stringWithUTF8String: sqlite_name + 1]];
}

@end


#pragma mark Private Declarations

@interface EncPLSqlitePreparedStatement (EncPLSqlitePreparedStatementPrivate)

- (int) bindValueForParameter: (int) parameterIndex withValue: (id) value;

- (void) assertNotClosed;
- (void) assertNotInUse;

- (EncPLSqliteResultSet *) checkoutResultSet;

@end

#pragma mark Public Implementation

/**
 * @internal
 * SQLite prepared query implementation.
 *
 * @par Thread Safety
 * EncPLSqlitePreparedStatement instances implement no locking and must not be shared between threads
 * without external synchronization.
 */
@implementation EncPLSqlitePreparedStatement

- (NSString *) queryString;
{
	return _queryString;
}
/**
 * @internal
 *
 * Initialize the prepared statement with an open database and an sqlite3 prepared statement.
 *
 * @param db A reference to the managing EncPLSqliteDatabase instance.
 * @param sqliteStmt The prepared sqlite statement. This class will assume ownership of the reference.
 * @param queryString The original SQL query string, used for error reporting.
 * @param closeAtCheckin A flag specifying whether the statement should be closed at first checkin. Used to support returning
 * only the result set to a caller. When the result set is closed, the prepared statement is closed.
 *
 * MEMORY OWNERSHIP WARNING:
 * We are passed an sqlite3_stmt reference which now we now assume authority for releasing
 * that statement using sqlite3_finalize().
 *
 * @par Designated Initializer
 * This method is the designated initializer for the EncPLSqlitePreparedStatement class.
 */
- (id) initWithDatabase: (EncPLSqliteDatabase *) db 
             sqliteStmt: (sqlite3_stmt *) sqlite_stmt
            queryString: (NSString *) queryString 
         closeAtCheckin: (BOOL) closeAtCheckin 
{
    if ((self = [super init]) == nil)
        return nil;

    /* Mark whether we should close when the first result set is checked in */
    _closeAtCheckin = closeAtCheckin;

    /* Save our database and statement reference. */
    _database = [db retain];
    _sqlite_stmt = sqlite_stmt;
    _queryString = [queryString retain];
    _inUse = NO;

    /* Cache parameter count */
    _parameterCount = sqlite3_bind_parameter_count(_sqlite_stmt);
    assert(_parameterCount >= 0); // sanity check

    return self;
}


/* GC */
- (void) finalize {
    // XXX: May cause a memory leak when garbage collecting due
    // to Apple's finalization rules. No ordering is maintained,
    // and such, there's no way to ensure that the sqlite3_stmt
    // is released before sqlite3_close() is called.
    [self close];
    [super finalize];
}

/* Manual */
- (void) dealloc {
    /* The statement must be released before the database is released, as the statement has a reference
     * to the database which would cause a SQLITE_BUSY error when the database is released. */
    [self close];
    
    /* Now release the database. */
    [_database release];
    
    /* Release the query statement */
    [_queryString release];
    
    [super dealloc];
}


/* from EncPLPreparedStatement */
- (void) close {
    if (_sqlite_stmt == nil)
        return;
    
    /* The finalization may return the last error returned by sqlite3_next(), but this has already
     * been handled by the -[EncPLSqliteResultSet next] implementation. Any remaining memory and
     * resources are released regardless of the error code, so we do not check it here. */
    sqlite3_finalize(_sqlite_stmt);
    _sqlite_stmt = nil;
}


/* from EncPLPreparedStatement */
- (int) parameterCount {
    [self assertNotClosed];

    return _parameterCount;
}


/**
 * @internal
 * Bind all parameters, fetching their value using the provided selector.
 */
- (void) bindParametersWithStrategy: (NSObject<EncPLSqliteParameterStrategy> *) strategy {
    [self assertNotInUse];
    
    /* Verify that a complete parameter list was provided */
    if ([strategy count] != _parameterCount)
        [NSException raise: ENC_EncPLSqliteException 
                    format: @"%@ prepared statement provided invalid parameter count (expected %d, but %d were provided)", [self class], _parameterCount, [strategy count]];
    
    /* Clear any existing bindings */
    sqlite3_clear_bindings(_sqlite_stmt);
    
    /* Sqlite counts parameters starting at 1. */
    for (int valueIndex = 1; valueIndex <= _parameterCount; valueIndex++) {
        /* (Note that NSArray indexes from 0, so we subtract one to get the current value) */
        id value = [strategy valueForParameter: valueIndex];
        if (value == nil) {
            [NSException raise: ENC_EncPLSqliteException
                        format: @"Missing parameter %d binding for query %@", valueIndex, _queryString];
        }

        /* Bind the parameter */
        int ret = [self bindValueForParameter: valueIndex
                                    withValue: value];
        
        /* If the bind fails, throw an exception (programmer error). */
        if (ret != SQLITE_OK) {
            [NSException raise: ENC_EncPLSqliteException
                        format: @"SQlite error binding parameter %d for query %@: %@", valueIndex - 1, _queryString, [_database lastErrorMessage]];
        }
    }
    
    /* If you got this far, all is well */
}

/* from EncPLPreparedStatement */
- (void) bindParameters: (NSArray *) parameters {
    EncPLSqliteArrayParameterStrategy *strategy;
    
    strategy = [[[EncPLSqliteArrayParameterStrategy alloc] initWithValues: parameters] autorelease];
    [self bindParametersWithStrategy: strategy];
}

- (void) bindParameterDictionary: (NSDictionary *) parameters {
    EncPLSqliteDictionaryParameterStrategy *strategy;
    
    strategy = [[[EncPLSqliteDictionaryParameterStrategy alloc] initWithStatement: _sqlite_stmt values: parameters] autorelease];
    [self bindParametersWithStrategy: strategy];
}


/* from EncPLPreparedStatement */
- (BOOL) executeUpdate {
    return [self executeUpdateAndReturnError: nil];
}


/* from EncPLPreparedStatement */
- (BOOL) executeUpdateAndReturnError: (NSError **) outError {
    [self assertNotInUse];

    int ret;
    
    /* Call sqlite3_step() to run the virtual machine */
    ret = sqlite3_step(_sqlite_stmt);

    /* Reset the statement */
    sqlite3_reset(_sqlite_stmt);
    
    /* On success, return (even if data was provided) */
    if (ret == SQLITE_DONE || ret == SQLITE_ROW)
        return YES;
    
    /* Query failed */
    [_database populateError: outError
               withErrorCode: EncPLDatabaseErrorQueryFailed
                 description: NSLocalizedString(@"An error occurred executing an SQL update.", @"")
                 queryString: _queryString];
    return NO;
}


/* from EncPLPreparedStatement */
- (NSObject<ENC_EncPLResultSet> *) executeQuery {
    return [self executeQueryAndReturnError: nil];
}

/* from EncPLPreparedStatement */
- (NSObject<ENC_EncPLResultSet> *) executeQueryAndReturnError: (NSError **) outError {
    /*
     * Check out a new EncPLSqliteResultSet statement.
     * At this point, is there any way for the query to actually fail? It has already been compiled and verified.
     */
    return [self checkoutResultSet];
}

/**
 * @internal
 *
 * Check a result set back in, releasing any associated data
 * and releasing any exclusive ownership on the prepared statement.
 */
- (void) checkinResultSet: (EncPLSqliteResultSet *) resultSet {
    assert(_inUse = YES); // That would be strange.

    _inUse = NO;
    sqlite3_reset(_sqlite_stmt);

    /* If the statement is to be closed on the first checkin, do so, and
     * release our database resources */
    if (_closeAtCheckin)
        [self close];
}

@end

#pragma mark Private Implementation

/**
 * @internal
 *
 * Private EncPLSqliteDatabase methods.
 */
@implementation EncPLSqlitePreparedStatement (EncPLSqlitePreparedStatementPrivate)

/**
 * @internal
 * Assert that the result set has not been closed
 */
- (void) assertNotClosed {
    if (_sqlite_stmt == nil)
        [NSException raise: ENC_EncPLSqliteException format: @"Attempt to access already-closed prepared statement."];
}

/**
 * @internal
 *
 * Assert that this instance is not in use by a EncPLSqliteResult.
 */
- (void) assertNotInUse {
    [self assertNotClosed];

    if (_inUse)
        [NSException raise: ENC_EncPLSqliteException format: @"A EncPLSqliteResultSet is already active and has not been properly closed for prepared statement '%@'", _queryString];
}

/**
 * @internal
 *
 * Check out a new EncPLSqliteResultSet, acquiring exclusive ownership
 * of the prepared statement. If another result set is currently checked
 * out, will throw an exception;
 */
- (EncPLSqliteResultSet *) checkoutResultSet {
    /* State validation. Only one result set may be checked out at a time */
    [self assertNotInUse];
    _inUse = YES;

   /*
    * MEMORY OWNERSHIP WARNING:
    * We pass our sqlite3_stmt reference to the EncPLSqliteResultSet, and gaurantee (by contract)
    * that the statement reference will remain valid until checkinResultSet is called for
    * the new EncPLSqliteResultSet instance.
    */
    return [[[EncPLSqliteResultSet alloc] initWithPreparedStatement: self sqliteStatemet: _sqlite_stmt] autorelease];
}

/**
 * @internal
 * Bind a value to a statement parameter, returning the SQLite bind result value.
 *
 * @param parameterIndex Index of parameter to be bound.
 * @param value Objective-C object to use as the value.
 */
- (int) bindValueForParameter: (int) parameterIndex withValue: (id) value {
    /* NULL */
    if (value == nil || value == [NSNull null]) {
        return sqlite3_bind_null(_sqlite_stmt, parameterIndex);
    }
    
	/* Blob handling */
	if ([value isKindOfClass:[TiBlob class]]) {
		value = [value data];
	}
	
	/* Image handling */
	if ([value isKindOfClass:[UIImage class]]) {
		value = UIImageJPEGRepresentation(value, 1.0);
	}
	
    /* Data */
	if ([value isKindOfClass: [NSData class]]) {
        return sqlite3_bind_blob(_sqlite_stmt, parameterIndex, [value bytes], [value length], SQLITE_TRANSIENT);
    }
    
    /* Date */
    else if ([value isKindOfClass: [NSDate class]]) {
        return sqlite3_bind_double(_sqlite_stmt, parameterIndex, [value timeIntervalSince1970]);
    }
    
    /* String */
    else if ([value isKindOfClass: [NSString class]]) {
        return sqlite3_bind_text(_sqlite_stmt, parameterIndex, [value UTF8String], -1, SQLITE_TRANSIENT);
    }
    
    /* Number */
    else if ([value isKindOfClass: [NSNumber class]]) {
        const char *objcType = [value objCType];
        int64_t number = [value longLongValue];
        
        /* Handle floats and doubles */
        if (strcmp(objcType, @encode(float)) == 0 || strcmp(objcType, @encode(double)) == 0) {
            return sqlite3_bind_double(_sqlite_stmt, parameterIndex, [value doubleValue]);
        }
        
        /* If the value can fit into a 32-bit value, use that bind type. */
        else if (number <= INT32_MAX) {
            return sqlite3_bind_int(_sqlite_stmt, parameterIndex, number);
            
            /* Otherwise use the 64-bit bind. */
        } else {
            return sqlite3_bind_int64(_sqlite_stmt, parameterIndex, number);
        }
    }
    
    /* Not a known type */
    [NSException raise: ENC_EncPLSqliteException format: @"SQLite error binding unknown parameter type '%@'. Value: '%@'", [value class], value];
    
    /* Unreachable */
    abort();
}

@end