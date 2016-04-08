package org.peacekeeper.crypto;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.peacekeeper.util.pkUtility;

/**
 * Created by NTier on 4/3/2016.
 */
public class Registar {

pkUtility pkUtil = pkUtility.getInstance();

private Registar() {
	String url ="http://www.example.com";
	SecurityGuard.initSecurity();
	StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
			                                               new Response.Listener<String>() {
				                                               @Override
				                                               public void onResponse(String response) {
					                                               // Do something with the response
				                                               }
			                                               },
			                                               new Response.ErrorListener() {
				                                               @Override
				                                               public void onErrorResponse(VolleyError error) {
					                                               // Handle error
				                                               }
			                                               });

// Add the request to the RequestQueue.
	//pkUtil.getRequestQueue().  .add(stringRequest);
}
}
