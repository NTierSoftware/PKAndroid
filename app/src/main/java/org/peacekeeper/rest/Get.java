package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 4/3/2016.


import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;


import org.peacekeeper.account.Contract;
import org.peacekeeper.util.pkUtility;

import java.net.MalformedURLException;
import java.net.URL;

public class Get {
protected static org.slf4j.Logger mLog;//	= org.slf4j.LoggerFactory.getLogger(Get.class);

protected  static Contract contract = new Contract();

protected StringRequest stringRequest = null;
//http://developer.android.com/training/volley/requestqueue.html
public Get(){ mLog	= org.slf4j.LoggerFactory.getLogger(getClass()); }//constructor

//public Get(URLGet url){
public Get(String url){
	super();

	//http://developer.android.com/training/volley/requestqueue.html
	stringRequest = new StringRequest(Method.GET, url,
                                   new Listener<String>() {
                                       @Override
                                       public void onResponse(String response) {
                                           mLog.debug("RESPONSE!!:\t"+ response);
                                       }
                                   },
                                   new ErrorListener() {
                                       @Override
                                       public void onErrorResponse(VolleyError error) {
	                                       mLog.debug("error RESPONSE!!:\t" + error.getLocalizedMessage() );
                                       }
                                   });

	pkUtility.getInstance().getRequestQueue().add(stringRequest);
}


public void run(){ pkUtility.getInstance().getRequestQueue().add(stringRequest); }

//PLACE ALL URL NAMES HERE
public static enum URLGet { Test, Status; }//enum GET

public <E extends Enum<E>> URL toURL( E URLPostOrGet){
	try { return new URL(contract.HTTPS_URL + URLPostOrGet.name() ); }
	catch (MalformedURLException e) { e.printStackTrace(); }
	return null;
}//contractURL
}//class Get
