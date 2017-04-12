/*Code for chaining asynch Volley requests. What's important is:
		a) The call to nextRequest() in the mJsonResp listener.
		nextRequest() ties each link in the chain together and is called by the listener.
		b) The member mLinkedRequest.
		c) The abstract function needing override getRequest().
http://stackoverflow.com/questions/33228364/need-to-send-multiple-volley-requests-in-a-sequence/38315244#38315244
*/
package org.peacekeeper.service;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.

import android.support.annotation.NonNull;

import com.android.volley.*;
import com.android.volley.Request.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.*;

import org.json.JSONObject;
import org.peacekeeper.util.pkUtility;

import java.net.*;
import java.util.*;

public class pkRequest{

//getRequest is used for the FUTURE request of mLinkedRequest.
//for CURRENT request of this() use the constructor
//abstract public JSONObject getRequest( final JSONObject response );

protected static org.slf4j.Logger mLog;
protected final static pkUtility mUtility = pkUtility.getInstance();
protected final static RequestQueue mRequestQueue = mUtility.getRequestQueue();

static private HashMap< String, String > newHeader(){
	HashMap< String, String > newHeader = new HashMap<>();
	final String applicationjson = "application/json";

	newHeader.put( "Accept", applicationjson );
	newHeader.put( "Content-Type", applicationjson );
	return newHeader;
}

static private HashMap<String, String> mHeaders = newHeader()
		, registrations2Hdr = newHeader();
static{	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	registrations2Hdr.put( "X-HTTP-Method-Override", "PATCH" );
}
//end static


private UUID msg_id = UUID.randomUUID();

//each enum needs priority and method, url, request, response, errlsnr
protected Response.Listener< JSONObject > mJsonResp =  new Response.Listener< JSONObject >(){
	@Override public void onResponse( JSONObject response ){
		String respStr = "response:\t" + ((response == null)? "NULL" : response.toString() );
		mLog.debug( "onResponse\t url:\t" + mPkURL.toString() + "\t:Response:\t" + respStr );
		//mLog.debug(  "Response:\t" + respStr );

		//nextRequest( response );
	}
};

protected final ErrorListener mErrLsnr = new ErrorListener(){
	@Override public void onErrorResponse( VolleyError error ){
		mLog.error( "Error!:\t" + error.getMessage() );

		if( error.networkResponse != null){
			NetworkResponse response = error.networkResponse;
			int statusCode = error.networkResponse.statusCode;
			mLog.error( "statuscode:\t" + statusCode + ":\t" + new String(response.data) );
		}
	}
};


//PLACE ALL URL NAMES HERE
public enum pkURL{
	//each link in the chain should be of ever increasing priority to reflect its progression.
	devices(Method.GET, Priority.IMMEDIATE)
	// http://stackoverflow.com/questions/28314160/error-in-volley-patch
	// java.net.ProtocolException: Unknown method 'PATCH'; must be one of [OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE]
	// http://stackoverflow.com/questions/19797842/patch-request-android-volley
	, registrations2( Method.POST, Priority.HIGH, pkRequest.registrations2Hdr)
	, registrations(Method.POST, Priority.NORMAL )
	, status
	;

	//each enum needs priority and method, url, request, response, errlsnr
	protected Priority mPriority;
	protected int mMethod;
	public HashMap< String, String > mHeader;
	public String URLstr;

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

private JsonObjectRequest mJsonRequest   = null;//The current request of this link. For future Request override getRequest().
private pkRequest         mLinkedRequest = null;
//This constructor is only used for intermediate and last links in the chain.
public pkRequest( @NonNull pkURL aPkURL, pkRequest aLinkedRequest ){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );
	mPkURL = aPkURL;

	if ( aLinkedRequest != null ){
		mLinkedRequest = aLinkedRequest;
		mLinkedRequest.msg_id = this.msg_id;
	}
}//cstr


//This constructor is  only used for the first link in the chain or singleton requests.
public pkRequest(  final pkURL aPkURL, JSONObject aRequestBody, pkRequest aLinkedRequest ){
	this( aPkURL, aLinkedRequest );

	try{ aRequestBody.put( "_id", msg_id.toString() ); } catch ( Exception ignore ){}

	mJsonRequest = //this is the current request of this link. For future Requests override getRequest().
	new JsonObjectRequest( aPkURL.mMethod, toURL(), aRequestBody, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPkURL.mPriority; }
		@Override public Map<String, String> getHeaders() throws AuthFailureError{ return aPkURL.mHeader; }
	};

	mJsonRequest.setShouldCache( false );


}//cstr


//This constructor is  only used for the first link in the chain or singleton requests.
public pkRequest(  final pkURL aPkURL  ){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );

	mPkURL = aPkURL;
	JSONObject requestBody = new JSONObject(  );
	try{ requestBody.put( "_id", msg_id.toString() ); } catch ( Exception ignore ){}

	mJsonRequest =
			new JsonObjectRequest( aPkURL.mMethod, toURL(), requestBody, mJsonResp, mErrLsnr ){
				@Override public Priority getPriority() { return mPkURL.mPriority; }
				@Override public Map<String, String> getHeaders() throws AuthFailureError{ return aPkURL.mHeader; }
			};

	mJsonRequest.setShouldCache( false );
}//cstr


private static final String URLhead = "http://173.17.175.92:82/"; //TODO make this HTTPS
//private static final String URLhead = "http://192.168.1.156/";
private String toURL(){
	URL url;

	switch ( mPkURL){
	case registrations2:
		this.mPkURL.URLstr = "registrations/" + this.msg_id;
		this.mPkURL.mHeader = registrations2Hdr;
		break;

	case devices:
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
	String hdrs;
	try{ hdrs = this.mJsonRequest.getHeaders().toString(); }
	catch ( AuthFailureError aAuthFailureError ){ hdrs = "ERROR: getHeaders()"; }

	byte[] body = mJsonRequest.getBody();
	String bodyStr = ( body == null ) ? "NULL" : new String( body );


	return new StringBuilder( "\nmethod:\t:" + mJsonRequest.getMethod() )
			.append( "\nheaders:\t" + hdrs )
			.append( "\nbody:\t" + bodyStr  )
			.append( "\nBodyContentType:\t" +  mJsonRequest.getBodyContentType() )
			.append( "\nPriority:\t" +  mJsonRequest.getPriority() )
			.append( "\ngetUrl():\t" +  mJsonRequest.getUrl() )
			.append( "\ntoURL():\t" + toURL )
			.append( "\n" )
			.toString();

}//toString()

public void setResponseListener( Response.Listener< JSONObject > aJsonResp ){ this.mJsonResp = aJsonResp; }


public Request submit(){
	mLog.debug( "submit:\n" + this.toString() );
	//if ( mLinkedRequest != null ) mLog.debug( "mLinkedRequest:\t" + mLinkedRequest.toString() );
if ( mJsonRequest != null ) return mRequestQueue.add( mJsonRequest );
return null;
}//submit()
}//class pkRequest
