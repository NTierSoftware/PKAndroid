package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.


import android.support.annotation.NonNull;
import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.Request.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.*;
import org.peacekeeper.util.pkUtility;

import java.net.*;
import java.util.*;

public abstract class pkRequest{

protected static org.slf4j.Logger mLog;
protected final static pkUtility mUtility = pkUtility.getInstance();
protected final static RequestQueue mRequestQueue = mUtility.getRequestQueue();
//end static


protected UUID msg_id = UUID.randomUUID();

protected final Toast mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );
//each enum needs priority and method, url, request, response, errlsnr


protected final Response.Listener< JSONObject > mJsonResp =  new Response.Listener< JSONObject >(){
	@Override public void onResponse( JSONObject response ){
		String respStr = "response:\t" + ((response == null)? "NULL" : response.toString() );
		mToast.setText( respStr );
		mToast.show();
		mLog.debug( "url:\t" + mPkURL + "\t:Response:\t" + respStr );
		nextRequest( response );
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

/*
static private HashMap< String, String > mHeaders ;
static{
	mHeaders = new HashMap<>();
	mHeaders.put( "Accept", "application/json" );
	mHeaders.put( "Content-Type", "application/json; charset=utf-8" );
}
*/


//PLACE ALL URL NAMES HERE
public enum pkURL{
//each link in the chain should be of ever increasing priority to reflect its progression.
	devices(Method.GET, Priority.IMMEDIATE, null)
	//http://stackoverflow.com/questions/28314160/error-in-volley-patch
	//java.net.ProtocolException: Unknown method 'PATCH'; must be one of [OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE]
	, registrations2(Method.PATCH, Priority.HIGH, devices)
	, registrations(Method.POST, Priority.NORMAL, registrations2 )

	, status
	;

	//each enum needs priority and method, url, request, response, errlsnr
	protected Priority mPriority = Priority.LOW;
	protected int mMethod = Method.GET;

	public pkURL mNextURL = null;

	pkURL(){}

	pkURL(int aMethod, Priority aPriority, pkURL aNextURL ){
		mMethod = aMethod;
		mPriority = aPriority;
		mNextURL = aNextURL;
	}
}//enum pkURL

private JsonObjectRequest mJsonRequest = null;
private pkRequest mChainedRequest = null;

public pkURL mPkURL;
//http://developer.android.com/training/volley/requestqueue.html

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
public pkRequest( @NonNull pkURL aPkURL, @NonNull JSONObject aRequestBody, pkRequest aChainedRequest ){
	this( aPkURL, aChainedRequest );

	try{ aRequestBody.put( "_id", msg_id.toString() ); } catch ( Exception ignore ){}

	mJsonRequest =
	new JsonObjectRequest( toString(), aRequestBody, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPkURL.mPriority; }
		//@Override public Map<String, String> getHeaders() throws AuthFailureError{ return mHeaders; }
	};

	mJsonRequest.setShouldCache( false );
}//cstr




public Request submit(){ return mRequestQueue.add( mJsonRequest ); }

private static final String URLhead = "http://192.168.1.156/";
private URL toURL(){
//private < E extends Enum< E > > URL toURL( E URLPostOrGet ){
	URL url;
	try{
		url =  new URL(URLhead + this.mPkURL.name() + "/" );
		mLog.debug( "toURL:\t" + url );
	}
	catch ( MalformedURLException e ){
		mLog.error( "Error!:\t" + e.getMessage() );
		url = null;
	}
	return url;
}//toURL


@Override public String toString(){ return toURL().toString();}


//getRequest is used for the FUTURE request of mChainedRequest.
//for CURRENT request of this() use the constructor
abstract public JSONObject getRequest( JSONObject response );
//abstract public JSONObject nextJSON( JSONObject response );

private Request nextRequest( JSONObject response ){
	Request retVal = null;
	if ( mChainedRequest != null ){

		this.mJsonRequest =
			new JsonObjectRequest( mChainedRequest.mPkURL.mMethod, mChainedRequest.toString(),
			                       mChainedRequest.getRequest( response ),
			                       mJsonResp, mErrLsnr ){

			@Override public Priority getPriority() { return mPkURL.mPriority; }
/*
			@Override public Map<String, String> getHeaders() throws AuthFailureError{
				mLog.debug( "getHeaders()!!" );
				return mHeaders;
			}
*/

			};

		mJsonRequest.setShouldCache( false );
		retVal =  submit();
	}
	return retVal;
}//nextRequest()

}//class pkRequest




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
public < E extends Enum< E > > pkRequest( E aURL ){
	this();
	mLog.debug( "aURL instanceof pkURL\t" +  (aURL instanceof pkURL ) );
	mURL = aURL;

	mJsonRequest = new JsonObjectRequest( toURL().toString(), null, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPriority; }

		@Override public Map<String, String> getHeaders() throws AuthFailureError{
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset=utf-8");
			return headers;
		}};

	mJsonRequest.setShouldCache( false );
}//cstr
*/


/*
//private static final String URLhead = "http://192.168.1.242:8888/"; http://192.168.1.156/status
private URL toURL( pkURL URLPostOrGet ){
//private < E extends Enum< E > > URL toURL( E URLPostOrGet ){
	URL url;
	try{
		url =  new URL(URLhead + URLPostOrGet.name() + "/" );
		mLog.debug( "toURL:\t" + url );
	}
	catch ( MalformedURLException e ){
		mLog.error( "Error!:\t" + e.getMessage() );
		url = null;
	}
return url;
}//toURL
*/

//public URL toURL(){ return toURL( mPkURL );}

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
