package ch.k43.util;

import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Securely hash, store, and verify passwords using PBKDF2 with the PBKDF2WithHmacSHA512 algorithm. A secure,
 * randomly generated 32-byte salt is used, along with an optional pepper value. The key length is set to 512 bits.
 * If not specified, the number of hash iterations is set to a generated value between 500'000 and 1'000'000.
 * 
 * @see isPasswordValid
 * 
 * @since 2025.05.17
 */
public final class KPasswordVault {

	// Declarations
	private static final	String	HASH_ALGORITHM			= "PBKDF2WithHmacSHA512";
	private static final	int		HASH_KEY_SIZE			= 512;
	private static final	int		SALT_SIZE_BYTES			= 32;
	private	static final	int		DEFAULT_ITERATIONS_LOW	= 500_000;
	private	static final	int		DEFAULT_ITERATIONS_HIGH	= 1_000_000;
	
	private 			 	byte[]	gPasswordHash			= null;
	private 				byte[]	gSalt					= null;
	private					long	gHashTimeMs				= -1;
	private  				int		gIterations				= -1;

	/**
	 * Hash the given password with a random generated iteration count between 500'000 and 1'000'000.
	 * 
	 * @param argPassword	Clear text password to be hashed
	 */
	public KPasswordVault(char[] argPassword) {
		this(argPassword, K.getRandomInt(DEFAULT_ITERATIONS_LOW, DEFAULT_ITERATIONS_HIGH), null);
	}
	
	/**
	 * Create a password vault with the given data. This constructor is used to initialize a password vault with previous
	 * retrieved data which can then be used to validate a given clear text password.
	 * 
	 * @param argSalt			Salt
	 * @param argIterations		Number of iterations
	 * @param argPasswordHash	Password hash
	 */
	public KPasswordVault(byte[] argSalt, int argIterations, byte[] argPasswordHash) {
	
		// Check arguments
		KLog.argException(K.isEmpty(argSalt) || argSalt.length != SALT_SIZE_BYTES, "KPasswordVault: argSalt must be " + SALT_SIZE_BYTES + " bytes long");
		KLog.argException(argIterations < 1_000 || argIterations > 10_000_000, "KPasswordVault: argIterations must be between 1000 and 10000000");
		KLog.argException(K.isEmpty(argPasswordHash) || argPasswordHash.length != 64, "KPasswordVault: argPasswordHash must be 64 bytes long");
		
		gSalt			= argSalt;
		gIterations		= argIterations;
		gPasswordHash	= argPasswordHash;
	}

	/**
	 * Hash the given password with a random generated iteration count between 500'000 and 1'000'000 and an optional pepper value.
	 * 
	 * @param argPassword	Clear text password to be hashed
	 * @param argPepper		Optional pepper to be added to the password
	 */
	public KPasswordVault(char[] argPassword, char[] argPepper) {
		this(argPassword, K.getRandomInt(DEFAULT_ITERATIONS_LOW, DEFAULT_ITERATIONS_HIGH), argPepper);
	}
	
	/**
	 * Hash the given password for the specified number of iterations.
	 * 
	 * @param argPassword	Clear text password to be hashed
	 * @param argIterations	Number of iterations (1_000 - 10_000_000)
	 */
	public KPasswordVault(char[] argPassword, int argIterations) {
		this(argPassword, argIterations, null);
	}
	
