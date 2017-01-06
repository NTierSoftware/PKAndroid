package org.peacekeeper.rest;
import android.os.*;

import com.android.volley.*;
import com.android.volley.toolbox.StringRequest;

import java.util.LinkedList;


public class SyncRequestLoader implements Response.ErrorListener, Response.Listener{
//public final String TAG = "SyncRequestLoader";

private RequestQueue mRequestQueue;

private Response.Listener mListener;
private Response.ErrorListener mErrorListener;

private LinkedList< StringRequest > mSyncRequests;

private Handler.Callback mCallback;

public SyncRequestLoader( RequestQueue mRequestQueue ){
	this.mRequestQueue = mRequestQueue;
	mSyncRequests = new LinkedList<>();
}

synchronized public void add( StringRequest request ){
	mSyncRequests.add( request );

	if ( mSyncRequests.size() == 1 ) transceive();
}

@Override
synchronized public void onResponse( Object response ){
	mListener.onResponse( response );

	//do anything here if u want

	removeCompletedRequest();

	continueIfPossible();
}

@Override
synchronized public void onErrorResponse( VolleyError error ){
	mErrorListener.onErrorResponse( error );

	//do anything here if u want

	removeCompletedRequest();

	continueIfPossible();
}

public boolean isOnCallback(){ return ( mCallback != null ); }

public void setCallback( Handler.Callback callback ){ this.mCallback = callback; }

synchronized private void transceive(){
	StringRequest request = mSyncRequests.getFirst();
	//mListener = request.getListener();
	mErrorListener = request.getErrorListener();

	StringRequest new_request = new StringRequest( request.getUrl(), this, this );

	mRequestQueue.add( new_request );
}

synchronized private void removeCompletedRequest(){ mSyncRequests.removeFirst(); }

synchronized private void continueIfPossible(){
	if ( mSyncRequests.size() > 0 ) transceive();

	else if ( isOnCallback() ) mCallback.handleMessage( Message.obtain( null, 1 ) );
}

}//class SyncRequestLoader
