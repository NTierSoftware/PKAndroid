//http://developer.android.com/training/articles/keystore.html
//http://developer.android.com/reference/android/security/keystore/KeyProtection.html
//http://www.bouncycastle.org/wiki/display/JA1/X.509+Public+Key+Certificate+and+Certification+Request+Generation
//http://www.bouncycastle.org/wiki/display/JA1/X.509+Public+Key+Certificate+and+Certification+Request+Generation#X.509PublicKeyCertificateandCertificationRequestGeneration-Version3CertificateCreation
//http://stackoverflow.com/questions/29852290/self-signed-x509-certificate-with-bouncy-castle-in-java


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
*/

//import android.security.keystore.*;

import org.peacekeeper.exception.*;
import org.slf4j.*;

import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x500.*;
import org.spongycastle.asn1.x500.style.*;
import org.spongycastle.cert.*;
import org.spongycastle.cert.jcajce.*;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.operator.*;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.pkcs.*;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.spongycastle.util.io.pem.PemObject;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.Provider.Service;
import java.security.cert.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

// http://stackoverflow.com/questions/18244630/elliptic-curve-with-digital-signature-algorithm-ecdsa-implementation-on-bouncy
public final class SecurityGuard {
//begin static
static private final Logger mLog = LoggerFactory.getLogger(org.peacekeeper.crypto.SecurityGuard.class);
static private java.security.KeyPair KEYPAIR = null;
static private java.security.KeyStore keyStore = null;
static private final String ECDSA = "ECDSA", SHA256withECDSA = "SHA256withECDSA"
		, AndroidKeyStore = "AndroidKeyStore", charset = "UTF-8"
		, NamedCurve = "secp256r1"; //"P-256"

static private final java.security.Provider PROVIDER = new org.spongycastle.jce.provider.BouncyCastleProvider();
//end static

private String message = null;
private byte[] hash = null, signature = null;

static void initSecurity(){ initSecurity(PROVIDER); }

//Moves provider to first place
static void initSecurity(java.security.Provider provider){
	listProviders();
	java.security.Security.removeProvider(provider.getName());

	int insertProviderAt = java.security.Security.insertProviderAt(provider, 1);
	mLog.debug("insertProviderAt:\t" + Integer.toString(insertProviderAt) ) ;
	listProviders();
}//initSecurity

public SecurityGuard(final String message) {
	initSecurity();
	this.message = message;
}

private java.security.KeyPair getKeyPair(final String alias){

	mLog.debug("KEYPAIR " + (KEYPAIR == null ? "" : "NOT ") + "null");

	if (KEYPAIR != null){return KEYPAIR; }


	mLog.debug("keyStore " + (keyStore == null ? "" : "NOT ") + "null");

	if (keyStore == null){
		mLog.debug("keyStore is null");
		try {
			keyStore = java.security.KeyStore.getInstance(AndroidKeyStore);
			keyStore.load(null);
			mLog.debug("keyStore init'd");
			keyStoreContents();
		}//try
		catch (java.security.KeyStoreException| java.io.IOException| java.security.NoSuchAlgorithmException| java.security.cert.CertificateException X) {
			pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto keyStore load err", X);
			mLog.error(CRYPTOERR.toString());
			KEYPAIR = null;
			throw CRYPTOERR;
		}
	}

	try{mLog.debug("keyStore does " + (keyStore.containsAlias(alias) ? "" : "NOT ") + "contain: "+ alias);}
	catch (Exception X){;}

	try {
		if ( keyStore.containsAlias(alias)) {
			java.security.PrivateKey PrivateKey = (java.security.PrivateKey) keyStore.getKey(alias, null); //no password
			java.security.PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
			KEYPAIR = new java.security.KeyPair(publicKey, PrivateKey );
		}//if
		else KEYPAIR = genKeyPair( alias );
	}//try
	catch (java.security.KeyStoreException| java.security.NoSuchAlgorithmException| java.security.UnrecoverableEntryException X) {
		pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto getKeyPair err", X);
		mLog.error(CRYPTOERR.toString());
		KEYPAIR = null;
		throw CRYPTOERR;
	}

	return KEYPAIR;
}//getKeyPair



// http://www.programcreek.com/java-api-examples/index.php?class=org.spongycastle.cert.X509v3CertificateBuilder&method=addExtension
private static java.security.cert.X509Certificate genRootCertificate( java.security.KeyPair kp, String CN){
	java.security.cert.X509Certificate certificate = null;
	try {
		X500Name x500Name = new X500NameBuilder(BCStyle.INSTANCE)
				                          .addRDN(BCStyle.CN, CN)
							.build();

		final java.util.Calendar start = java.util.Calendar.getInstance();
		final java.util.Date now = start.getTime();
		//expires in one day - just enough time to be replaced by CA CERT
		//TODO consider genRootCertificate(KeyPair kp, String CN, Date expire )
		start.add(java.util.Calendar.DATE, 1);
		final java.util.Date expire = start.getTime();


		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SHA256withECDSA);

		ContentSigner signer = csBuilder.build(kp.getPrivate());

/*
		DefaultAlgorithmNameFinder daf = new DefaultAlgorithmNameFinder();
		String algo = daf.getAlgorithmName( signer.getAlgorithmIdentifier() );
		mLog.debug("genroot signer.getAlgorithmIdentifier(): \t" + algo);
*/

		java.math.BigInteger serialnum = new java.math.BigInteger(80, new java.security.SecureRandom());//new Random()),

		X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                                      x500Name, //builder.build(),
                                      serialnum,
                                      now, //new Date(System.currentTimeMillis() - 50000),
                                      expire,
                                      x500Name, //builder.build(),
                                      kp.getPublic()
									);
		X509CertificateHolder certHolder = certGen.build(signer);


/*
		algo = daf.getAlgorithmName(certHolder.getSignatureAlgorithm());
		mLog.debug("genroot certHolder.getSignatureAlgorithm(): \t" + algo);
*/


