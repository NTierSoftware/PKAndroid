package udinic.accounts_authenticator_example.authentication;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.Response;

import org.json.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.peacekeeper.rest.LinkedRequest;
import org.peacekeeper.rest.LinkedRequest.pkURL;
import org.peacekeeper.util.pkUtility;
import org.slf4j.*;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;


import static udinic.accounts_authenticator_example.authentication.AuthenticatorActivity.KEY_ERROR_MESSAGE;
import static udinic.accounts_authenticator_example.authentication.AuthenticatorActivity.PARAM_USER_PASS;


public class pkServerAuthenticate implements ServerAuthenticate{
private static final Logger mLog = LoggerFactory.getLogger( pkServerAuthenticate.class );
//public static final String authtoken;// = "pkServerAuthenticate";
private static String authtoken;// = "pkServerAuthenticate";
private pkUtility mUtility = pkUtility.getInstance();

@Override public String userSignUp( final String name, final String email, final String zipCode,
                          final String authType ) throws Exception{
return userSignUp( null, name, email, zipCode, authType );
}

//public String userSignUp( final String name, final String email, final String pass, final String authType )throws Exception{
public String userSignUp( final Activity aActivity, final String accountName, final String email, final String zipCode,
                          final String authType ) throws Exception{

	mLog.trace( "userSignUp():\t" );
	LinkedRequest ChainReg3 = new LinkedRequest( pkURL.devices, null ){
		@Override public JSONObject getRequest( final JSONObject response ){
			try{
				this.mPkURL.URLstr = new StringBuilder( "devices/" )
						.append( response.getString( "deviceId" ) )
						.append( "?where=keeperId==\"" )
						.append( response.getString( "keeperId" ) + "\"" )
						.toString();
			}catch ( JSONException X ){ X.printStackTrace(); }

			authtoken = SecurityGuard.getAuthToken( response );
//			authtoken = SecurityGuard.SETAuthToken( response ); SET AUTH TOKEN HERE
			this.mPkURL.mHeader.put( "Authorization", authtoken );

		return null;
		}//getRequest
	};

	Response.Listener< JSONObject > aJsonResp =  new Response.Listener< JSONObject >(){
		@Override public void onResponse( JSONObject response ){
			mLog.error( "\n\n\t\t!!!!onResponse from ChainReg3!!!!!!\n\n" );
			String respStr = "response:\t" + ((response == null)? "NULL" : response.toString() );

			if (aActivity != null){
				Bundle data = new Bundle();
				try{
					data.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
					data.putString( AccountManager.KEY_AUTHTOKEN, authtoken );
					data.putString( AccountManager.KEY_USERDATA, email );
					data.putString( AccountManager.KEY_ACCOUNT_TYPE, authType );
//				data.putString( PARAM_USER_PASS, accountPassword ); //TODO pw is not zip FIX
					data.putString( PARAM_USER_PASS, zipCode );//TODO pw is not zip FIX
				}catch ( Exception e ){ data.putString( KEY_ERROR_MESSAGE, e.getMessage() ); }

				Intent retVal = new Intent().putExtras( data );

				aActivity.setResult( Activity.RESULT_OK, retVal );
				aActivity.finish();

			}//if
		}
	};

	ChainReg3.setResponseListener( aJsonResp );

	LinkedRequest ChainReg2 = new LinkedRequest( pkURL.registrations2, ChainReg3 ){
		@Override public JSONObject getRequest( final JSONObject response ){
			return getReceivedCode();
		}
	};


	LinkedRequest ChainReg1 = new LinkedRequest( pkURL.registrations
												, getRegistration(),
	                                             ChainReg2 ){
		@Override public JSONObject getRequest( final JSONObject response ){ return null; }
	};

	ChainReg1.submit();

return authtoken;
}//userSignUp



@Override
public String userSignIn( final String user, final String pass, final String authType )
throws Exception{
//public String userSignUp( final String name, final String email, final String pass, final String authType )throws Exception{
	return authtoken;
}//userSignIn()


private JSONObject getRegistration(){
	PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
	mLog.debug( SecurityGuard.toPEM( CSR ) );

	JSONObject registration = new JSONObject();
	try{
		//boolean accepted = false;
		//String CSRstr = Base64.toBase64String( CSR.getEncoded() );

		registration.put( "accepted", false )
		            .put( "csr", Base64.toBase64String( CSR.getEncoded() ) )
		            .put( "deviceId", mUtility.getUniqDeviceID().toString() )
		            .put( "deviceOSType", "Android" )
		            .put( "deviceOSVersion", mUtility.getVersion().toString() )
		            .put( "keeperId", "" )
		            .put( "receivedCode", "" )
		            .put( "referredBy", "JDtest@boosh.com" );

	}catch ( IOException | JSONException X ){
		mLog.error( X.getMessage() );
		registration = pkUtility.errJSONObject;
	}

	mLog.debug( "\nPOST :\t" + registration.toString() );
return registration;
}//getRegistration


private JSONObject getReceivedCode(){
	JSONObject ReceivedCode = new JSONObject();
	try{
		ReceivedCode.put( "receivedCode", "12345678" );
	}catch ( JSONException X ){
		mLog.error( X.getMessage() );
		ReceivedCode = pkUtility.errJSONObject;
	}

	mLog.debug( "\nReceivedCode PATCH :\t" + ReceivedCode.toString() );
return ReceivedCode;
}//getReceivedCode

}//class pkServerAuthenticate



