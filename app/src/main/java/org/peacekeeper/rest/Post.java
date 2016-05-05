// Created by John Donaldson, NTier Software Engineering on 4/7/2016.
package org.peacekeeper.rest;

import com.android.volley.Request.*;
import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.json.*;
import org.peacekeeper.crypto.*;
import org.spongycastle.pkcs.*;


public class Post extends Get{
public static enum URLPost{
	registrations, //Certificate Signature Request
	ACRAException;
}//URLPost


public Post( URLPost url ){
	super();

	//http://developer.android.com/training/volley/requestqueue.html
	//String urlStr = toURL(url).toString();


	String urlStr = "https://192.168.1.242:8181/GaelWebSvcGF4/rest/GAEL/";
	switch ( url ){
	case registrations:
		urlStr += getRegistration().toString();
		break;

	case ACRAException:
		break;

	default:
		break;
	}//switch

	final String urlString = urlStr;
	stringRequest = new StringRequest( Method.POST, urlStr,
	                                   new Response.Listener< String >(){
		                                   @Override public void onResponse( String response ){
			                                   mLog.debug(
					                                   "urlStr:\t" + urlString + "\t:Response:\t" + response );
		                                   }
	                                   },

	                                   new Response.ErrorListener(){
		                                   @Override
		                                   public void onErrorResponse( VolleyError error ){
			                                   mLog.debug( "ERROR urlStr:\t" + urlString + error
					                                   .getLocalizedMessage() );
		                                   }
	                                   } );
}//Post

//private JSONArray getRegistration() {
private JSONObject getRegistration(){
	PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
	mLog.debug(SecurityGuard.toPEM(CSR));

	//JSONArray registration = new JSONArray();
	JSONObject rowObject = new JSONObject();
	try{
		rowObject.put( "msg_id", msg_id.toString() );
		//registration.put(rowObject);

//		String CSRstr = Base64.toBase64String( CSR.getEncoded() );
		String CSRstr = SecurityGuard.toPEM( CSR );
		rowObject.put( "CSR", CSRstr );
		//registration.put(rowObject);

		rowObject.put( "referredBy", "IamtheReferrerTest@boosh.com" );
		//registration.put(rowObject);

		rowObject.put( "receivedCode", "" );
		//registration.put(rowObject);

		rowObject.put( "accepted", "false" );
		//registration.put(rowObject);
	}
//	catch (IOException| JSONException X) { X.printStackTrace(); }
	catch ( Exception X ){ X.printStackTrace(); }

	mLog.debug( "\nPOST :\t" + rowObject.toString() );
	//mLog.debug(registration.toString());
//	return registration;
	return rowObject;
}//getRegistration


}//class Post