		certificate = new JcaX509CertificateConverter()
				              //.setProvider(BouncyCastleProvider.PROVIDER_NAME)
				              .getCertificate(certHolder);
	}//try
	catch( OperatorCreationException| java.security.cert.CertificateException X ) {
		pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto selfSignedCert gen err", X);
		mLog.error(CRYPTOERR.toString());
		throw CRYPTOERR;
	}




	mLog.debug( "genroot kp.getPublic().getAlgorithm(): \t" + kp.getPublic().getAlgorithm() );
		mLog.debug("certificate.getPublicKey().getAlgorithm():\t" + certificate.getPublicKey().getAlgorithm());

return certificate;
}//genRootCertificate()


//http://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
private java.security.KeyPair genKeyPair(final String alias) {
	java.security.KeyPair keyPair = null;
	try {
		java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance( KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

		kpg.initialize(
				              new KeyGenParameterSpec.Builder(
						                                             alias,
						                                             KeyProperties.PURPOSE_SIGN)
						              .setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec(NamedCurve))
						              .setDigests(KeyProperties.DIGEST_SHA256
						              )
// Only permit the private key to be used if the user authenticated
// within the last five minutes.
								               //.setUserAuthenticationRequired(true)
								               //.setUserAuthenticationValidityDurationSeconds(5 * 60)

						              .build(),
				              new java.security.SecureRandom());
		keyPair = kpg.generateKeyPair();
		java.security.Signature signature = java.security.Signature.getInstance(SHA256withECDSA);
		signature.initSign(keyPair.getPrivate());

	}
	catch(Exception X){X.printStackTrace();}
return keyPair;
}//genKeyPair


