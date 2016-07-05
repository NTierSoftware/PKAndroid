package org.peacekeeper.crypto;
/*PeaceKeeper Cryptographic Security Policy:

        Asymmetric Key Generation : ECDSA with 256 bits
        Asymmetric Signature : ECDSA for P-256
        SecurityGuard Digest : SHA-256
        Symmetric Key Generation : AES
        Symmetric Key Length : 256
        Symmetric Encryption : AES in CTR (Counter) mode, with appended HMAC.
        Certificate Format : X.509v3
        Random ID Size : 256 bits from /dev/urandom.
        Password Encryption : bcrypt

//http://developer.android.com/training/articles/keystore.html
//http://developer.android.com/reference/android/security/keystore/KeyProtection.html
//http://www.bouncycastle.org/wiki/display/JA1/X.509+Public+Key+Certificate+and+Certification+Request+Generation
//http://www.bouncycastle.org/wiki/display/JA1/X.509+Public+Key+Certificate+and+Certification+Request+Generation#X.509PublicKeyCertificateandCertificationRequestGeneration-Version3CertificateCreation
//http://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java

*/


import android.util.Base64;

import org.json.*;
import org.peacekeeper.exception.*;
import org.peacekeeper.util.pkUtility;
import org.slf4j.*;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x500.*;
import org.spongycastle.asn1.x500.style.*;
import org.spongycastle.asn1.x509.*;
import org.spongycastle.cert.*;
import org.spongycastle.cert.jcajce.*;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.operator.*;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.pkcs.*;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.spongycastle.util.io.pem.PemObject;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.KeyStore.*;
import java.security.Provider.Service;
import java.security.cert.*;
import java.util.*;

import javax.crypto.SecretKey;


// http://stackoverflow.com/questions/18244630/elliptic-curve-with-digital-signature-algorithm-ecdsa-implementation-on-bouncy
final public class SecurityGuard{
//begin static
static private final Logger mLog = LoggerFactory.getLogger( SecurityGuard.class );
static private final Provider PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider();

static private final char[] keyStorePW = "PeaceKeeperKeyStorePW".toCharArray();

static private final String ECDSA = "ECDSA"
							, SHA256withECDSA = "SHA256withECDSA"
							, NamedCurve = "P-256" //"secp256r1"
							, providerName = PROVIDER.getName()
							, Alias = "PK." //=PeaceKeeper
							, pubKeyAlias  = Alias + "pub"
							, priKeyAlias  = Alias + "pri"
							, certKeyAlias = Alias + "Cert"
							, keyStoreType = "pkcs12"
							, keyStoreFilename = Alias + keyStoreType
							, deviceID = UUID.randomUUID().toString()
							, emailAddr = "ntiersoftwareengineering@gmail.com";

static private KeyPair mKEYPAIR = null;
static private KeyStore mKEYSTORE = null;

static private byte[] hash = null;
//end static
private String message = null;

public enum entryType {deviceID, keeperID}

public SecurityGuard( final String aMessage ){
	initSecurity();
	this.message = aMessage;
}

static public void initSecurity(){ initSecurity( PROVIDER ); }

//https://msdn.microsoft.com/en-us/library/windows/desktop/aa376502(v=vs.85).aspx
// http://stackoverflow.com/questions/20532912/generating-the-csr-using-bouncycastle-api
// http://www.bouncycastle.org/wiki/display/JA1/X.509+Public+Key+Certificate+and+Certification+Request+Generation#X.509PublicKeyCertificateandCertificationRequestGeneration-SubjectAlternativeName
static public PKCS10CertificationRequest genCSR(){
	try{
		KeyPair pair = getKeyPair();

		GeneralNames subjectAltName = new GeneralNames(
				new GeneralName(GeneralName.rfc822Name, emailAddr));

		ExtensionsGenerator extnsnGenr = new ExtensionsGenerator();
		extnsnGenr.addExtension(Extension.subjectAlternativeName, false, subjectAltName);


//http://stackoverflow.com/questions/12863235/csr-generated-with-bouncycastle-missing-public-key-and-attributes
//http://stackoverflow.com/questions/34169954/create-pkcs10-request-with-subject-alternatives-using-bouncy-castle-in-java
//		//extnsnGenr.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
		//extnsnGenr.addExtension(Extension.subjectAlternativeName, false, new GeneralName(GeneralName.rfc822Name, emailAddr) );

		PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
				getX500Name()
				, pair.getPublic() )
//				.addAttribute(Extension.subjectAlternativeName, new DEROctetString( subjectAltName)   )
				.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extnsnGenr.generate());
				//.setLeaveOffEmptyAttributes(false)
				;

		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder( SHA256withECDSA );

