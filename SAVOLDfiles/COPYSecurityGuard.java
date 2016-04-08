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

/*
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
*/

// http://stackoverflow.com/questions/18244630/elliptic-curve-with-digital-signature-algorithm-ecdsa-implementation-on-bouncy
public class COPYSecurityGuard {
//begin static
static private final org.slf4j.Logger				mLog = org.slf4j.LoggerFactory.getLogger( COPYSecurityGuard.class );
static private java.security.KeyPair keyPair = null;
static private final String SHA256withECDSA = "SHA256withECDSA",
                            charset = "UTF-8";
//end static

private String message = null;
private byte[] hash = null, signature = null;

public COPYSecurityGuard(final String message){ this.message = message; }

/*
public KeyPair GenerateKeys(){
	if (keyPair == null) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", SpongeyCastle );

	        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");

            kpg.initialize(ecSpec, new SecureRandom());

            keyPair = kpg.generateKeyPair(); }
        catch (NoSuchAlgorithmException| NoSuchProviderException| InvalidAlgorithmParameterException X)
        {   keyPair = null;
            pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("GenerateKeys err", X);;
            mLog.error(CRYPTOERR.toString());
            throw CRYPTOERR; }
    }//if

return keyPair;
}//GenerateKeys


public KeyPair GenerateKeys(){
	if (keyPair == null) {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME );

			ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");

			kpg.initialize(ecSpec, new SecureRandom());

			keyPair = kpg.generateKeyPair(); }
		catch (NoSuchAlgorithmException| NoSuchProviderException| InvalidAlgorithmParameterException X)
		{   keyPair = null;
			pkException CRYPTOERR = new pkException(pkErrCode.CRYPTO).set("GenerateKeys err", X);;
			mLog.error(CRYPTOERR.toString());
			throw CRYPTOERR; }
	}//if

	return keyPair;
}//GenerateKeys
*/

//http://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.html
public java.security.KeyPair genKeyPair(String keystoreAlias) {

	java.security.KeyPairGenerator kpg = null;
	try {
		kpg = java.security.KeyPairGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");

		kpg.initialize(
               new android.security.keystore.KeyGenParameterSpec.Builder(
		                                              keystoreAlias,
		                                              android.security.keystore.KeyProperties.PURPOSE_SIGN)
                       .setAlgorithmParameterSpec(org.spongycastle.jce.ECNamedCurveTable.getParameterSpec("P-256"))
                       .setDigests(android.security.keystore.KeyProperties.DIGEST_SHA256)
                       .setUserAuthenticationRequired(true)
		                        // Only permit the private key to be used if the user authenticated
		                        // within the last five minutes.
                       .setUserAuthenticationValidityDurationSeconds(5 * 60)
		               .build());
	java.security.KeyPair keyPair = kpg.generateKeyPair();
	//Signature signature =
	//Signature.getInstance(SHA256withECDSA).initSign(keyPair.getPrivate());
	//signature.initSign(keyPair.getPrivate());
	} catch (java.security.InvalidKeyException| java.security.NoSuchAlgorithmException| java.security.InvalidAlgorithmParameterException| java.security.NoSuchProviderException X) {
		X.printStackTrace();
	}


}//genKeyPair



private static final char[] keyStorePW = "PeaceKeeperKeyStorePW".toCharArray();
//https://github.com/boeboe/be.boeboe.spongycastle/commit/5942e4794c6f950a95409f2612fad7de7cc49b33
private void storeKey(final String alias, java.security.KeyPair keyPair){


	try {
		java.security.KeyStore keyStore = java.security.KeyStore.getInstance("BKS");
		keyStore.load(null);
		keyStore.setEntry(alias, keyPair, null);

		keyStore.store(new java.io.FileOutputStream("keystore.jks"), keyStorePW);
	}

	catch (java.io.FileNotFoundException X){
		org.peacekeeper.exception.pkException CRYPTOERR = new org.peacekeeper.exception.pkException(org.peacekeeper.exception.pkErrCode.CRYPTO).set("storeKey err", X);;
		mLog.error(CRYPTOERR.toString());
		throw CRYPTOERR; }
	catch ( java.security.cert.CertificateException| java.security.NoSuchAlgorithmException| java.security.KeyStoreException| java.io.IOException  X){
		org.peacekeeper.exception.pkException CRYPTOERR = new org.peacekeeper.exception.pkException(org.peacekeeper.exception.pkErrCode.CRYPTO).set("storeKey err", X);;
		mLog.error(CRYPTOERR.toString());
	throw CRYPTOERR; }

}//storeKey

/*
public KeyPair createNewKeys(android.view.View view) {
//	String alias = aliasText.getText().toString();
	KeyPair keyPair = null;
	try {
		// Create new key if needed
		//if (!keyStore.containsAlias(alias)) {
			java.util.Calendar start = java.util.Calendar.getInstance()
					, end = java.util.Calendar.getInstance()
							  ;
		end.add(java.util.Calendar.YEAR, 1);

			android.security.KeyPairGeneratorSpec spec = new android.security.KeyPairGeneratorSpec.Builder(this)
					                            .setAlias(alias)
					                            .setSubject(new javax.security.auth.x500.X500Principal("CN=PeaceKeeper, O=Android Authority"))
					                            .setSerialNumber(BigInteger.ONE)
					                            .setStartDate(start.getTime())
					                            .setEndDate(end.getTime())
					                            .build();
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
			generator.initialize(spec);

			keyPair = generator.generateKeyPair();
		//}
	} catch (Exception X) {
		android.widget.Toast.makeText(this, "Exception " + X.getMessage() + " occurred", android.widget.Toast.LENGTH_LONG).show();
		mLog.error(X.getLocalizedMessage());
	}
//	refreshKeys();
	return keyPair;
}
*/


public byte[] getSignature(){
    if (signature == null)
    try {
        java.security.KeyPair pair = this.GenerateKeys();
	    java.security.Signature ecdsaSign = java.security.Signature.getInstance(SHA256withECDSA);
	    Signature ecdsaSign = Signature.getInstance(SHA256withECDSA, org.spongycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME);
        ecdsaSign.initSign(pair.getPrivate());
        ecdsaSign.update(this.message.getBytes(charset));
        signature = ecdsaSign.sign();
    } catch (java.security.NoSuchAlgorithmException| java.security.NoSuchProviderException|
             java.security.InvalidKeyException| java.io.UnsupportedEncodingException| java.security.SignatureException X)
    {
        org.peacekeeper.exception.pkException CRYPTOERR = new org.peacekeeper.exception.pkException(org.peacekeeper.exception.pkErrCode.CRYPTO).set("Crypto Signature err", X);
        mLog.error(CRYPTOERR.toString());
        signature = null;
        throw CRYPTOERR;
    }//catch

return signature;
}//getSignature

public boolean verify(){
    java.security.Signature ecdsaVerify = null;
    boolean retVal = false;
    try {
        ecdsaVerify = java.security.Signature.getInstance(SHA256withECDSA, org.spongycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME);
        ecdsaVerify.initVerify(GenerateKeys().getPublic());
        ecdsaVerify.update(this.message.getBytes(charset));
        retVal = ecdsaVerify.verify( getSignature() );
    } catch (java.security.NoSuchAlgorithmException| java.security.NoSuchProviderException| java.security.SignatureException| java.io.UnsupportedEncodingException X)
    {   retVal = false;
        org.peacekeeper.exception.pkException CRYPTOERR = new org.peacekeeper.exception.pkException(org.peacekeeper.exception.pkErrCode.CRYPTO).set("crypto verify err", X);
        mLog.error(CRYPTOERR.toString()); }

    finally { return retVal; }
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


    StringBuilder retVal = new StringBuilder("SecurityGuard:\t").append(this.message)
                            .append("\tHash: ").append(hashStr);

return retVal.toString();
}

// http://stackoverflow.com/questions/20532912/generating-the-csr-using-bouncycastle-api
public org.spongycastle.pkcs.PKCS10CertificationRequest generateCSR(){
    java.security.KeyPair pair = this.GenerateKeys();

    //TODO fix X500 EmailAddress
    org.spongycastle.asn1.x500.X500Name subject = new org.spongycastle.asn1.x500.X500NameBuilder( new org.spongycastle.asn1.x500.style.BCStrictStyle() )
            .addRDN(org.spongycastle.asn1.x500.style.BCStrictStyle.EmailAddress, "JD.John.Donaldson@gmail.com")
            .build();

    org.spongycastle.pkcs.PKCS10CertificationRequestBuilder p10Builder = new org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder(
            subject
            , pair.getPublic() )
            .setLeaveOffEmptyAttributes(true);

    org.spongycastle.operator.jcajce.JcaContentSignerBuilder csBuilder = new org.spongycastle.operator.jcajce.JcaContentSignerBuilder(SHA256withECDSA);
    org.spongycastle.operator.ContentSigner signer = null;

    try { signer = csBuilder.build(pair.getPrivate()); }
    catch (org.spongycastle.operator.OperatorCreationException X) {
        org.peacekeeper.exception.pkException CRYPTOERR = new org.peacekeeper.exception.pkException(org.peacekeeper.exception.pkErrCode.CRYPTO).set("CSR err", X);
        mLog.error(CRYPTOERR.toString());
        throw CRYPTOERR;
    }

return p10Builder.build(signer);
}//generateCSR


//Get the CSR as a PEM formatted String
public String toPEM(org.spongycastle.pkcs.PKCS10CertificationRequest CSR){
    java.io.StringWriter str = new java.io.StringWriter();
    org.spongycastle.openssl.jcajce.JcaPEMWriter pemWriter = new org.spongycastle.openssl.jcajce.JcaPEMWriter(str);
    String retVal = "";
    try {
        org.spongycastle.util.io.pem.PemObject pemObject = new org.spongycastle.util.io.pem.PemObject("CERTIFICATE REQUEST", CSR.getEncoded());
        pemWriter.writeObject(pemObject);
        pemWriter.close();
        str.close();
        retVal = str.toString();
    } catch (java.io.IOException X) {
        retVal = "";
        org.peacekeeper.exception.pkException CRYPTOERR = new org.peacekeeper.exception.pkException(org.peacekeeper.exception.pkErrCode.CRYPTO).set("toPEM err", X);
        mLog.error(CRYPTOERR.toString());
        throw CRYPTOERR; }
    finally { return retVal; }
}//toPEM

}//class SecurityGuard
