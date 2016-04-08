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

import android.security.keystore.*;

import org.peacekeeper.exception.*;
import org.slf4j.*;
import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x500.*;
import org.spongycastle.asn1.x500.style.BCStrictStyle;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.*;
import org.spongycastle.cert.jcajce.*;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.operator.*;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.pkcs.*;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.spongycastle.util.io.pem.PemObject;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.Provider.Service;
import java.security.cert.*;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

// http://stackoverflow.com/questions/18244630/elliptic-curve-with-digital-signature-algorithm-ecdsa-implementation-on-bouncy
public class SecurityGuard {
//begin static
static private final Logger mLog = LoggerFactory.getLogger(org.peacekeeper.crypto.SecurityGuard.class);
static private java.security.KeyPair KEYPAIR = null;
static private java.security.KeyStore keyStore = null;
static private final String ECDSA = "ECDSA", SHA256withECDSA = "SHA256withECDSA",
		AndroidKeyStore = "AndroidKeyStore", NamedCurve = "P-256", charset = "UTF-8";
//end static

private String message = null;
private byte[] hash = null, signature = null;

public SecurityGuard(final String message) {
	this.message = message;
}

private void keyStoreContents() {
	try {
		java.util.Enumeration<String> aliases = keyStore.aliases();
		mLog.debug("keyStore contents:" + aliases.toString());
		while (aliases.hasMoreElements()) { mLog.debug(aliases.nextElement().toString()); }
	}
	catch( Exception X ){X.printStackTrace();}
}//keyStoreContents

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
			mLog.debug("keyStore containsAlias:\t" + alias);
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


		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SHA256withECDSA)
//		JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(ECDSA)
				                                    //.setProvider(BouncyCastleProvider.PROVIDER_NAME)
				;

		ContentSigner signer = csBuilder.build(kp.getPrivate());

		DefaultAlgorithmNameFinder daf = new DefaultAlgorithmNameFinder();
		String algo = daf.getAlgorithmName( signer.getAlgorithmIdentifier() );
		mLog.debug("genroot signer.getAlgorithmIdentifier(): \t" + algo);

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


		algo = daf.getAlgorithmName(certHolder.getSignatureAlgorithm());
		mLog.debug("genroot certHolder.getSignatureAlgorithm(): \t" + algo);


		//mLog.debug("certHolder algo:\t" + certHolder.getSignatureAlgorithm().getAlgorithm().toString());
		certificate = new JcaX509CertificateConverter()
				              //.setProvider(BouncyCastleProvider.PROVIDER_NAME)
				              .getCertificate(certHolder);
	}//try
	catch( OperatorCreationException| java.security.cert.CertificateException X ) {
		pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto selfSignedCert gen err", X);
		mLog.error(CRYPTOERR.toString());
		//certificate = null;
		throw CRYPTOERR;
	}




	mLog.debug( "genroot kp.getPublic().getAlgorithm(): \t" + kp.getPublic().getAlgorithm() );
		mLog.debug("certificate.getPublicKey().getAlgorithm():\t" + certificate.getPublicKey().getAlgorithm());

return certificate;
}//genRootCertificate()


private java.security.cert.X509Certificate genSelfSignedCert(java.security.KeyPair kp, String CN){
	java.security.cert.X509Certificate certificate;

	try{
		X500Name x500Name = new X500NameBuilder(BCStyle.INSTANCE)
				                    .addRDN(BCStyle.CN, CN)
				                    .build();

		java.security.SecureRandom rand = new java.security.SecureRandom();
		java.security.PrivateKey privKey = kp.getPrivate();
		java.security.PublicKey pubKey = kp.getPublic();

		SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(pubKey.getEncoded()));

		java.util.Date startDate = new java.util.Date(); // now

		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(startDate);
		c.add(java.util.Calendar.YEAR, 1);
		java.util.Date endDate = c.getTime();

		X509v3CertificateBuilder v3CertGen = new X509v3CertificateBuilder(
                                             x500Name,
                                             java.math.BigInteger.valueOf(rand.nextLong()),
                                             startDate, endDate,
                                             x500Name,
                                             subPubKeyInfo);


		ContentSigner signer = new JcaContentSignerBuilder(SHA256withECDSA).build(privKey);

		DefaultAlgorithmNameFinder daf = new DefaultAlgorithmNameFinder();
		String algo = daf.getAlgorithmName(signer.getAlgorithmIdentifier());
		mLog.debug("SelfSigned signer.getAlgorithmIdentifier(): \t" + algo);

		X509CertificateHolder certHolder = v3CertGen.build(signer);
		certificate = new JcaX509CertificateConverter().getCertificate(certHolder);

		//x500PrivateCredential x = new x500PrivateCredential(certificate, privKey, "");
	}//try
	catch( OperatorCreationException| java.security.cert.CertificateException X ) {
		pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto selfSignedCert gen err", X);
		mLog.error(CRYPTOERR.toString());
		//certificate = null;
		throw CRYPTOERR;
	}

	mLog.debug("SelfSigned kp.getPrivate().getAlgorithm(): \t" + kp.getPrivate().getAlgorithm());
	mLog.debug( "SelfSigned kp.getPublic().getAlgorithm(): \t" + kp.getPublic().getAlgorithm() );
	mLog.debug( "SelfSigned certificate.getPublicKey().getAlgorithm():\t" + certificate.getPublicKey().getAlgorithm());