private static final String testAlias = "JD test Alias";
public byte[] getSignature(){
    if (this.signature == null)
    try {
        java.security.KeyPair keyPair = getKeyPair(testAlias);
//	    Signature ecdsaSign = Signature.getInstance(SHA256withECDSA, BouncyCastleProvider.PROVIDER_NAME);
		mLog.debug("\ngetSignature()\n");
	    initSecurity(java.security.Security.getProvider("AndroidKeyStoreBCWorkaround"));
	    mLog.debug("\ngetSignature()\n");
	    //Provider AndroidKeyStoreBCWorkaround = Security.getProvider("AndroidKeyStoreBCWorkaround");
	    //Security.removeProvider("AndroidKeyStoreBCWorkaround");
	    //listProviders();
	   // mLog.debug("\n\nSecurity.insertProviderAt: " +Security.insertProviderAt(AndroidKeyStoreBCWorkaround, 1));
		//listProviders();
	    java.security.Signature ecdsaSign = java.security.Signature.getInstance(SHA256withECDSA);

	    byte[] test = keyPair.getPrivate().getEncoded();
//	    mLog.debug("keyPair.getPrivate().getEncoded():\t" + (test==null ? "null" : "NOT null ") );


	    //PrivateKey privateKey = (PrivateKey) keyStore.getKey(testAlias, null);

	    ecdsaSign.initSign(keyPair.getPrivate());
        ecdsaSign.update(this.message.getBytes(charset));
        signature = ecdsaSign.sign();
} catch (java.security.NoSuchAlgorithmException| java.security.InvalidKeyException| java.security.SignatureException| java.io.UnsupportedEncodingException X)
    {   X.printStackTrace();
        pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto Signature err", X);
        mLog.error(CRYPTOERR.toString());
        signature = null;
        throw CRYPTOERR;
    }//catch

	initSecurity();
	return signature;
}//getSignature

public boolean verify(){
    boolean retVal;
    try {
	    java.security.Signature ecdsaVerify = java.security.Signature.getInstance(SHA256withECDSA);
        ecdsaVerify.initVerify(getKeyPair(testAlias).getPublic());
	    ecdsaVerify.update(this.message.getBytes(charset));
        retVal = ecdsaVerify.verify( getSignature() );

    } catch (java.security.NoSuchAlgorithmException| java.security.InvalidKeyException| java.security.SignatureException| java.io.UnsupportedEncodingException X)
    {   retVal = false;
        pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("crypto verify err", X);
        mLog.error(CRYPTOERR.toString()); }

return retVal;
}//verify

// http://stackoverflow.com/questions/9661008/compute-sha256-hash-in-android-java-and-c-sharp?lq=1
public void setHash() throws java.security.NoSuchAlgorithmException, java.io.UnsupportedEncodingException
{   java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
    this.hash = digest.digest(message.getBytes(charset));
}//setHash


//http://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash/23273249#23273249
@Override
public String toString() {
    if (this.hash == null) return null;

    String hashStr = new java.math.BigInteger(1, this.hash).toString(16);

// Now we need to zero pad it if you actually want the full 32 chars.
    while (hashStr.length() < 32) { hashStr = "0" + hashStr; }


    StringBuilder retVal = new StringBuilder("SecurityGuard:\t")
                           .append(this.message)
                           .append("\tHash: ").append(hashStr);

return retVal.toString();
}

//https://msdn.microsoft.com/en-us/library/windows/desktop/aa376502(v=vs.85).aspx
// http://stackoverflow.com/questions/20532912/generating-the-csr-using-bouncycastle-api
public PKCS10CertificationRequest generateCSR(){
    java.security.KeyPair pair = this.getKeyPair(testAlias);

    X500Name subject = new X500NameBuilder( new BCStrictStyle() )
            .addRDN(BCStrictStyle.EmailAddress, "JD.John.Donaldson@gmail.com")
            .build();

	mLog.debug("\ngenerateCSR()\n");
	initSecurity(java.security.Security.getProvider("AndroidKeyStoreBCWorkaround"));
	mLog.debug("\ngenerateCSR()\n");

    PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
            subject
            , pair.getPublic() )
            .setLeaveOffEmptyAttributes(true);

    JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SHA256withECDSA);
    ContentSigner signer;

    try { signer = csBuilder.build(pair.getPrivate()); }
    catch (OperatorCreationException X) {
        pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("CSR err", X);
        mLog.error(CRYPTOERR.toString());
        throw CRYPTOERR;
    }
	PKCS10CertificationRequest CSR = p10Builder.build(signer);
	initSecurity();
