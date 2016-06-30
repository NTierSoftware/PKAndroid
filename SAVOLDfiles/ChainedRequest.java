package org.peacekeeper.rest;// Created by John Donaldson, NTier Software Engineering on 6/29/2016.

import org.json.JSONObject;


public abstract class ChainedRequest<T> {
	int nextMethod;
	String nextURL;
	public ChainedRequest( int aNextMethod, String aNextURL ){
		nextMethod = aNextMethod;
		nextURL = aNextURL;
	}
	abstract public JSONObject nextJSON( T response );
}
