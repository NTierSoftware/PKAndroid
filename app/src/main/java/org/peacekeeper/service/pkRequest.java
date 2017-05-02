/*Code for chaining asynch Volley requests. What's important is:
		a) The call to nextRequest() in the mJsonResp listener.
		nextRequest() ties each link in the chain together and is called by the listener.
		b) The member mLinkedRequest.
		c) The abstract function needing override getRequest().
http://stackoverflow.com/questions/33228364/need-to-send-multiple-volley-requests-in-a-sequence/38315244#38315244
*/
package org.peacekeeper.service;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.

import com.android.volley.*;
import com.android.volley.Request.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.*;

import org.json.*;
import org.peacekeeper.util.pkUtility;

import java.net.*;
import java.util.*;

public class pkRequest{

//getRequest is used for the FUTURE request of mLinkedRequest.
//for CURRENT request of this() use the constructor
//abstract public JSONObject getRequest( final JSONObject response );

public pkURL mPkURL;
//protected final static pkUtility    mUtility      = pkUtility.getInstance();
//protected final pkUtility    mUtility      = pkUtility.getInstance();
protected final static RequestQueue mRequestQueue = pkUtility.getInstance().getRequestQueue();
protected static org.slf4j.Logger mLog;

private static final String                    URLhead  = "http://173.17.175.92:82/"; //TODO make this HTTPS
private static HashMap< String, String > mHeaders = newHeader()
										, registrations2Hdr = newHeader();
//end static
protected final ErrorListener                   mErrLsnr  = new ErrorListener(){
	@Override public void onErrorResponse( VolleyError error ){
		mLog.error( "Error!:\t" + error.getMessage() );

		if ( error.networkResponse != null ){
			NetworkResponse response = error.networkResponse;
			int statusCode = error.networkResponse.statusCode;
			mLog.error( "statuscode:\t" + statusCode + ":\t" + new String( response.data ) );
		}
	}
};
//each enum needs priority and method, url, request, response, errlsnr
protected       Response.Listener< JSONObject > mJsonResp = new Response.Listener< JSONObject >(){
	@Override public void onResponse( JSONObject response ){
		String respStr = ( response == null ) ? "NULL" : response.toString();
		mLog.debug( "onResponse\t url:\t" + mPkURL.toString() + "\t:response:\t" + respStr );
	}
};
RequestFuture< JSONObject > mFuture = RequestFuture.newFuture();
/*
58ee 6c74 7da4 b000 0cf8 24e2
9a49 3320 f582 42ab b9b4 48d9 73cf 8a40
*/
//private String            msg_id       = UUID.randomUUID().toString().replace( "-", "" ).substring(	8 );
private JsonObjectRequest mJsonRequest = null;

static{    // http://stackoverflow.com/questions/19797842/patch-request-android-volley
	registrations2Hdr.put( "X-HTTP-Method-Override", "PATCH" );
}


//PLACE ALL URL NAMES HERE
public enum pkURL{
	//each link in the chain should be of ever increasing priority to reflect its progression.
	devices( Method.GET, Priority.IMMEDIATE )
	// http://stackoverflow.com/questions/28314160/error-in-volley-patch
	// java.net.ProtocolException: Unknown method 'PATCH'; must be one of [OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE]
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	,
	registrations2( Method.POST, Priority.HIGH, pkRequest.registrations2Hdr ),
	registrations( Method.POST, Priority.NORMAL ),
	status;

	public    HashMap< String, String > mHeader;
	private   String                    URLstr = "";
	//each enum needs priority and method, url, request, response, errlsnr
	protected Priority                  mPriority;
	protected int                       mMethod;

	pkURL(){
		mMethod = Method.GET;
		mPriority = Priority.LOW;
		mHeader = pkRequest.mHeaders;
	}

	pkURL( int aMethod,
	       Priority aPriority,
	       HashMap< String, String > aHeader ){
		this( aMethod, aPriority );
		mHeader = aHeader;
	}

	pkURL( int aMethod, Priority aPriority ){
		mMethod = aMethod;
		mPriority = aPriority;
		mHeader = pkRequest.mHeaders;
	}

	public pkURL addToURL(String aString){
		URLstr += aString;
		return this;
	}
}//enum pkURL

///This constructor is  only used for the first link in the chain or singleton requests.
public pkRequest( final pkURL aPkURL, JSONObject aRequestBody ){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );

	mPkURL = aPkURL;