		ContentSigner signer = csBuilder.build( pair.getPrivate() );
		PKCS10CertificationRequest CSR = p10Builder.build( signer );
	return CSR;

	}catch ( IOException| OperatorCreationException X ){
	//}catch ( OperatorCreationException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "registrations err", X );
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}
}//genCSR


//Get the CertSignRequest as a PEM formatted String
static public String toPEM( PKCS10CertificationRequest CSR ){
	StringWriter str = new StringWriter();
	JcaPEMWriter pemWriter = new JcaPEMWriter( str );
	String retVal;
	try{
		PemObject pemObject = new PemObject( "CERTIFICATE REQUEST", CSR.getEncoded() );
		pemWriter.writeObject( pemObject );

		pemWriter.close();
		str.close();
		retVal = str.toString();
	}catch ( IOException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "toPEM err", X );
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}
	return retVal;
}//toPEM

//see https://github.com/nelenkov/ecdh-kx/blob/master/src/org/nick/ecdhkx/Crypto.java
static public void listAlgorithms( String algFilter ){
	java.security.Provider[] providers = java.security.Security.getProviders();
	for ( java.security.Provider p : providers ){
		String providerStr = String.format( "%s/%s/%f\n", p.getName(), p.getInfo(),
		                                    p.getVersion() );
		mLog.debug( providerStr );
		Set< Service > services = p.getServices();
		List< String > algs = new ArrayList<>();
		for ( Service s : services ){
			boolean match = true;
			if ( algFilter != null ){
				match = s.getAlgorithm().toLowerCase()
				         .contains( algFilter.toLowerCase() );
			}

			if ( match ){
				String algStr = String.format( "\t%s/%s/%s", s.getType(),
				                               s.getAlgorithm(), s.getClassName() );
				algs.add( algStr );
			}
		}

		Collections.sort( algs );
		for ( String alg : algs ) mLog.debug( "\t" + alg );
		mLog.debug( "" );
	}
}//listAlgorithms

static public void listCurves(){
	mLog.debug( "Supported named curves:" );
	java.util.Enumeration< ? > names = SECNamedCurves.getNames();
	while ( names.hasMoreElements() ){ mLog.debug( "\t" + names.nextElement() ); }
}//listCurves

static public void listKeyStore(){
	try{
		Enumeration< String > aliases = mKEYSTORE.aliases();

		mLog.debug( ( aliases.hasMoreElements() ? "" : "Empty" ) + "mKEYSTORE contents" );
		while ( aliases.hasMoreElements() ){
			mLog.debug( ":\t" + aliases.nextElement().toString() );
		}
	}catch ( Exception X ){ mLog.debug( "Empty mKEYSTORE contents" ); }
}//listKeyStore

static public String listProviders(){
	Provider[] providers = Security.getProviders();
	StringBuilder list = new StringBuilder().append( "Num providers: " + providers.length );
	int i = 0;
	for ( Provider p : providers ){
		list.append( "\n\tProvider" + ++i + ": " + p.getName() + "\t info: " + p.getInfo() );
		Set< Provider.Service > services = p.getServices();
		list.append( "\t\tNum services: " + services.size() );
		int k = 0;
		for ( Service s : services ){
			list.append( "\n\t\t\tService" + ++k + ": " + "\ttype: " + s.getType() + "\talgo: " + s
					.getAlgorithm() );
		}
	}

	return list.toString();
}//listProviders