return certificate;
}//genSelfSignedCert



//http://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
/*
private KeyPair genKeyPair(final String alias) {
*/
/*
	final int  keyPurpose = KeyProperties.PURPOSE_SIGN
			                        |KeyProperties.PURPOSE_DECRYPT
			                        |KeyProperties.PURPOSE_ENCRYPT
			                        |KeyProperties.PURPOSE_VERIFY
*//*

	final int  keyPurpose = 1;
			   //, FiveMin = 5 * 60
					;

	final Calendar start = Calendar.getInstance();
	final Date now = start.getTime();
	start.add(Calendar.YEAR, 1);
	final Date expire = start.getTime();
	KeyPair keyPair;

	mLog.debug("genKeyPair()");

*/
/*
	KeyStore keyStore = null;
	mLog.error("CREATING STORE");
	try {
		keyStore = KeyStore.getInstance("AndroidKeyStore");
		keyStore.load(null);
		mLog.error("STORE CREATED!!!!");
	}
	catch(Exception X) {X.printStackTrace();}
*//*




	//ECParameterSpec ecSpec = (ECParameterSpec)ECNamedCurveTable.getParameterSpec(NamedCurve);
	//ECParameterSpec ecSpec = new ECGenParameterSpec("secp256r1")


	try {

		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ECDSA, BouncyCastleProvider.PROVIDER_NAME);
		//KeyPairGenerator kpg = KeyPairGenerator.getInstance(SHA256withECDSA, BouncyCastleProvider.PROVIDER_NAME);

*/
/*
		AlgorithmParameterSpec alSpec = new KeyGenParameterSpec.Builder(alias, keyPurpose )
											//.setAlgorithmParameterSpec( new ECGenParameterSpec(NamedCurve) )
				                                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))


				                                .setAlgorithmParameterSpec(ecSpec)
													.setDigests(KeyProperties.DIGEST_SHA256)
													.setUserAuthenticationRequired(true)
													// Only permit the private key to be used if the user authenticated within the last five minutes.
													//	              .setUserAuthenticationValidityDurationSeconds(FiveMin)
													.setKeyValidityStart(now)
													.setKeyValidityEnd(expire)
												.build();
		kpg.initialize( alSpec);
*//*


		ECGenParameterSpec     ecGenSpec = new ECGenParameterSpec(NamedCurve);
		kpg.initialize(ecGenSpec, new SecureRandom());

		keyPair = kpg.generateKeyPair();

	} catch (NoSuchAlgorithmException| InvalidAlgorithmParameterException| NoSuchProviderException X) {

//f		X.printStackTrace();
		//Crypto KeyPair gen err=[java.security.InvalidAlgorithmParameterException: parameter object not a ECParameterSpec
		pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto KeyPair gen err", X);
		mLog.error(CRYPTOERR.toString());
		keyPair = null;

		throw CRYPTOERR;
	}

		mLog.debug("public:\t" + keyPair.getPublic().getAlgorithm() );
		mLog.debug("private:\t" + keyPair.getPrivate().getAlgorithm() );

//	mLog.error("KEYPAIR GENERATED!?!?!");
	//keyStoreContents();

// Now we import the Spongeycastle provided keypair into the AndroidKeyStore provided KeyStore.
// see http://developer.android.com/reference/android/security/keystore/KeyProtection.html

	try {//selfSignedCert is only for initial persistence of the KeyPair. We replace it with
		// true good CA Cert later.
		X509Certificate[] selfSignedCert = new X509Certificate[1];

//		selfSignedCert[0] = genSelfSignedCert(keyPair, alias);
		selfSignedCert[0] = genRootCertificate(keyPair, alias);

*/
/*
		mLog.debug("selfSignedCert public:\t" + selfSignedCert[0].getPublicKey().getAlgorithm() );
		mLog.debug("private:\t" + keyPair.getPrivate().getAlgorithm() );
*//*



		//Android Java test
		if (!(selfSignedCert[0].getPublicKey().getAlgorithm()).equals(keyPair.getPrivate().getAlgorithm())) {
		mLog.debug("failed Android test");
*/
/*
			throw new IllegalArgumentException("Algorithm of private key does not match "
					                                   + "algorithm of public key in end certificate of entry "
					                                   + "(with index number: 0)");
*//*

		}

//jdk8 test
		if (!keyPair.getPrivate().getAlgorithm().equals
				                               (selfSignedCert[0].getPublicKey().getAlgorithm())) {
			mLog.debug("failed jdk test");

*/
/*
			throw new IllegalArgumentException
					      ("private key algorithm does not match " +
							       "algorithm of public key in end entity " +
							       "certificate (at index 0)");
*//*

		}




		KeyStore.Entry privateKey = new PrivateKeyEntry(keyPair.getPrivate(), selfSignedCert );

//		KeyStore.ProtectionParameter param = new KeyProtection.Builder( KeyProperties.PURPOSE_SIGN)
		KeyStore.ProtectionParameter param = new KeyProtection.Builder( keyPurpose )
				                                     .setDigests(KeyProperties.DIGEST_SHA256)
				                                     .build();

		keyStore.setEntry(alias, privateKey, param);
		mLog.debug("key saved:\t");
		keyStoreContents();
	} catch (KeyStoreException X) {
//	} catch (KeyStoreException| IOException| NoSuchAlgorithmException| CertificateException X) {
		pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto KeyStore gen err", X);
		mLog.error(CRYPTOERR.toString());
		keyPair = null;
		throw CRYPTOERR;
	}

return keyPair;
}//genKeyPair
*/


