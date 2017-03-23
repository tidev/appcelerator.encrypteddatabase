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

/* Dependencies */
#import <Foundation/Foundation.h>
#include "ThirdpartyNS.h"

/**
 * @defgroup functions Plausible Database Functions Reference
 */

/**
 * @defgroup constants Plausible Database Constants Reference
 */

/**
 * @defgroup enums Enumerations
 * @ingroup constants
 */

/**
 * @defgroup globals Global Variables
 * @ingroup constants
 */

/**
 * @defgroup exceptions Exceptions
 * @ingroup constants
 */

/* Exceptions */
extern NSString *EncPLDatabaseException;

/* Error Domain and Codes */
extern NSString *EncPLDatabaseErrorDomain;
extern NSString *EncPLDatabaseErrorQueryStringKey;
extern NSString *EncPLDatabaseErrorVendorErrorKey;
extern NSString *EncPLDatabaseErrorVendorStringKey;

/**
 * NSError codes in the Plausible Database error domain.
 * @ingroup enums
 */
typedef enum {
    /** An unknown error has occured. If this
     * code is received, it is a bug, and should be reported. */
    EncPLDatabaseErrorUnknown = 0,
    
    /** File not found. */
    EncPLDatabaseErrorFileNotFound = 1,
    
    /** An SQL query failed. */
    EncPLDatabaseErrorQueryFailed = 2,
    
    /** The provided SQL statement was invalid. */
    EncPLDatabaseErrorInvalidStatement = 3,
    
    /** cipher migrate failed */
    EncPLDatabaseErrorCipherMigrateFailed = 4
} EncPLDatabaseError;


/* Library Includes */
#import "EncPLResultSet.h"
#import "EncPLPreparedStatement.h"
#import "EncPLDatabase.h"

#import "EncPLSqliteDatabase.h"
#import "EncPLSqlitePreparedStatement.h"
#import "EncPLSqliteResultSet.h"

#ifdef EncPL_DB_PRIVATE

@interface EncPlausibleDatabase : NSObject {
}

+ (NSError *) errorWithCode: (EncPLDatabaseError) errorCode localizedDescription: (NSString *) localizedDescription 
                queryString: (NSString *) queryString
                 vendorError: (NSNumber *) vendorError vendorErrorString: (NSString *) vendorErrorString;

@end

#endif /* EncPL_DB_PRIVATE */

/**
 * @mainpage Plausible Database
 *
 * @section intro_sec Introduction
 *
 * Plausible Database provides a generic Objective-C interface for interacting with
 * SQL databases. SQLite is the initial and primary target, but the API has been
 * designed to support more traditional databases.
 *
 * While the code is stable and unit tested, the API has not yet been finalized,
 * and may see incompatible changes prior to the 1.0 release.
 *
 * Plausible Database provides an Objective-C veneer over the underlying SQL database. Objects
 * are automatically bound to statement parameters, and converted to and from the underlying SQL datatypes.
 *
 * Library classes supporting subclassing are explicitly documented. Due to Objective-C's fragile base classes,
 * binary compatibility with subclasses is NOT guaranteed. You should avoid subclassing library
 * classes -- use class composition instead.
 *
 *
 * @section doc_sections Documentation Sections
 * - @subpage exec_sql
 * - @subpage error_handling
 *
 *
 * @section services Integration & Development Services
 * Plausible Database is provided free of charge under the BSD license, and may be freely integrated with any application.
 * We can provide assistance with integrating our code in your own iPhone or Mac application, as well as development of additional features --
 * including support for additional databases --  under a license of your choosing (higher rates apply for non BSD-licensed work).
 * Contact Plausible Labs for more information: http://www.plausiblelabs.com
 */

