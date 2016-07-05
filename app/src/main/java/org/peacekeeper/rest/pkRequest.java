//http://developer.android.com/training/volley/requestqueue.html
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
	newHeader.put( "Accept", "application/json" );
	newHeader.put( "Content-Type", "application/json; charset=utf-8" );
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	return newHeader;
}


static private HashMap<String, String> mHeaders = newHeader(), reg2Header = newHeader();
static{
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	reg2Header.put( "X-HTTP-Method-Override", "PATCH" );
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
	this.mHeaders.put( "Authorization", SecurityGuard.getAuthToken() );
	return newHeader;
}

//PLACE ALL URL NAMES HERE
public enum pkURL{
//each link in the chain should be of ever increasing priority to reflect its progression.
	devices(Method.GET, Priority.IMMEDIATE, null)
	// http://stackoverflow.com/questions/28314160/error-in-volley-patch
	// java.net.ProtocolException: Unknown method 'PATCH'; must be one of [OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE]
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	//, registrations2(Method.POST, Priority.HIGH, pkRequest.reg2Header, devices)
	//, registrations2(Method.PATCH, Priority.HIGH, reg2Header , devices)
	, registrations2(Method.PATCH, Priority.HIGH, devices)
	, registrations(Method.POST, Priority.NORMAL, registrations2 )
	, status
	;

	//each enum needs priority and method, url, request, response, errlsnr
	protected Priority mPriority;
	protected int mMethod;
	protected HashMap< String, String > mHeader;
	protected String URLstr;
	public pkURL mNextURL = null;

	pkURL(){
		mMethod = Method.GET;
		mPriority = Priority.LOW;
		mHeader = pkRequest.mHeaders;
	}

	pkURL(int aMethod, Priority aPriority, pkURL aNextURL ){
		mMethod = aMethod;
		mPriority = aPriority;
		mHeader = pkRequest.mHeaders;
		mNextURL = aNextURL;
	}

	pkURL(int aMethod,
	      Priority aPriority,
	      HashMap< String, String > aHeader,
	      pkURL aNextURL ){
		this(aMethod, aPriority, aNextURL);
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
	new JsonObjectRequest( toString(), aRequestBody, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPkURL.mPriority; }
		@Override public Map<String, String> getHeaders() throws AuthFailureError{ return aPkURL.mHeader; }
	};

	mJsonRequest.setShouldCache( false );
}//cstr


public Request submit(){
	mLog.debug( "submit:\t" );
	return mRequestQueue.add( mJsonRequest ); }

private static final String URLhead = "http://192.168.1.156/";
private URL toURL() throws JSONException{
//private < E extends Enum< E > > URL toURL( E URLPostOrGet ){
	URL url;

	switch ( mPkURL){
		case registrations2:
			this.mPkURL.URLstr = "registrations/" + this.msg_id;
			break;

	case devices:
		final String deviceID = SecurityGuard.getEntry( entryType.deviceID )
				   , keeperID = SecurityGuard.getEntry( entryType.keeperID );

		this.mPkURL.URLstr = new StringBuilder( "devices/" )
				.append( deviceID )
				.append( "?where=keeperId==" )
				.append( keeperID )
				.toString();

		this.mHeaders = getAuthorizationHeader();
		break;

	default: this.mPkURL.URLstr = mPkURL.name();
	}//switch

	try{
		url =  new URL(URLhead + this.mPkURL.URLstr + "/" );
		mLog.debug( "toURL:\t" + url );
	}
	catch ( MalformedURLException e ){
		mLog.error( "Error!:\t" + e.getMessage() );
		url = null;
	}
	return url;
}//toURL


@Override public String toString(){
	try{ return toURL().toString();}
	catch ( JSONException X ){ mLog.error( X.getMessage() ); }

	return "ERROR in toString()";
}


//getRequest is used for the FUTURE request of mChainedRequest.
//for CURRENT request of this() use the constructor
abstract public JSONObject getRequest( JSONObject response );

private Request nextRequest(){
//private Request nextRequest( JSONObject response ){
	mLog.debug( "nextRequest:\t" + mResponse.toString() );
	Request retVal = null;
	if ( mChainedRequest != null ){
		final JSONObject requestBody = mChainedRequest.getRequest( mResponse );
		final String aURL = mChainedRequest.toString();
		this.mJsonRequest =
			new JsonObjectRequest( mChainedRequest.mPkURL.mMethod, aURL,
			                       requestBody,
			                       mJsonResp, mErrLsnr ){

			@Override public Priority getPriority() { return mPkURL.mPriority; }
			@Override public Map<String, String> getHeaders() throws AuthFailureError{ return mPkURL.mHeader; }
			};

		mJsonRequest.setShouldCache( false );
		this.mChainedRequest = mChainedRequest.mChainedRequest;
		retVal =  submit();
	}//if
	return retVal;
}//nextRequest()



}//class pkRequest


/*
private static int MaxPriority = Priority.IMMEDIATE.ordinal();
private Priority incPriority(){
	return Priority.values()[Math.min( (this.mPkURL.mPriority.ordinal() + 1), MaxPriority )];
}//incPriority
*/


/*
private pkRequest(){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );
	mJsonRequest = new JsonObjectRequest( toURL().toString(), null, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPkURL.mPriority; }

		@Override public Map<String, String> getHeaders() throws AuthFailureError{
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset=utf-8");
			return headers;
		}};

	mJsonRequest.setShouldCache( false );
}//cstr
*/



/*
private pkRequest( @NonNull pkURL aPkURL, JSONObject aRequestBody){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );

	mPkURL = aPkURL;

	//try{ aRequestBody.put( "_id", msg_id.toString() ); } catch ( Exception ignore ){}

*/
/*
	mJsonRequest =
		new JsonObjectRequest( toString(), aRequestBody, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPkURL.mPriority; }

		@Override public Map<String, String> getHeaders() throws AuthFailureError{
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset=utf-8");
			return headers;
		}};
	mJsonRequest.setShouldCache( false );
*//*


}
*/
