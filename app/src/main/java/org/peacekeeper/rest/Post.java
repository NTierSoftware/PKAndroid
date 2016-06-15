// Created by John Donaldson, NTier Software Engineering on 4/7/2016.
package org.peacekeeper.rest;

import android.widget.Toast;

import com.android.volley.Request.*;
import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.json.*;
import org.peacekeeper.crypto.*;
import org.peacekeeper.util.*;
import org.spongycastle.pkcs.*;
import org.spongycastle.util.encoders.*;

import java.io.IOException;
import java.util.*;


public class Post extends Get{
public static enum URLPost{
	registrations //Certificate Signature Request
	, ACRAException
	, testGAEL
	;
}//URLPost

//protected pkUtility mUtility = pkUtility.getInstance();

//public Post( URLPost aURLPost ){
public < E extends Enum< E > > Post( E aURL ){
	super();
	mURL = aURL;

	StringBuilder URLstr = new StringBuilder( toURL( mURL ).toString() );
	//mLog = org.slf4j.LoggerFactory.getLogger( getClass() );
	//String mUrlStr = toURL(url).toString();

	//String mUrlStr = "https://192.168.1.242:8181/GaelWebSvcGF4/rest/GAEL/status";
	switch ( (URLPost) mURL ){
	case registrations:
		break;

	default: break;
	}//switch

	final String url = URLstr.toString();


/*
	//http://developer.android.com/training/volley/requestqueue.html
	stringRequest = new StringRequest( Method.POST, url, mRespLsnr, mErrLsnr){
		@Override protected Map<String, String> getParams(){
		PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
		mLog.debug(SecurityGuard.toPEM(CSR));
		Map<String, String>  registration = new HashMap<>();

		try{
			registration.put( "_id", msg_id.toString() );
			boolean accepted = false;
			registration.put( "accepted", Boolean.toString( accepted ) );

			String CSRstr = Base64.toBase64String( CSR.getEncoded() );
			registration.put( "csr", CSRstr );
//		registration.put("deviceID", mUtility.getUniqDeviceID().toString() );
			registration.put("deviceID", "JD deviceID" );

			registration.put( "deviceOSType", "Android" );
//			registration.put( "deviceOSVersion", mUtility.getVersion().toString() );
			registration.put( "deviceOSVersion", "JD deviceOSVersion" );

			registration.put( "keeperID", "" );
			registration.put( "receivedCode", "" );
			registration.put( "referredBy", "IamtheReferrerTest@boosh.com" );
		} catch ( Exception X ){ mLog.error( X.getMessage() ); }

		mLog.debug( "\nPOST :\t" + registration.toString() );
	  return registration; }//getParams()
	};//new StringRequest
*/


	Response.Listener<JSONObject> jsonResp = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			mLog.debug("JSON onResponse:\t" + response.toString());
		}
	};

	JsonObjectRequest req = new JsonObjectRequest(Method.POST, url, getRegistration(),jsonResp, mErrLsnr){
	@Override public Map<String, String> getHeaders() throws AuthFailureError{
		mLog.debug( "getHeaders()!!" );

		HashMap<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json; charset=utf-8");
		return headers;
	}};

	mUtility.getRequestQueue().add( req );
}//Post



private JSONObject getRegistration(){
	PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
	mLog.debug(SecurityGuard.toPEM(CSR));

	JSONObject registration = new JSONObject();
	try{
		registration.put( "_id", msg_id.toString() );
		boolean accepted = false;
//		registration.put( "accepted", Boolean.toString( accepted ) );
		registration.put( "accepted", accepted);

		String CSRstr = Base64.toBase64String( CSR.getEncoded() );
		registration.put( "csr", CSRstr );

//		registration.put("deviceId", mUtility.getUniqDeviceID().toString() );
		registration.put("deviceId", "" );

		registration.put( "deviceOSType", "Android" );
//		registration.put( "deviceOSVersion", mUtility.getVersion().toString() );
		registration.put( "deviceOSVersion", "JD deviceOSVersion" );

		registration.put( "keeperId", "" );
		registration.put( "receivedCode", "" );
		registration.put( "referredBy", "IamtheReferrerTest@boosh.com" );
		//registration.put(rowObject);
	}	catch( IOException | JSONException X ){ mLog.error( X.getMessage() ); }

	mLog.debug( "\nPOST :\t" + registration.toString() );
return registration;
}//getRegistration


}//class Post

/*
//http://stackoverflow.com/questions/19837820/volley-jsonobjectrequest-post-request-not-working/19945676#19945676
import java.io.UnsupportedEncodingException;
		import java.util.Map;
		import org.json.JSONException;
		import org.json.JSONObject;
		import com.android.volley.NetworkResponse;
		import com.android.volley.ParseError;
		import com.android.volley.Request;
		import com.android.volley.Response;
		import com.android.volley.Response.ErrorListener;
		import com.android.volley.Response.Listener;
		import com.android.volley.toolbox.HttpHeaderParser;

public class CustomRequest extends Request<JSONObject> {

private Listener<JSONObject> listener;
private Map<String, String> params;

public CustomRequest(String url, Map<String, String> params,
                     Listener<JSONObject> reponseListener, ErrorListener errorListener) {
	super(Method.GET, url, errorListener);
	this.listener = reponseListener;
	this.params = params;
}

public CustomRequest(int method, String url, Map<String, String> params,
                     Listener<JSONObject> reponseListener, ErrorListener errorListener) {
	super(method, url, errorListener);
	this.listener = reponseListener;
	this.params = params;
}

protected Map<String, String> getParams()
throws com.android.volley.AuthFailureError {
	return params;
};

@Override
protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
	try {
		String jsonString = new String(response.data,
		                               HttpHeaderParser.parseCharset(response.headers));
		return Response.success(new JSONObject(jsonString),
		                        HttpHeaderParser.parseCacheHeaders(response));
	} catch (UnsupportedEncodingException e) {
		return Response.error(new ParseError(e));
	} catch (JSONException je) {
		return Response.error(new ParseError(je));
	}
}

@Override
protected void deliverResponse(JSONObject response) {
	// TODO Auto-generated method stub
	listener.onResponse(response);
}
}*/
