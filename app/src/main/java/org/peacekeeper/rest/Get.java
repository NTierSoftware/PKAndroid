package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.


import android.widget.Toast;

import com.android.volley.*;
import com.android.volley.Request.*;
import com.android.volley.Response.ErrorListener;
import com.android.volley.toolbox.*;

import org.json.JSONObject;
import org.peacekeeper.util.pkUtility;

import java.net.*;
import java.util.*;


public class Get{
protected static org.slf4j.Logger mLog;//	= org.slf4j.LoggerFactory.getLogger(Get.class);
protected final static pkUtility mUtility = pkUtility.getInstance();
protected final static RequestQueue mRequestQueue = mUtility.getRequestQueue();
//end static

protected final UUID msg_id = UUID.randomUUID();
//protected StringRequest stringRequest = null;
protected Enum  mURL;
protected Toast mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );

protected Priority mPriority = Priority.LOW;
//protected  JsonObjectRequest mJsonRequest;


protected Response.Listener< JSONObject > mJsonResp =  new Response.Listener< JSONObject >(){
	@Override public void onResponse( JSONObject response ){
		String respStr = "response:\t" + ((response == null)? "NULL" : response.toString() );
		mToast.setText( respStr );
		mToast.show();
		mLog.debug( "url:\t" + mURL + "\t:Response:\t" + respStr );
		nextRequest();
	}
};

protected ErrorListener mErrLsnr = new ErrorListener(){
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


protected JsonObjectRequest mJsonRequest, mNextRequest;


//PLACE ALL URL NAMES HERE
public enum URLGet{
	status
	, devices
}//enum GET

//http://developer.android.com/training/volley/requestqueue.html
public Get(){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );
	//mUtility = pkUtility.getInstance();
	//mLog.debug( "?? null arg Get() cstr/t getClass()/t" + getClass() );
	//mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );
}//cstr


public < E extends Enum< E > > Get( E aURL ){
	this();
	mLog.debug( "aURL instanceof URLGet\t" +  (aURL instanceof URLGet) );
	mURL = aURL;

/*
	mJsonRequest = new JsonObjectRequest( Method.POST, url, getRegistration(), jsonResp, mErrLsnr){
		@Override public Map<String, String> getHeaders() throws AuthFailureError{
			mLog.debug( "getHeaders()!!" );

			HashMap<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset=utf-8");
			return headers;
		}};
*/


	mJsonRequest = new JsonObjectRequest( toURL().toString(), null, mJsonResp, mErrLsnr ){
		@Override public Priority getPriority() { return mPriority; }

		@Override public Map<String, String> getHeaders() throws AuthFailureError{
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset=utf-8");
			return headers;
		}};

	mJsonRequest.setShouldCache( false );

}//cstr

public Request submit(){ return mRequestQueue.add( mJsonRequest ); }

//private static final String URLhead = "http://192.168.1.242:8888/"; http://192.168.1.156/status
private static final String URLhead = "http://192.168.1.156/";
public < E extends Enum< E > > URL toURL( E URLPostOrGet ){
	try{URL url =  new URL(URLhead + URLPostOrGet.name() + "/" );
		mLog.debug( "toURL:\t" + url );
		return url; }
	catch ( MalformedURLException e ){ mLog.error( "Error!:\t" + e.getMessage() ); }
	return null;
}//toURL

public < E extends Enum< E > > URL toURL(){ return toURL( mURL );}

public Request nextRequest(){
	Request retVal = null;
	if ( this.mNextRequest != null ){
		this.mJsonRequest = this.mNextRequest;
		retVal =  this.submit();
		this.mNextRequest = null;
	}
	return retVal;
}//nextRequest()
}//class Get