//http://stackoverflow.com/questions/16412315/creating-custom-x509-v3-extensions-in-java-with-bouncy-castle
//http://www.ietf.org/rfc/rfc3280.txt
/*

private static Extension UniqID(){
	byte[] UniqID = null;
	try {
		String id = pkUtility.getInstance().getUniqDeviceID().toString();
		UniqID = id.getBytes(charset);
		mLog.debug("getUniqDeviceID():\t" + id);
	} catch (java.io.UnsupportedEncodingException e) {
		e.printStackTrace();
	}
//	ASN1ObjectIdentifier asn1iod = new ASN1ObjectIdentifier("1.2.3.4");
//	return new Extension( asn1iod, true, UniqID);
return new Extension( Extension.subjectAlternativeName, true, UniqID);
}//UniqID
*/

//Moves provider to first place
static void initSecurity( Provider provider ){
	Security.removeProvider( provider.getName() );

	int insertProviderAt = Security.insertProviderAt( provider, 1 );
	//mLog.debug( "insertProviderAt:\t" + Integer.toString( insertProviderAt ) );
	//mLog.debug( listProviders() );
}//initSecurity

static public void setEntry(final JSONObject response ){
	mKEYSTORE = getKeyStore();
	listKeyStore();

	SecretKey deviceID = new SecretKey(){
		@Override public String getAlgorithm(){ return null; }

		@Override public String getFormat(){ return null; }

		@Override public byte[] getEncoded(){
			String retVal;
			try{
				retVal = response.getString( entryType.deviceID.name() );
			}catch ( JSONException aE ){
				aE.printStackTrace();
				retVal = "";
			}
			return retVal.getBytes();
		}
	},
	keeperID = 	new SecretKey(){
		@Override public String getAlgorithm(){ return null; }

		@Override public String getFormat(){ return null; }

		@Override public byte[] getEncoded(){
			String retVal;
			try{
				retVal = response.getString( entryType.keeperID.name() );
			}catch ( JSONException aE ){
				aE.printStackTrace();
				retVal = "";
			}
			return retVal.getBytes();
		}
	};



	KeyStore.SecretKeyEntry deviceIDEntry = new SecretKeyEntry( deviceID )
							, keeperIDEntry = new SecretKeyEntry( keeperID );
	try{
		mKEYSTORE.setEntry(entryType.deviceID.name(), deviceIDEntry, null   );
		mKEYSTORE.setEntry(entryType.keeperID.name(), keeperIDEntry, null   );
	}catch ( KeyStoreException aE ){
		aE.printStackTrace();
	}

	storeKey();
}//setEntry

static public String getEntry(entryType aEntryType){
	String retVal;
	try{
		KeyStore.SecretKeyEntry secretKeyEntry = (SecretKeyEntry) mKEYSTORE.getEntry( aEntryType.name(), null );
		retVal = new String( secretKeyEntry.getSecretKey().getEncoded());
	}catch ( NoSuchAlgorithmException |UnrecoverableEntryException |KeyStoreException X ){
		mLog.error( X.getMessage() );
		retVal = ""; }
return retVal;
}

