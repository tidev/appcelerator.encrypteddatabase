/**
 * Appcelerator Titanium Mobile Modules
 * Copyright (c) 2010-2020 by Axway, Inc. All Rights Reserved.
 * Proprietary and Confidential - This source code is not for redistribution
 */
package appcelerator.encrypteddatabase;

/**
 * Enum type indicating the hashing algorithm type to be used by the SQLCipher library.
 * <p>
 * Provides methods to acquire the algorithm's unique IDs used by the module and SQLCipher library.
 */
public enum TiHashAlgorithmType {
	SHA1(1, "HMAC_SHA1", "PBKDF2_HMAC_SHA1"),
	SHA256(2, "HMAC_SHA256", "PBKDF2_HMAC_SHA256"),
	SHA512(3, "HMAC_SHA512", "PBKDF2_HMAC_SHA512");

	/** Unique integer ID used by this Titanium module in JavaScript to represent this algorithm. */
	private int tiIntId;

	/** Unique string ID to be assigned to SQLCipher library's "cipher_hmac_algorithm" PRAGMA variable. */
	private String cipherHmacStringId;

	/** Unique string ID to be assigned to SQLCipher library's "cipher_kdf_algorithm" PRAGMA variable. */
	private String cipherKdfStringId;

	/**
	 * Creates a new algorithm type using the given data.
	 * @param tiIntId Unique ID used in JavaScript to identify the algorithm type.
	 * @param cipherHmacStringId SQLCipher library's string ID to be assigned to PRAGMA "cipher_hmac_algorithm".
	 * @param cipherKdfStringId SQLCipher library's string ID to be assigned to PRAGMA "cipher_kdf_algorithm".
	 */
	private TiHashAlgorithmType(int tiIntId, String cipherHmacStringId, String cipherKdfStringId)
	{
		this.tiIntId = tiIntId;
		this.cipherHmacStringId = cipherHmacStringId;
		this.cipherKdfStringId = cipherKdfStringId;
	}

	/**
	 * Gets the SQLCipher library's string ID for this algorithm.
	 * Expected to be assigned to SQL PRAGMA variable "cipher_hmac_algorithm" or "cipher_default_hmac_algorithm".
	 * @return Returns the SQLCipher HMAC algorithm string identifier.
	 */
	public String toCipherHmacStringId()
	{
		return this.cipherHmacStringId;
	}

	/**
	 * Gets the SQLCipher library's string ID for this algorithm.
	 * Expected to be assigned to SQL PRAGMA variable "cipher_kdf_algorithm" or "cipher_default_kdf_algorithm".
	 * @return Returns the SQLCipher KDF algorithm string identifier.
	 */
	public String toCipherKdfStringId()
	{
		return this.cipherKdfStringId;
	}

	/**
	 * Gets the unique integer ID that this Titanium module uses to represent this algorithm
	 * in JavaScript such as module.HMAC_SHA1, module.HMAC_SHA2, etc.
	 * @return Returns the algorithm's unique integer identifier.
	 */
	public int toTiIntId()
	{
		return this.tiIntId;
	}

	/**
	 * Gets a human readable name identifying this algorithm type.
	 * @return Returns the algorithm's name.
	 */
	@Override
	public String toString()
	{
		return this.cipherHmacStringId;
	}

	/**
	 * Fetches an algorithm type matching the given unique Titanium module integer ID.
	 * This ID matches the value returned by TiHashAlgorithmType.toTiIntId().
	 * @param value The unique integer ID of the algorithm to search for.
	 * @return Returns a algorithm object. Returns null if given an invalid ID.
	 */
	public static TiHashAlgorithmType fromTiIntId(int value)
	{
		for (TiHashAlgorithmType nextObject : TiHashAlgorithmType.values()) {
			if ((nextObject != null) && (nextObject.tiIntId == value)) {
				return nextObject;
			}
		}
		return null;
	}

	/**
	 * Fetches an algorithm type matching a string ID assigned to SQLCipher library PRAGMA "cipher_hmac_algorithm".
	 * @param value The unique SQLCipher HMAC string ID of the algorithm to search for.
	 * @return Returns a algorithm object. Returns null if given an invalid ID.
	 */
	public static TiHashAlgorithmType fromCipherHmacStringId(String value)
	{
		for (TiHashAlgorithmType nextObject : TiHashAlgorithmType.values()) {
			if ((nextObject != null) && nextObject.cipherHmacStringId.equals(value)) {
				return nextObject;
			}
		}
		return null;
	}

	/**
	 * Fetches an algorithm type matching a string ID assigned to SQLCipher library PRAGMA "cipher_kdf_algorithm".
	 * @param value The unique SQLCipher KDF string ID of the algorithm to search for.
	 * @return Returns a algorithm object. Returns null if given an invalid ID.
	 */
	public static TiHashAlgorithmType fromCipherKdfStringId(String value)
	{
		for (TiHashAlgorithmType nextObject : TiHashAlgorithmType.values()) {
			if ((nextObject != null) && nextObject.cipherKdfStringId.equals(value)) {
				return nextObject;
			}
		}
		return null;
	}
}
