package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.


import android.widget.Toast;

import com.android.volley.Request.*;
import com.android.volley.Response.*;
import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.peacekeeper.util.*;

import java.net.*;
import java.util.*;


public class Get{
protected static org.slf4j.Logger mLog;//	= org.slf4j.LoggerFactory.getLogger(Get.class);

protected UUID msg_id = UUID.randomUUID();
protected StringRequest stringRequest = null;
protected pkUtility mUtility = pkUtility.getInstance();
protected Enum  mURL;
//protected Toast mToast, mToast2 ;// = Toast.makeText( this, "onCreate", Toast.LENGTH_SHORT );
protected Toast mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );

protected Response.Listener< String > mRespLsnr =  new Response.Listener< String >(){
	@Override public void onResponse( String response ){
		String respStr = "response:\t" + ((response == null)? "NULL" : response.toString() );
		mToast.setText( respStr );
		mToast.show();
		mLog.debug( "url:\t" + mURL + "\t:Response:\t" + respStr );
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


//PLACE ALL URL NAMES HERE
public static enum URLGet{
	Test, status, testGAEL;
}//enum GET

//http://developer.android.com/training/volley/requestqueue.html
public Get(){
	mLog = org.slf4j.LoggerFactory.getLogger( getClass() );
	//mLog.debug( "?? null arg Get() cstr/t getClass()/t" + getClass() );
}//cstr


public < E extends Enum< E > > Get( E aURL ){
	this();
	mURL = aURL;

	stringRequest = new StringRequest( Method.GET, toURL( mURL ).toString(), mRespLsnr, mErrLsnr );
}//cstr

public void submit(){ mUtility.getRequestQueue().add( stringRequest ); }

private static final String URLhead = "http://192.168.1.242:8888/";
public < E extends Enum< E > > URL toURL( E URLPostOrGet ){
	try{ return new URL(URLhead + URLPostOrGet.name() + "/" ); }
	catch ( MalformedURLException e ){ mLog.error( "Error!:\t" + e.getMessage() ); }
	return null;
}//contractURL
}//class Get