static private KeyPair getKeyPair(){
	mLog.debug( "mKEYPAIR " + ( mKEYPAIR == null ? "" : "NOT " ) + "null" );

	if ( mKEYPAIR != null ){return mKEYPAIR; }
	mLog.debug( "mKEYSTORE " + ( mKEYSTORE == null ? "" : "NOT " ) + "null" );

	mKEYSTORE = getKeyStore();
	listKeyStore();

	try{
		if ( mKEYSTORE.containsAlias( priKeyAlias ) ){
			PrivateKey privateKey = (PrivateKey) mKEYSTORE.getKey( priKeyAlias, keyStorePW );
			PublicKey  publicKey  = (PublicKey)  mKEYSTORE.getKey( pubKeyAlias, keyStorePW );
			//mKEYPAIR = new KeyPair(publicKey, privateKey );
		}//if
		else genKeyPair();
	}//try
	catch ( KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "Crypto getKeyPair err", X );
		mLog.error( CRYPTOERR.toString() );
		mKEYPAIR = null;
		throw CRYPTOERR;
	}

	return mKEYPAIR;
}//getKeyPair


static private KeyStore getKeyStore(){
	if ( mKEYSTORE == null ){
		if ( keyStoreFileExists() ){
			try{
				InputStream is = new FileInputStream( keyStoreFilename );
				mKEYSTORE = KeyStore.getInstance( keyStoreType, providerName );
				mKEYSTORE.load( is, keyStorePW );
				is.close();
			}catch ( Exception X ){ genKeyStore(); }
		}
		else genKeyStore();
	}
	return mKEYSTORE;
}//getKeyStore

static private boolean keyStoreFileExists(){
	return new File( pkUtility.getInstance().getAppDataDir()
			, keyStoreFilename )
			.isFile();
}//keyStoreFileExists

//http://stackoverflow.com/questions/16412315/creating-custom-x509-v3-extensions-in-java-with-bouncy-castle
static private final ASN1ObjectIdentifier device = new ASN1ObjectIdentifier( "2.5.6.14");
static private X500Name getX500Name(){
/*
	"userId"		: <uuid>,		# "1ccf1ca9-ddf1-4d30-ba50-b0122db35f32"
	"deviceId"		: <uuid>,		# "e53ed886-0853-419a-96e3-8ec33d644853"
	"name"			: <string>,		# "Vince"
*/

//	final String testPostalCode = "94602-4105";
	final String testPostalCode = "92117";
	return new X500NameBuilder( BCStyle.INSTANCE )
//			.addRDN( BCStyle.CN, Alias )
			.addRDN( BCStyle.CN, "testCN" )
			//.addRDN( BCStrictStyle.EmailAddress, emailAddr )
			.addRDN( BCStyle.POSTAL_CODE, testPostalCode )
			.addRDN( BCStyle.C, "US" )
			.addRDN( device, deviceID )

			.build();
}//getX500Name

// http://www.programcreek.com/java-api-examples/index.php?class=org.spongycastle.cert.X509v3CertificateBuilder&method=addExtension
//private static X509Certificate genRootCertificate( KeyPair kp, String CN){
static private X509Certificate genRootCertificate( KeyPair kp ){
	X509Certificate certificate;
	try{
		final Calendar calendar = Calendar.getInstance();
		final Date now = calendar.getTime();
		//expires in one day - just enough time to be replaced by CA CERT
		calendar.add( Calendar.DATE, 1 );
		final Date expire = calendar.getTime();


		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder( SHA256withECDSA );

		ContentSigner signer = csBuilder.build( kp.getPrivate() );

/*
		DefaultAlgorithmNameFinder daf = new DefaultAlgorithmNameFinder();
		String algo = daf.getAlgorithmName( signer.getAlgorithmIdentifier() );
		mLog.debug("genroot signer.getAlgorithmIdentifier(): \t" + algo);
*/

		BigInteger certSerialnum = new BigInteger( 80, new SecureRandom() );//new Random()),

		X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
				getX500Name(), //builder.build(),
				certSerialnum,
				now, //new Date(System.currentTimeMillis() - 50000),
				expire,
				getX500Name(),
				kp.getPublic()
		);

		X509CertificateHolder certHolder = certGen.build( signer );

		certificate = new JcaX509CertificateConverter()
				.setProvider( PROVIDER.getName() )
				.getCertificate( certHolder );
	}//try
	catch ( OperatorCreationException | CertificateException X ){//| CertIOException X ) {
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set(
				"Crypto selfSignedCert gen err", X );
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}

	return certificate;
}//genRootCertificate()

