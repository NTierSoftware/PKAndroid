package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.


import com.android.volley.Request.*;
import com.android.volley.Response.*;
import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.peacekeeper.util.*;

import java.net.*;
import java.util.*;


public class Get{
protected static org.slf4j.Logger mLog;//	= org.slf4j.LoggerFactory.getLogger(Get.class);

//protected  static Contract contract = new Contract();
protected UUID msg_id = UUID.randomUUID();
protected StringRequest stringRequest = null;

//PLACE ALL URL NAMES HERE
public static enum URLGet{
	Test, Status;
}//enum GET

//http://developer.android.com/training/volley/requestqueue.html
public Get(){ mLog = org.slf4j.LoggerFactory.getLogger( getClass() ); }//constructor


public Get( URLGet url ){
	super();
	String urlStr = toURL( url ).toString();

	//http://developer.android.com/training/volley/requestqueue.html
	stringRequest = new StringRequest( Method.GET, urlStr,
	                                   new Listener< String >(){
		                                   @Override
		                                   public void onResponse( String response ){
			                                   mLog.debug( "GET RESPONSE!!:\t" + response );
		                                   }
	                                   },
	                                   new ErrorListener(){
		                                   @Override
		                                   public void onErrorResponse( VolleyError error ){
			                                   mLog.debug( "error RESPONSE!!:\t" + error
					                                   .getLocalizedMessage() );
		                                   }
	                                   } );

	pkUtility.getInstance().getRequestQueue().add( stringRequest );
}

public void submit(){ pkUtility.getInstance().getRequestQueue().add( stringRequest ); }

public < E extends Enum< E > > URL toURL( E URLPostOrGet ){
//	try { return new URL(contract.HTTPS_URL + URLPostOrGet.name() + "/" ); }
	try{
		return new URL(
				"https://192.168.1.242:8181/GaelWebSvcGF4/rest/GAEL/" + URLPostOrGet.name() + "/" );
	}catch ( MalformedURLException e ){ e.printStackTrace(); }
	return null;
}//contractURL
}//class Get
