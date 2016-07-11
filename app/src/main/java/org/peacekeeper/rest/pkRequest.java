/*Code for chaining asynch volley requests. What's important is:
		a) The call to nextRequest() in the mJsonResp listener.
		nextRequest() ties each link in the chain together and is called by the listener.
		b) The member mChainedRequest.
		c) The abstract function needing override getRequest().
http://stackoverflow.com/questions/33228364/need-to-send-multiple-volley-requests-in-a-sequence/38315244#38315244
*/
package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.


import android.support.annotation.NonNull;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.Request.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.peacekeeper.crypto.SecurityGuard.entryType;
import org.peacekeeper.util.pkUtility;

import java.net.*;
import java.util.*;


public abstract class pkRequest{

protected static org.slf4j.Logger mLog;
protected final static pkUtility mUtility = pkUtility.getInstance();
protected final static RequestQueue mRequestQueue = mUtility.getRequestQueue();
protected final static Toast mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );

static private HashMap< String, String > newHeader(){
	HashMap< String, String > newHeader = new HashMap<>();
	final String applicationjson = "application/json";

	newHeader.put( "Accept", applicationjson );
	newHeader.put( "Content-Type", applicationjson );
	return newHeader;
}

static private HashMap<String, String> mHeaders = newHeader()
		, registrations2Hdr = newHeader();
static{
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	registrations2Hdr.put( "X-HTTP-Method-Override", "PATCH" );
}


//end static


private UUID msg_id = UUID.randomUUID();

//each enum needs priority and method, url, request, response, errlsnr

private JSONObject mResponse = null;


protected final Response.Listener< JSONObject > mJsonResp =  new Response.Listener< JSONObject >(){
	@Override public void onResponse( JSONObject response ){
		String respStr = "response:\t" + ((response == null)? "NULL" : response.toString() );
		mToast.setText( respStr );
		mToast.show();
		mLog.debug( "onResponse\t url:\t" + mPkURL.toString() + "\t:Response:\t" + respStr );
		mResponse = response;
		nextRequest();
	}
};



protected final ErrorListener mErrLsnr = new ErrorListener(){
	@Override public void onErrorResponse( VolleyError error ){
		mToast.setText( "Error:\t" + error.getMessage() );
		mToast.show();
		mLog.error( "Error!:\t" + error.getMessage() );

		if( error.networkResponse != null){
			NetworkResponse response = error.networkResponse;
			int statusCode = error.networkResponse.statusCode;
			mLog.error( "statuscode:\t" + statusCode + ":\t" + new String(response.data) );
		}
	}
};


private HashMap< String, String > getAuthorizationHeader(){
	HashMap< String, String > newHeader = newHeader();
	//newHeader.put( "Authorization", SecurityGuard.getAuthToken() );
	newHeader.put( "Authorization", "test getAuthToken()" );
	mLog.debug( newHeader.toString() );
	return newHeader;
}

//PLACE ALL URL NAMES HERE
public enum pkURL{
	//each link in the chain should be of ever increasing priority to reflect its progression.
	devices(Method.GET, Priority.IMMEDIATE)
	// http://stackoverflow.com/questions/28314160/error-in-volley-patch
	// java.net.ProtocolException: Unknown method 'PATCH'; must be one of [OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE]
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
//	, registrations2( Method.POST, Priority.HIGH, pkRequest.registrations2Hdr, devices)
	, registrations2( Method.POST, Priority.HIGH, pkRequest.registrations2Hdr)
	//, registrations2(Method.PATCH, Priority.HIGH, devices)
	, registrations(Method.POST, Priority.NORMAL )
	, status
	;

	//each enum needs priority and method, url, request, response, errlsnr
	protected Priority mPriority;
	protected int mMethod;
	public HashMap< String, String > mHeader;
	protected String URLstr;
	//public pkURL mNextURL = null;

	pkURL(){
		mMethod = Method.GET;
		mPriority = Priority.LOW;
		mHeader = pkRequest.mHeaders;
	}

	pkURL(int aMethod, Priority aPriority ){
		mMethod = aMethod;
		mPriority = aPriority;
		mHeader = pkRequest.mHeaders;
	}

	pkURL(int aMethod,
	      Priority aPriority,
	      HashMap< String, String > aHeader){
		this(aMethod, aPriority);
		mHeader = aHeader;
	}

}//enum pkURL



public pkURL mPkURL;

private JsonObjectRequest mJsonRequest = null;//The current request of this link. For future Request override getRequest().
private pkRequest mChainedRequest = null;
//This constructor is only used for intermediate and last links in the chain.
public pkRequest( @NonNull pkURL aPkURL, pkRequest aChainedRequest ){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );
	mPkURL = aPkURL;

	if ( aChainedRequest != null ){
		mChainedRequest = aChainedRequest;
		mChainedRequest.msg_id = this.msg_id;
	}
}//cstr