/**
 * @page exec_sql Basic SQL Programming Guide
 *
 * @section create_conn Creating a Connection
 *
 * Open a connection to a database file:
 *
 * <pre>
 * EncPLSqliteDatabase *db = [[EncPLSqliteDatabase alloc] initWithPath:  @"/path/to/database"];
 * if (![db open])
 *     NSLog(@"Could not open database");
 * </pre>
 *
 * @section exec_update Update Statements
 *
 * Update statements can be executed using -[EncPLDatabase executeUpdate:]
 *
 * <pre>
 * if (![db executeUpdate: @"CREATE TABLE example (id INTEGER)"])
 *     NSLog(@"Table creation failed");
 *
 * if (![db executeUpdate: @"INSERT INTO example (id) VALUES (?)", [NSNumber numberWithInteger: 42]])
 *     NSLog(@"Data insert failed");
 * </pre>
 * @section exec_query Query Statements
 *
 * Queries can be executed using -[EncPLDatabase executeQuery:]. To iterate over the returned results, an NSObject instance
 * conforming to EncPLResultSet will be returned.
 *
 * <pre>
 * NSObject<EncPLResultSet> *results = [db executeQuery: @"SELECT id FROM example WHERE id = ?", [NSNumber numberWithInteger: 42]];
 * while ([results next]) {
 *     NSLog(@"Value of column id is %d", [results intForColumn: @"id"]);
 * }
 *
 * // Failure to close the result set will not leak memory, but may
 * // retain database resources until the instance is deallocated.
 * [results close];
 * </pre>
 *
 * @section prepared_stmt Prepared Statements
 *
 * Pre-compilation of SQL statements and advanced parameter binding
 * are supported by EncPLPreparedStatement. A prepared statement can
 * be constructed using -[EncPLDatabase prepareStatement:].
 *
 * <pre>
 * NSObject<EncPLPreparedStatement> *stmt = [db prepareStatement: @"INSERT INTO example (name, color) VALUES (?, ?)"];
 
 * // Bind the parameters
 * [stmt bindParameters: [NSArray arrayWithObjects: @"Widget", @"Blue", nil]];
 *
 * // Execute the INSERT
 * if ([stmt executeUpdate] == NO)
 *     NSLog(@"INSERT failed");
 *
 * </pre>
 *
 * @subsection named_params Name-based Parameter Binding
 *
 * Name-based parameter binding is also supported:
 *
 * <pre>
 * // Prepare the statement
 * NSObject<EncPLPreparedStatement> *stmt = [db prepareStatement: @"INSERT INTO test (name, color) VALUES (:name, :color)"];
 *
 * // Bind the parameters using a dictionary
 * NSMutableDictionary *parameters = [NSMutableDictionary dictionaryWithCapacity: 2];
 * [parameters setObject: @"Widget" forKey: @"name"];
 * [parameters setObject: @"Blue" forKey: @"color"];
 *
 * [stmt bindParameterDictionary: parameters];
 *
 * // Execute the INSERT
 * if ([stmt executeUpdate] == NO)
 *     NSLog(@"INSERT failed");
 *
 * </pre>
 */

/**
 * @page error_handling Error Handling Programming Guide
 *
 * Where a method may return an error, Plausible Database provides access to the underlying cause via an optional NSError argument.
 *
 * All returned errors will be a member of one of the below defined domains, however, new domains and error codes may be added at any time.
 * If you do not wish to report on the error cause, many methods support a simple form that requires no NSError argument.
 *
 * @section Error Domains, Codes, and User Info
 *
 * @subsection database_errors Database Errors
 *
 * Any errors in the database driver use the #EncPLDatabaseErrorDomain error domain, and and one of the error codes defined in #EncPLDatabaseError. Additionally, the
 * following keys will be available in the NSError user info dictionary:
 *
 * - #EncPLDatabaseErrorQueryStringKey - Query which caused the error (optional).
 * - #EncPLDatabaseErrorVendorErrorKey - The native database error code.
 * - #EncPLDatabaseErrorVendorStringKey - The native database error string.
 */

