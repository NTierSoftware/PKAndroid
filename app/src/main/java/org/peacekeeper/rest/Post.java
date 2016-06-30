// Created by John Donaldson, NTier Software Engineering on 4/7/2016.
package org.peacekeeper.rest;

import com.android.volley.*;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;
import java.util.*;


public class Post extends Get{
public enum URLPost {
	registrations //Certificate Signature Request
	, invitationReplies, invitations, tribes, ACRAException, test;

}//URLPost

//private JsonObjectRequest mJsonRequest;

public < E extends Enum< E > > Post( E aURL ){
	super();
	mURL = aURL;

	//StringBuilder URLstr = new StringBuilder( toURL().toString() );

	//String mUrlStr = "https://192.168.1.242:8181/GaelWebSvcGF4/rest/GAEL/status";
	switch ( (URLPost) mURL ){
	case registrations:
		break;

	default:
		break;
	}//switch



//each enum needs priority and method, url, request, response, errlsnr
	JsonObjectRequest mJsonRequest2 = new JsonObjectRequest( Method.PATCH, "http://192.168.1.156/",
	                                                         getReceivedCode(), mJsonResp,
	                                                         mErrLsnr ){
		@Override public Map< String, String > getHeaders() throws AuthFailureError{
			mLog.debug( "getHeaders()!!" );

			HashMap< String, String > headers = new HashMap<>();
			headers.put( "Accept", "application/json" );

			return headers;
		}

		@Override public Priority getPriority(){ return Priority.NORMAL; }
	};


/*
	Response.Listener< JSONObject > jsonResp = new Response.Listener< JSONObject >(){
		@Override public void onResponse( JSONObject response ){
			mLog.debug( "JSON onResponse:\t" + response.toString() );
		}
	};
*/


//each enum needs priority and method, url, request, response, errlsnr
	mJsonRequest = new JsonObjectRequest( Method.POST, toURL().toString(), getRegistration(),
	                                      mJsonResp, mErrLsnr ){
		/*
			@Override public Map<String, String> getHeaders() throws AuthFailureError{
				mLog.debug( "getHeaders()!!" );

				HashMap<String, String> headers = new HashMap<>();
				headers.put("Accept", "application/json");
		//		headers.put("Accept", "application/json; charset=utf-8");
		//		headers.put("Content-Type", "application/json; charset=utf-8");

				return headers;
			}
		*/
		@Override public Priority getPriority(){ return mPriority; }
	};

}//Post


private JSONObject getRegistration(){
	PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
	mLog.debug( SecurityGuard.toPEM( CSR ) );

	JSONObject registration = new JSONObject();
	try{
		boolean accepted = false;
		String CSRstr = Base64.toBase64String( CSR.getEncoded() );

		registration.put( "_id", msg_id.toString() )
		            .put( "accepted", accepted )
		            .put( "csr", CSRstr )
		            .put( "deviceId", mUtility.getUniqDeviceID().toString() )
		            .put( "deviceOSType", "Android" )
		            .put( "deviceOSVersion", mUtility.getVersion().toString() )
		            .put( "keeperId", "" )
		            .put( "receivedCode", "" )
		            .put( "referredBy", "IamtheReferrerTest@boosh.com" );

	}catch ( IOException | JSONException X ){ mLog.error( X.getMessage() ); }

	mLog.debug( "\nPOST :\t" + registration.toString() );
	return registration;
}//getRegistration

private JSONObject getReceivedCode(){

	JSONObject ReceivedCode = new JSONObject();
	try{
		ReceivedCode.put( "receivedCode", "12345678" );
	}catch ( JSONException X ){ mLog.error( X.getMessage() ); }

	mLog.debug( "\nPATCH :\t" + ReceivedCode.toString() );
	return ReceivedCode;
}//getReceivedCode


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