	if ( aRequestBody == null ) aRequestBody = new JSONObject();
/*
	try{ aRequestBody.put( "_id", msg_id ); }
	catch ( Exception x ){
		mLog.error( x.toString() );
		x.printStackTrace();
	}
*/

	mJsonRequest =
			//this is the current request of this link. For future Requests override getRequest().
//			new JsonObjectRequest( aPkURL.mMethod, toURL(), aRequestBody, mJsonResp, mErrLsnr ){
			new JsonObjectRequest( aPkURL.mMethod, toURL(), aRequestBody, mFuture, mFuture ){
				@Override public Map< String, String > getHeaders() throws
						AuthFailureError{ return aPkURL.mHeader; }

				@Override public Priority getPriority(){ return mPkURL.mPriority; }

			};

	mJsonRequest.setShouldCache( false );
}//cstr

//private static final String URLhead = "http://192.168.1.156/";

private String toURL(){
	URL url;

	switch ( mPkURL ){
	case registrations2:
		this.mPkURL.URLstr = "registrations/" + this.mPkURL.URLstr;
		this.mPkURL.mHeader = registrations2Hdr;
		break;

	case devices:
		break;

	default:
		this.mPkURL.URLstr = mPkURL.name() + "/";
	}//switch

	try{
		url = new URL( URLhead + this.mPkURL.URLstr  );
	}catch ( MalformedURLException e ){
		mLog.error( "Error!:\t" + e.getMessage() );
		url = null;
	}
	return url.toString();
}//toURL

//This constructor is  only used for the first link in the chain or singleton requests.
public pkRequest( final pkURL aPkURL ){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );

	mPkURL = aPkURL;
	JSONObject requestBody = new JSONObject();
/*
	try{ requestBody.put( "_id", msg_id ); }
	catch ( Exception x ){
		mLog.error( x.toString() );
		x.printStackTrace();
	}
*/

	mJsonRequest =
//			new JsonObjectRequest( aPkURL.mMethod, toURL(), requestBody, mJsonResp, mErrLsnr ){
			new JsonObjectRequest( aPkURL.mMethod, toURL(), requestBody, mFuture, mFuture ){
				@Override public Priority getPriority(){ return mPkURL.mPriority; }

				@Override public Map< String, String > getHeaders() throws
						AuthFailureError{ return aPkURL.mHeader; }
			};

	mJsonRequest.setShouldCache( false );

}//cstr

static private HashMap< String, String > newHeader(){
	HashMap< String, String > newHeader = new HashMap<>();
	final String applicationjson = "application/json";

	newHeader.put( "Accept", applicationjson );
	newHeader.put( "Content-Type", applicationjson );
	return newHeader;
}

//public void setResponseListener( Response.Listener< JSONObject > aJsonResp ){ this.mJsonResp = aJsonResp; }

public void submit(){
	//pkUtility.getInstance().debugToast( "submit:\n" + this.toString() );
	mLog.debug( "submit:\n" + this.toString() );
	if ( mJsonRequest != null ) //return mRequestQueue.add( mJsonRequest );
		mFuture.setRequest( mRequestQueue.add( mJsonRequest ) );

}//submit()

@Override public String toString(){
	String toURL = toURL(),
			headers ="",
			JSONbody = "null";

	if ( mJsonRequest == null ) return "toString() NULL mJsonRequest:\t" + toURL;

	try{ headers = this.mJsonRequest.getHeaders().toString(); }
	catch ( AuthFailureError aAuthFailureError ){ headers = "ERROR: getHeaders()"; }


	byte[] body = mJsonRequest.getBody();
	if ( body != null ){
		try{
			final int indentSpaces = 4;
			JSONbody = new JSONObject( new String( body ) ).toString( indentSpaces );
		}catch ( JSONException aE ){
			mLog.debug( aE.getMessage() );
			JSONbody = "error parsing JSONbody!";
		}
	}//if
	return new StringBuilder( "\nmethod:\t:" + mJsonRequest.getMethod() )
			.append( "\nheaders:\t" + headers )
			.append( "\nbody:\t" + JSONbody )
			.append( "\nbody length:\t" + body.length )
			.append( "\nBodyContentType:\t" + mJsonRequest.getBodyContentType() )
			.append( "\nPriority:\t" + mJsonRequest.getPriority() )
			.append( "\ngetUrl():\t" + mJsonRequest.getUrl() )
			.append( "\ntoURL():\t" + toURL  )
			//.append( "\n" )
			.toString();

}//toString()

}//class pkRequest
