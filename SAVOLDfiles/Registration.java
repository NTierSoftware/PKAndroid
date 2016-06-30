// Created by John Donaldson, NTier Software Engineering on 4/7/2016.
package org.peacekeeper.rest;

import com.android.volley.AuthFailureError;
import com.android.volley.Request.Method;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;
import java.util.*;


public class Registration extends Post{

//private JsonObjectRequest mJsonRequest;

public < E extends Enum< E > > Registration( E aURL ){
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
		JSONObject receivedCode
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


private JSONObject getReceivedCode(){

	JSONObject ReceivedCode = new JSONObject();
	try{
		ReceivedCode.put( "receivedCode", "12345678" );
	}catch ( JSONException X ){ mLog.error( X.getMessage() ); }

	mLog.debug( "\nPATCH :\t" + ReceivedCode.toString() );
	return ReceivedCode;
}//getRegistration


}//class Post