static private void genKeyStore(){
	unRegister();
	try{
		mKEYSTORE = KeyStore.getInstance( keyStoreType, providerName );
//Pass null as the stream argument to initialize an empty KeyStore or to initialize a KeyStore which does not rely on an InputStream.
		mKEYSTORE.load( null, keyStorePW );
		genKeyPair();
		X509Certificate[] selfSignedCert = new X509Certificate[ 1 ];
		selfSignedCert[ 0 ] = genRootCertificate( mKEYPAIR );

		mKEYSTORE.setCertificateEntry( certKeyAlias, selfSignedCert[ 0 ] );
		mKEYSTORE.setKeyEntry( priKeyAlias, mKEYPAIR.getPrivate(), keyStorePW, selfSignedCert );
		storeKey();

		mLog.debug( "mKEYSTORE init'd" );
	}catch ( KeyStoreException | NoSuchProviderException
			| IOException | NoSuchAlgorithmException | CertificateException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "genKeyStore err", X );
		;
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}
}//genKeyStore

static private void genKeyPair(){
	KeyPairGenerator kpg;
	try{
		kpg = KeyPairGenerator.getInstance( ECDSA, providerName );
	}catch ( NoSuchAlgorithmException | NoSuchProviderException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "genKeyPair err", X );
		;
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}
	try{
		ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec( NamedCurve );
		kpg.initialize( ecSpec, new SecureRandom() );
		SecureRandom x = new SecureRandom();

/*
			kpg.initialize(
			new android.security.keystore.KeyGenParameterSpec.Builder(
			Alias,
			android.security.keystore.KeyProperties.PURPOSE_SIGN)
			.setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec(NamedCurve))
			.setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256
			)
			// Only permit the private key to be used if the user authenticated
			// within the last five minutes.
			//.setUserAuthenticationRequired(true)
			//.setUserAuthenticationValidityDurationSeconds(5 * 60)

			.build(),
			new java.security.SecureRandom());*/
	}catch ( java.security.InvalidAlgorithmParameterException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set(
				"genKeyPair initialize err", X );
		;
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}

	mKEYPAIR = kpg.generateKeyPair();
}//genKeyPair

static private byte[] genNonce(){
// http://stackoverflow.com/questions/5683206/how-to-create-an-array-of-20-random-bytes
	final int nonceLen = 32;
	byte[] nonce = new byte[ nonceLen ];
	new SecureRandom().nextBytes( nonce );
return nonce;
}

//https://github.com/boeboe/be.boeboe.spongycastle/commit/5942e4794c6f950a95409f2612fad7de7cc49b33
static private void storeKey(){
	String path = pkUtility.getInstance().getAppDataDir();

	mLog.debug( "storeKey path:\t" + path );

	File file = new File( path, keyStoreFilename );
	file.getParentFile().mkdirs();
	mLog.debug( "storeKey mKEYSTORE file: " + file.getAbsolutePath() );

	try{
		mKEYSTORE.store( new FileOutputStream( file ), keyStorePW );
	}catch ( FileNotFoundException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "storeKey err", X );

		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}catch ( CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "storeKey err", X );

		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}
}//storeKey

static private boolean unRegister(){//purges mKEYSTORE
	boolean unRegister = !keyStoreFileExists();
	if ( !unRegister ){
		try{
			String path = pkUtility.getInstance().getAppDataDir();
			File fKeyStore = new File( path, keyStoreFilename );
			InputStream is = new FileInputStream( fKeyStore );
			mKEYSTORE = KeyStore.getInstance( keyStoreType, providerName );
			mKEYSTORE.load( is, keyStorePW );
			listKeyStore();

			Enumeration< String > aliases = mKEYSTORE.aliases();
			while ( aliases.hasMoreElements() ){
				mKEYSTORE.deleteEntry( aliases.nextElement().toString() );
			}
			is.close();
			mKEYSTORE = null;
			mKEYPAIR = null;
			unRegister = fKeyStore.delete();
		}catch ( Exception X ){unRegister = false; }
	}
	return unRegister;
}//unRegister()