//This constructor is  only used for the first link in the chain or singleton requests.
public pkRequest( @NonNull final pkURL aPkURL, @NonNull JSONObject aRequestBody, pkRequest aChainedRequest ){
	this( aPkURL, aChainedRequest );

	try{ aRequestBody.put( "_id", msg_id.toString() ); } catch ( Exception ignore ){}

	mJsonRequest = //this is the current request of this link. For future Requests override getRequest().
	new JsonObjectRequest( aPkURL.mMethod, toURL(), aRequestBody, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPkURL.mPriority; }
		@Override public Map<String, String> getHeaders() throws AuthFailureError{ return aPkURL.mHeader; }
	};

	mJsonRequest.setShouldCache( false );
}//cstr


private static final String URLhead = "http://192.168.1.156/";
private String toURL(){ //} throws JSONException{
//private < E extends Enum< E > > URL toURL( E URLPostOrGet ){
	URL url;

	switch ( mPkURL){
	case registrations2:
		this.mPkURL.URLstr = "registrations/" + this.msg_id;
		this.mPkURL.mHeader = registrations2Hdr;
		break;

	case devices:
		final String deviceID = "testdeviceId" //SecurityGuard.getEntry( entryType.deviceId )
				, keeperID = "testkeeperId" //SecurityGuard.getEntry( entryType.keeperId )
				;

//GET http://192.168.1.156:80/devices/bbdef07b-9360-4d7b-8448-21c4daca4711?where=keeperId=="b0c486e5-13b1-4555-ba5f-d54bb1f0a6f7"
		this.mPkURL.URLstr = new StringBuilder( "devices/" )
				.append( deviceID )
				.append( "?where=keeperId==\"" )
				.append( keeperID + "\"" )
				.toString();

		this.mHeaders = getAuthorizationHeader();
		break;

	default: this.mPkURL.URLstr = mPkURL.name();
	}//switch

	try{
		url =  new URL(URLhead + this.mPkURL.URLstr + "/" );
	}
	catch ( MalformedURLException e ){
		mLog.error( "Error!:\t" + e.getMessage() );
		url = null;
	}
	return url.toString();
}//toURL


@Override public String toString(){
	String toURL = toURL();

	if (mJsonRequest == null) return "toString() NULL mJsonRequest:\t" + toURL;
	String hdrs ="";
/*
	try{
		hdrs = this.mJsonRequest.getHeaders().toString();
	}
	catch ( AuthFailureError aAuthFailureError ){
		hdrs = "ERROR: getHeaders()";
	}
*/


	return new StringBuilder( "\nmethod:\t:" + mJsonRequest.getMethod() )
			.append( "\nheaders:\t" + hdrs )
			.append( "\nbody:\t" +  new String( mJsonRequest.getBody()) )
			.append( "\nBodyContentType:\t" +  mJsonRequest.getBodyContentType() )
			.append( "\nPriority:\t" +  mJsonRequest.getPriority() )
			.append( "\ngetUrl():\t" +  mJsonRequest.getUrl() )
			.append( "\ntoURL():\t" + toURL )
			.append( "\n" )
			.toString();

}//toString()


//getRequest is used for the FUTURE request of mChainedRequest.
//for CURRENT request of this() use the constructor
abstract public JSONObject getRequest( JSONObject response );

private Request nextRequest(){//nextRequest() ties each link in the chain together and is called by the listener.
//private Request nextRequest( JSONObject response ){
	mLog.debug( "nextRequest:\t" + mResponse.toString() );
	Request retVal = null;
// * * * * * * This is how and where the the "future" request must be called/constructed. * * * * * *
	if ( mChainedRequest != null ){
		final JSONObject requestBody = mChainedRequest.getRequest( mResponse );

		final String aURL = mChainedRequest.toURL();
		final Map<String, String> header = mChainedRequest.mPkURL.mHeader;
		final int mMethod = mChainedRequest.mPkURL.mMethod;
		this.mJsonRequest = 	new JsonObjectRequest( mMethod, aURL,
		                                              requestBody,
		                                              mJsonResp, mErrLsnr ){

			@Override public Priority getPriority() { return mPkURL.mPriority; }
			@Override public Map<String, String> getHeaders() throws AuthFailureError{
				return header;
			}
		};

		mJsonRequest.setShouldCache( false );
		this.mChainedRequest = mChainedRequest.mChainedRequest;
		retVal =  submit();
	}//if
// * * * * * *  * * * * * * * * * * * *  * * * * * * * * * * * *  * * * * * * * * * * * * * * * * * *

return retVal;
}//nextRequest()


public Request submit(){
	mLog.debug( "submit:\n" );
	mLog.debug( this.toString() );
	if (mChainedRequest!= null ) mLog.debug( "mChainedRequest:\t" + mChainedRequest.toString() );
	//if (this.mPkURL == pkURL.registrations2 ) return null;
	if ( mJsonRequest!= null ) return mRequestQueue.add( mJsonRequest );
	return null;
}//submit()


}//class pkRequest