/*
public String userSignUp( final String name, final String email, final String zipCode, final String authType )throws Exception{
	mLog.trace( "userSignUp():\t" );

	String url = "https://api.parse.com/1/users";

	DefaultHttpClient httpClient = new DefaultHttpClient();
	HttpPost httpPost = new HttpPost( url );

	httpPost.addHeader( "X-Parse-Application-Id", "XUafJTkPikD5XN5HxciweVuSe12gDgk2tzMltOhr" );
	httpPost.addHeader( "X-Parse-REST-API-Key", "8L9yTQ3M86O4iiucwWb4JS7HkxoSKo7ssJqGChWx" );
	httpPost.addHeader( "Content-Type", "application/json" );

	String user = "{\"username\":\"" + email + "\",\"password\":\"" + pass + "\",\"phone\":\"415-392-0202\"}";
	HttpEntity entity = new StringEntity( user );
	httpPost.setEntity( (com.belladati.httpclientandroidlib.HttpEntity) entity );

	String authtoken = null;
	try{
		CloseableHttpResponse response = httpClient.execute( httpPost );
		String responseString = EntityUtils.toString( (HttpEntity) response.getEntity() );

		if ( response.getStatusLine().getStatusCode() != 201 ){
			ParseComError error = new Gson().fromJson( responseString, ParseComError.class );
			throw new Exception( "Error creating user[" + error.code + "] - " + error.error );
		}

		User createdUser = new Gson().fromJson( responseString, User.class );

		authtoken = createdUser.sessionToken;
	}catch ( IOException e ){ mLog.error( e.getMessage() ); }

	return authtoken;
}//userSignUp

public String userSignIn( final String user, final String pass, final String authType )throws Exception{
//public String userSignUp( final String name, final String email, final String pass, final String authType )throws Exception{

/*
	mLog.debug( "userSignIn" );

	DefaultHttpClient httpClient = new DefaultHttpClient();
	String url = "https://api.parse.com/1/login"
		 , query = null;

	try{
		query = String.format( "%s=%s&%s=%s", "username", URLEncoder.encode( user, "UTF-8" ),
		                       "password", pass );
	}catch ( UnsupportedEncodingException e ){ mLog.error( e.getMessage() );}
	url += "?" + query;

	HttpGet httpGet = new HttpGet( url );

	httpGet.addHeader( "X-Parse-Application-Id", "XUafJTkPikD5XN5HxciweVuSe12gDgk2tzMltOhr" );
	httpGet.addHeader( "X-Parse-REST-API-Key", "8L9yTQ3M86O4iiucwWb4JS7HkxoSKo7ssJqGChWx" );

	HttpParams params = new BasicHttpParams();
	params.setParameter( "username", user )
		  .setParameter( "password", pass );

	httpGet.setParams( (com.belladati.httpclientandroidlib.params.HttpParams) params );
//        httpGet.getParams().setParameter("username", user).setParameter("password", pass);

	String authtoken = null;
	try{
		//HttpResponse response = httpClient.execute(httpGet);
		CloseableHttpResponse response = httpClient.execute( httpGet );
		String responseString = EntityUtils.toString( (HttpEntity) response.getEntity() );
		if ( response.getStatusLine().getStatusCode() != 200 ){
			ParseComError error = new Gson().fromJson( responseString, ParseComError.class );
			throw new Exception( "Error signing-in [" + error.code + "] - " + error.error );
		}

		User loggedUser = new Gson().fromJson( responseString, User.class );
		authtoken = loggedUser.sessionToken;

	}catch ( IOException e ){ mLog.error( e.getMessage() );}

return authtoken;
}//userSignIn()


*/