/* Definition of Registration:
KeyStore contains valid certificate
 */
/*
static private boolean isRegistered(){//TODO isRegistered
	boolean isRegistered = false;

	if ( mKEYSTORE == null ){
		if ( keyStoreFileExists() ){
		}
	}
	return isRegistered;
}//isRegistered
*/

/*
public boolean verify(){
	boolean verify;
	try{
		Signature ecdsaVerify = Signature.getInstance( SHA256withECDSA );
		ecdsaVerify.initVerify( getKeyPair().getPublic() );
		ecdsaVerify.update( this.message.getBytes( charset ) );
		verify = ecdsaVerify.verify( getSignature() );

	}catch ( NoSuchAlgorithmException | InvalidKeyException | SignatureException | UnsupportedEncodingException X ){
		verify = false;
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "crypto verify err", X );
		mLog.error( CRYPTOERR.toString() );
	}

	return verify;
}//verify
*/

//http://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash/23273249#23273249
@Override
public String toString(){
	if ( this.hash == null ) return null;

	String hashStr = new BigInteger( 1, this.hash ).toString( 16 );

// Now we need to zero pad it if you actually want the full 32 chars.
	while ( hashStr.length() < 32 ){ hashStr = "0" + hashStr; }


	StringBuilder retVal = new StringBuilder( "SecurityGuard:\t" )
			.append( this.message )
			.append( "\tHash: " ).append( hashStr );

	return retVal.toString();
}

/*
private byte[] getSignature(){
	if ( signature == null )
		try{
			KeyPair keyPair = getKeyPair();
			Signature ecdsaSign = Signature.getInstance( SHA256withECDSA );

			ecdsaSign.initSign( keyPair.getPrivate() );
			ecdsaSign.update( message.getBytes( charset ) );
			signature = ecdsaSign.sign();
		}catch ( NoSuchAlgorithmException | InvalidKeyException | SignatureException | UnsupportedEncodingException X ){
			X.printStackTrace();
			pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "Crypto Signature err",
			                                                                 X );
			mLog.error( CRYPTOERR.toString() );
			signature = null;
			throw CRYPTOERR;
		}//catch

	return signature;
}//getSignature
*/

/*
instructions for generating a PKC authentication-token:
		- Generate a random 32-byte Nonce in a byte-array, using your platform's equivalent of linux' os.urandom( 32 ).
		- Sign the Nonce with the user's PrivateKey, storing the signature in a byte-array.
		- Concatenate the Nonce and the Signature into a single byte-array.
		- Encode the byte-array as a base64 string - this bocomes our security-token.
		- Combine the user's userId, deviceId, and security-token into a string with the following format:

		"PEACEKEEPER-TOKEN userId,deviceId:security-token”
*/
public static String getAuthToken(){
	StringBuilder secToken = new StringBuilder() ;
	try{
		byte[] nonce = genNonce();

		Signature signature = Signature.getInstance( SHA256withECDSA );

		signature.initSign( getKeyPair().getPrivate() );
		signature.update( nonce );

		byte[] sign = signature.sign()
			   , securityToken = new byte[nonce.length + sign.length];
		System.arraycopy(nonce, 0, securityToken, 0, nonce.length);
		System.arraycopy(sign, 0, securityToken, nonce.length, sign.length);
//TODO userid???
		secToken.append("PEACEKEEPER-TOKEN ")
				.append( getEntry( entryType.deviceID ) )
				.append( ":" )
				.append( Base64.encodeToString( securityToken, Base64.DEFAULT ) );

	}
	catch ( SignatureException |NoSuchAlgorithmException |InvalidKeyException X ){ mLog.error( X.getMessage() ); }

return secToken.toString();
}//getAuthToken
}//class SecurityGuard

