// Created by John Donaldson, NTier Software Engineering on 4/7/2016.
package org.peacekeeper.rest;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public class Put extends Get {
//private static final org.slf4j.Logger mLog	= org.slf4j.LoggerFactory.getLogger(Put.class);

public Put(URLPost url){
	super();

	//http://developer.android.com/training/volley/requestqueue.html
	final String urlStr = toURL(url).toString();

	stringRequest = new StringRequest(Method.PUT, urlStr,
		new Response.Listener<String>(){
			@Override public void onResponse(String response) {
				mLog.debug("urlStr:\t" + urlStr + "\t:Response:\t" + response);
			}
		},

		new Response.ErrorListener(){
		@Override public void onErrorResponse(VolleyError error) {
			mLog.debug("ERROR urlStr:\t" + urlStr + error.getLocalizedMessage() );
		}
	});

	switch (url){
		case registrations:
			break;

		case ACRAException :
			break;

		default:break;
	}//switch

}//Put

public static enum URLPost{
	registrations, //Certificate Signature Request
	ACRAException;
}//URLPost


}