private java.security.KeyPair genKeyPair(final String alias) {
	java.security.KeyPair keyPair = null;
	try {
//		KeyPairGenerator kpg = KeyPairGenerator.getInstance( KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
		java.security.KeyPairGenerator kpg = java.security.KeyPairGenerator.getInstance( ECDSA, "AndroidKeyStore");

		kpg.initialize(
				              new KeyGenParameterSpec.Builder(
						                                             alias,
						                                             KeyProperties.PURPOSE_SIGN)
						              .setAlgorithmParameterSpec(new java.security.spec.ECGenParameterSpec("secp256r1"))
						              .setDigests(KeyProperties.DIGEST_SHA256
								                         //,KeyProperties.DIGEST_SHA384
								                         //, KeyProperties.DIGEST_SHA512
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
}


private static final String testAlias = "JD test Alias";
public byte[] getSignature(){
    if (this.signature == null)
    try {
        java.security.KeyPair keyPair = getKeyPair(testAlias);
//	    Signature ecdsaSign = Signature.getInstance(SHA256withECDSA, BouncyCastleProvider.PROVIDER_NAME);
	    java.security.Signature ecdsaSign = java.security.Signature.getInstance(SHA256withECDSA);

	    byte[] test = keyPair.getPrivate().getEncoded();
	    mLog.debug("keyPair.getPrivate().getEncoded():\t" + (test==null ? "null" : "NOT null ") );

	    mLog.debug("keyPair.getPrivate().getEncoded():\t" + test);
	    //org.spongycastle.jce.interfaces.ECPrivateKey x = (org.spongycastle.jce.interfaces.ECPrivateKey) keyPair.getPrivate();

	    java.security.PrivateKey privateKey = (java.security.PrivateKey) keyStore.getKey(testAlias, null);
	    //ecdsaSign.initSign(privateKey);
	    ecdsaSign.initSign(keyPair.getPrivate());
        ecdsaSign.update(this.message.getBytes(charset));
        signature = ecdsaSign.sign();
} catch (java.security.UnrecoverableKeyException| java.security.KeyStoreException| java.security.NoSuchAlgorithmException| java.security.InvalidKeyException| java.security.SignatureException| java.io.UnsupportedEncodingException X)
    {   X.printStackTrace();
        pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("Crypto Signature err", X);
        mLog.error(CRYPTOERR.toString());
        signature = null;
        throw CRYPTOERR;
    }//catch

	return signature;
}//getSignature

public boolean verify(){

    boolean retVal;
    try {
//	    Signature ecdsaVerify = Signature.getInstance(SHA256withECDSA, BouncyCastleProvider.PROVIDER_NAME);
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

    PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
            subject
            , pair.getPublic() )
            .setLeaveOffEmptyAttributes(true);

    JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SHA256withECDSA);
    ContentSigner signer;// = null;

    try { signer = csBuilder.build(pair.getPrivate()); }
    catch (OperatorCreationException X) {
        pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("CSR err", X);
        mLog.error(CRYPTOERR.toString());
        throw CRYPTOERR;
    }

return p10Builder.build(signer);
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

public boolean unRegister(){
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

}//class SecurityGuard
