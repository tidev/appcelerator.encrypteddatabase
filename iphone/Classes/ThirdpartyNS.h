/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2013 by Appcelerator, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */

#ifndef Enc_ThirdpartyNS_h
#define Enc_ThirdpartyNS_h

#ifndef __ENC_NAMESPACE_PREFIX_
#define __ENC_NAMESPACE_PREFIX_	ENC
#endif

#ifndef __ENC_NS_SYMBOL
// Must have multiple levels of macros so that __ENC_NAMESPACE_PREFIX_ is
// properly replaced by the time the namespace prefix is concatenated.
#define __ENC_NS_REWRITE(ns, symbol) ns ## _ ## symbol
#define __ENC_NS_BRIDGE(ns, symbol) __ENC_NS_REWRITE(ns, symbol)
#define __ENC_NS_SYMBOL(symbol) __ENC_NS_BRIDGE(__ENC_NAMESPACE_PREFIX_, symbol)
#endif

// EncPlausibleDatabase
#ifndef EncPlausibleDatabase
#define EncPlausibleDatabase __ENC_NS_SYMBOL(EncPlausibleDatabase)
#endif
#ifndef EncPLDatabaseException
#define EncPLDatabaseException __ENC_NS_SYMBOL(EncPLDatabaseException)
#endif
#ifndef EncPLDatabaseErrorDomain
#define EncPLDatabaseErrorDomain __ENC_NS_SYMBOL(EncPLDatabaseErrorDomain)
#endif
#ifndef EncPLDatabaseErrorQueryStringKey
#define EncPLDatabaseErrorQueryStringKey __ENC_NS_SYMBOL(EncPLDatabaseErrorQueryStringKey)
#endif
#ifndef EncPLDatabaseErrorVendorErrorKey
#define EncPLDatabaseErrorVendorErrorKey __ENC_NS_SYMBOL(EncPLDatabaseErrorVendorErrorKey)
#endif
#ifndef EncPLDatabaseErrorVendorStringKey
#define EncPLDatabaseErrorVendorStringKey __ENC_NS_SYMBOL(EncPLDatabaseErrorVendorStringKey)
#endif
#ifndef EncPLDatabase
#define EncPLDatabase __ENC_NS_SYMBOL(EncPLDatabase)
#endif
#ifndef EncPLPreparedStatement
#define EncPLPreparedStatement __ENC_NS_SYMBOL(EncPLPreparedStatement)
#endif
#ifndef EncPLResultSet
#define EncPLResultSet __ENC_NS_SYMBOL(EncPLResultSet)
#endif
#ifndef EncPLSqliteException
#define EncPLSqliteException __ENC_NS_SYMBOL(EncPLSqliteException)
#endif
#ifndef EncPLSqliteDatabase
#define EncPLSqliteDatabase __ENC_NS_SYMBOL(EncPLSqliteDatabase)
#endif
#ifndef EncPLSqlitePreparedStatement
#define EncPLSqlitePreparedStatement __ENC_NS_SYMBOL(EncPLSqlitePreparedStatement)
#endif
#ifndef EncPLSqliteResultSet
#define EncPLSqliteResultSet __ENC_NS_SYMBOL(EncPLSqliteResultSet)
#endif
#ifndef EncPLSqliteParameterStrategy
#define EncPLSqliteParameterStrategy __ENC_NS_SYMBOL(EncPLSqliteParameterStrategy)
#endif
#ifndef EncPLSqliteArrayParameterStrategy
#define EncPLSqliteArrayParameterStrategy __ENC_NS_SYMBOL(EncPLSqliteArrayParameterStrategy)
#endif
#ifndef EncPLSqliteDictionaryParameterStrategy
#define EncPLSqliteDictionaryParameterStrategy __ENC_NS_SYMBOL(EncPLSqliteDictionaryParameterStrategy)
#endif

#endif