	/**
	 * Hash the given password for the specified number of iterations.
	 * 
	 * @param argPassword	Clear text password to be hashed
	 * @param argIterations	Number of iterations (1_000 - 10_000_000)
	 * @param argPepper		Optional pepper to be added to the password and salt
	 */
	public KPasswordVault(char[] argPassword, int argIterations, char[] argPepper) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argPassword), "KPasswordVault: argPassword must not be empty");
		KLog.argException(argIterations < 1_000 || argIterations > 10_000_000, "KPasswordVault: argIterations must be between 1000 and 10000000");
		
		// Start timer
		KTimer timer = new KTimer();
		
		// Generate unique 32-byte salt
		gSalt = K.getRandomBytes(SALT_SIZE_BYTES);
		
		// Save iterations
		gIterations = argIterations;
		
		// Hash password with salt
		gPasswordHash = hashPassword(argPassword, gSalt, gIterations, argPepper);
		
		KLog.debug("Password hashed ({}, {} byte salt{}, {} bit key size, {} iterations, {} ms)",
				HASH_ALGORITHM,
				SALT_SIZE_BYTES,
				!K.isEmpty(argPepper) ? " (plus pepper)" : "",
				HASH_KEY_SIZE,
				argIterations,
				timer.getElapsedMilliseconds());
	}
	
	/**
	 * Clear all object variables.
	 */
	public final void clear() {
		
		// Overwrite variables with zeroes
		if (!K.isEmpty(gSalt)) {
			Arrays.fill(gSalt, (byte) 0);
		}

		if (!K.isEmpty(gPasswordHash)) {
			Arrays.fill(gPasswordHash, (byte) 0);
		}

		// Reset variables
		gSalt			= null;
		gIterations		= -1;
		gPasswordHash	= null;
		gHashTimeMs		= -1L;
	}
	
	/**
	 * Compare hash values with constant-time comparison thus preventing attackers from learning where the values differ with timing attacks.
	 * 
	 * @param arg1	First value to test
	 * @param arg2	Second value to test
	 * 
	 * @return true (equal values) false (not equal values)
	 */
	private final boolean constantTimeCompare(byte[] arg1, byte[] arg2) {
		
		// Check arguments
		KLog.argException(K.isEmpty(arg1) || K.isEmpty(arg2), "KPasswordVault.constantTimeCompare(): arg1 and arg2 must not be empty");

		if (arg1.length != arg2.length) {
			return false;
		}

		// Compare all bytes to avoid premature return
	    int result = 0;
	    
	    // XOR all bytes and OR them into result
	    for (int index = 0; index < arg1.length; index++) {
	        result |= arg1[index] ^ arg2[index];
	    }
	    
	    return (result == 0);
	}
	
	/**
	 * Return the elapsed time for the password hash generation.
	 * 
	 * @return Time in milliseconds
	 */
	public final long getHashTimeMs() {
		return gHashTimeMs;
	}
	
	/**
	 * Return the used iteration count.
	 * 
	 * @return Iterations
	 */
	public final int getIterations() {
		return gIterations;
	}
	
	/**
	 * Return the password hash.
	 * 
	 * @return Password hash
	 */
	public final byte[] getPasswordHash() {
		return gPasswordHash;
	}
	
	/**
	 * Return the used salt.
	 * 
	 * @return Salt
	 */
	public final byte[] getSalt() {
		return gSalt;
	}
	
	/**
	 * Hash the given password with the specified salt and the given iteration.
	 * 
	 * @param argPassword	Clear text password to be hashed
	 * @param argSalt		Salt to be added to the password
	 * @param argIterations	Number of iterations used (1_000 - 10_000_000)
	 * @param argPepper		Optional petter value
	 * 
	 * @return	Hashed password
	 */
	private final byte[] hashPassword(char[] argPassword, byte[] argSalt, int argIterations, char[] argPepper) {
		
		// Check arguments
		KLog.argException(K.isEmpty(argPassword) || K.isEmpty(argSalt), "KPasswordVault.hashPassword(): argPassword/argSalt must not be empty");
		KLog.argException(argIterations < 1_000 || argIterations > 10_000_000, "KPasswordVault.hashPassword(): argIterations must be between 1000 and 10000000");
		
		// Declarations
		char[]	combinedPassword	= new char[argPassword.length + (K.isEmpty(argPepper) ? 0 : argPepper.length)];
		byte[]	passwordHash		= new byte[0];
		KTimer	timer 				= new KTimer();
		
		// Hash given password with PBKDF2WithHmacSHA512 and key size 512 for the specified number of iterations
		try {
			// Concatenate pepper to password (if present)
	        System.arraycopy(argPassword, 0, combinedPassword, 0, argPassword.length);
	        
	        if (!K.isEmpty(argPepper)) {
		        System.arraycopy(argPepper, 0, combinedPassword, argPassword.length, argPepper.length);
			}
			
			// Hash the password
	        PBEKeySpec keySpec = new PBEKeySpec(combinedPassword, argSalt, argIterations, HASH_KEY_SIZE);
	        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(HASH_ALGORITHM);
	        passwordHash = keyFactory.generateSecret(keySpec).getEncoded();
	        
		} catch (Exception e) {
			KLog.argException("Unable to hash password: {}", e.toString());
		} finally {
			// Clear sensitive data from memory
			Arrays.fill(combinedPassword, '\0');
			combinedPassword = null;
		}
		
		// Save elapsed time of hash generation
		gHashTimeMs = timer.getElapsedMilliseconds();
		
		KLog.debug(gHashTimeMs < 100L, "KPasswordVault.hashPassword(): Hashing took less than 100 ms - Consider increasing number of iterations");
		
		return passwordHash;
	}

	/**
	 * Hash the password and compare it against the stored password hash.
	 * 
	 * @param 	argPassword	Clear text password to be checked
	 * @return	true if password matches
	 */
	public final boolean isPasswordValid(char[] argPassword) {
		return isPasswordValid(argPassword, null);
	}
	
	/**
	 * Hash the password with the pepper and compare it against the stored password hash.
	 * 
	 * @param 	argPassword	Clear text password to be checked
	 * @param	argPepper	Optional pepper which was added to the password and salt
	 * @return	true if password matches
	 */
	public final boolean isPasswordValid(char[] argPassword, char[] argPepper) {

		// Check arguments
		KLog.argException(K.isEmpty(argPassword), "KPasswordVault.isPasswordValid(): argPassword must not be empty");
		
		// Hash password with salt
		byte[] passwordHash	= hashPassword(argPassword, gSalt, gIterations, argPepper);
		
		// Check if hashes match by using a constant-time comparison method
		return (constantTimeCompare(passwordHash, gPasswordHash));
	}

	/**
	 * String representation of object.
	 */
	@Override
	public String toString() {
		return "KPasswordVault [gSalt=" + Arrays.toString(gSalt) + ", gIterations=" + gIterations + ", gPasswordHash="
				+ Arrays.toString(gPasswordHash) + ", gHashTimeMs=" + gHashTimeMs + "]";
	}
}