/*

//http://www.androidauthority.com/use-android-keystore-store-passwords-sensitive-information-623779/
public void encryptString(String alias) {
	try {
		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
		RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

		// Encrypt the text
		String initialText = startText.getText().toString();
		if(initialText.isEmpty()) {
			Toast.makeText(this, "Enter text in the 'Initial Text' widget", Toast.LENGTH_LONG).show();
			return;
		}

		Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
		input.init(Cipher.ENCRYPT_MODE, publicKey);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		CipherOutputStream cipherOutputStream = new CipherOutputStream(
				                                                              outputStream, input);
		cipherOutputStream.write(initialText.getBytes("UTF-8"));
		cipherOutputStream.close();

		byte [] vals = outputStream.toByteArray();
		encryptedText.setText(Base64.encodeToString(vals, Base64.DEFAULT));
	} catch (Exception e) {
		Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
		Log.e(TAG, Log.getStackTraceString(e));
	}
}

public void decryptString(String alias) {
	try {
		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
		RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();

		Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
		output.init(Cipher.DECRYPT_MODE, privateKey);

		String cipherText = encryptedText.getText().toString();
		CipherInputStream cipherInputStream = new CipherInputStream(
				                                                           new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
		ArrayList<Byte> values = new ArrayList<>();
		int nextByte;
		while ((nextByte = cipherInputStream.read()) != -1) {
			values.add((byte)nextByte);
		}

		byte[] bytes = new byte[values.size()];
		for(int i = 0; i < bytes.length; i++) {
			bytes[i] = values.get(i).byteValue();
		}

		String finalText = new String(bytes, 0, bytes.length, "UTF-8");
		decryptedText.setText(finalText);

	} catch (Exception e) {
		Toast.makeText(this, "Exception " + e.getMessage() + " occured", Toast.LENGTH_LONG).show();
		Log.e(TAG, Log.getStackTraceString(e));
	}
}
*/


//https://msdn.microsoft.com/en-us/library/windows/desktop/aa376502(v=vs.85).aspx
// http://stackoverflow.com/questions/20532912/generating-the-csr-using-bouncycastle-api
/*
static public PKCS10CertificationRequest genCSRold(){
	KeyPair pair = getKeyPair();
	PKCS10CertificationRequestBuilder p10Builder;
	ContentSigner signer;

	try{
		PublicKey publicKey = getKeyStore().getCertificate( certKeyAlias ).getPublicKey();
		p10Builder = new JcaPKCS10CertificationRequestBuilder(
				getX500Name()
				, publicKey )
				.setLeaveOffEmptyAttributes(true)

// http://www.bouncycastle.org/wiki/display/JA1/X.509+Public+Key+Certificate+and+Certification+Request+Generation#X.509PublicKeyCertificateandCertificationRequestGeneration-SubjectAlternativeName

		;

		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder( SHA256withECDSA );

		signer = csBuilder.build( pair.getPrivate() );
	}catch ( KeyStoreException | OperatorCreationException X ){
		pkException CRYPTOERR = new pkException( pkErrCode.CRYPTO ).set( "registrations err", X );
		mLog.error( CRYPTOERR.toString() );
		throw CRYPTOERR;
	}

	PKCS10CertificationRequest CSR = p10Builder.build( signer );
	return CSR;
}//genCSR
*/

/*
// http://stackoverflow.com/questions/9661008/compute-sha256-hash-in-android-java-and-c-sharp?lq=1
private void setHash() throws NoSuchAlgorithmException, UnsupportedEncodingException{
	MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
	this.hash = digest.digest( message.getBytes( charset ) );
}//setHash
*/