return CSR;
}//generateCSR


//Get the CSR as a PEM formatted String
public String toPEM(PKCS10CertificationRequest CSR){
    java.io.StringWriter str = new java.io.StringWriter();
    JcaPEMWriter pemWriter = new JcaPEMWriter(str);
    String retVal = "";
    try
/*
	    (
		        StringWriter str = new StringWriter();
		        JcaPEMWriter pemWriter = new JcaPEMWriter(str);
        )
*/
    {
        PemObject pemObject = new PemObject("CERTIFICATE REQUEST", CSR.getEncoded());
        pemWriter.writeObject(pemObject);
        pemWriter.close();
        str.close();
        retVal = str.toString();
    } catch (java.io.IOException X) {
        retVal = "";
        pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("toPEM err", X);
        mLog.error(CRYPTOERR.toString());
        throw CRYPTOERR; }
return retVal;
}//toPEM

static public boolean unRegister(){
	boolean retval = false;
return retval;
}//unRegister()

//see https://github.com/nelenkov/ecdh-kx/blob/master/src/org/nick/ecdhkx/Crypto.java
static public void listAlgorithms(String algFilter) {
	java.security.Provider[] providers = java.security.Security.getProviders();
	for (java.security.Provider p : providers) {
		String providerStr = String.format("%s/%s/%f\n", p.getName(),
				                                  p.getInfo(), p.getVersion());
		mLog.debug(providerStr);
		java.util.Set<java.security.Provider.Service> services = p.getServices();
		java.util.List<String> algs = new java.util.ArrayList<String>();
		for (java.security.Provider.Service s : services) {
			boolean match = true;
			if (algFilter != null) {
				match = s.getAlgorithm().toLowerCase()
						        .contains(algFilter.toLowerCase());
			}


			if (match) {
				String algStr = String.format("\t%s/%s/%s", s.getType(),
						                             s.getAlgorithm(), s.getClassName());
				algs.add(algStr);
			}
		}


		java.util.Collections.sort(algs);
		for (String alg : algs) {
			mLog.debug("\t" + alg);
		}
		mLog.debug("");
	}
}


static public void listCurves() {
	mLog.debug("Supported named curves:");
	java.util.Enumeration<?> names = SECNamedCurves.getNames();
	while (names.hasMoreElements()) {
		mLog.debug( "\t" + (String) names.nextElement());
	}
}


private void keyStoreContents() {
	try {
		java.util.Enumeration<String> aliases = keyStore.aliases();
		mLog.debug((aliases.hasMoreElements() ? "" : "Empty") + "keyStore contents" );
		while (aliases.hasMoreElements() ) { mLog.debug(":\t" + aliases.nextElement().toString() ); }
	}
	catch( Exception X ){X.printStackTrace();}
}//keyStoreContents

static public void listProviders(){
	java.security.Provider[] providers = java.security.Security.getProviders();
	StringBuilder list = new StringBuilder().append("Num providers: " + providers.length );
	int i = 0;
	for (java.security.Provider p : providers){
		list.append("\n\tProvider " + ++i + ": " + p.getName() + "\t info: " + p.getInfo());
		java.util.Set<java.security.Provider.Service> services = p.getServices();
		list.append("\tNum services: " + services.size());
		for (java.security.Provider.Service s : services ){
			//list.append("\n\t\tService: " + s.toString() + "\ttype: " + s.getType() + "\talgo: " + s.getAlgorithm());
		}
	}

	mLog.debug(list.toString());
}//listProviders

}//class SecurityGuard

