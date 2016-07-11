package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 7/11/2016.
//http://stackoverflow.com/questions/19837820/volley-jsonobjectrequest-post-request-not-working


import com.android.volley.*;
import com.android.volley.Response.*;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class CustomRequest extends Request<JSONObject> {

private Listener<JSONObject> listener;
private Map<String, String> params;

public CustomRequest(String url, Map<String, String> params,
                     Listener<JSONObject> reponseListener, ErrorListener errorListener) {
	super(Method.GET, url, errorListener);
	this.listener = reponseListener;
	this.params = params;
}

public CustomRequest(int method, String url, Map<String, String> params,
                     Listener<JSONObject> reponseListener, ErrorListener errorListener) {
	super(method, url, errorListener);
	this.listener = reponseListener;
	this.params = params;
}

protected Map<String, String> getParams() throws com.android.volley.AuthFailureError
{ return params; };

@Override
protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
	try {
		String jsonString = new String(response.data,
		                               HttpHeaderParser.parseCharset(response.headers));
		return Response.success(new JSONObject(jsonString),
		                        HttpHeaderParser.parseCacheHeaders(response));
	} catch (UnsupportedEncodingException e) {
		return Response.error(new ParseError(e));
	} catch (JSONException je) {
		return Response.error(new ParseError(je));
	}
}


@Override
protected Response<JSONObject> xparseNetworkResponse(NetworkResponse response) {
	try {
		String jsonString = new String(response.data,
		                               HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
		return Response.success(new JSONObject(jsonString),
		                        HttpHeaderParser.parseCacheHeaders(response));
	} catch (UnsupportedEncodingException e) {
		return Response.error(new ParseError(e));
	} catch (JSONException je) {
		return Response.error(new ParseError(je));
	}
}

@Override
protected void deliverResponse(JSONObject response) {
	// TODO Auto-generated method stub
	listener.onResponse(response);
}
}
