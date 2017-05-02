// http://stackoverflow.com/questions/30549268/android-volley-timeout-exception-when-using-requestfuture-get
//http://afzaln.com/volley/com/android/volley/toolbox/RequestFuture.html
//http://stackoverflow.com/questions/36735682/android-synchronizing-methods-across-processes/36737001#36737001
// http://stackoverflow.com/questions/16904741/can-i-do-a-synchronous-request-with-volley


package org.peacekeeper.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.*;

import com.android.volley.ServerError;

import org.json.*;
import org.peacekeeper.app.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.peacekeeper.service.pkRequest.pkURL;
import org.peacekeeper.util.*;
import org.slf4j.*;

import java.util.IllegalFormatCodePointException;
import java.util.concurrent.*;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;


/**
 Asynchronously handles an intent using a worker thread. Receives a ResultReceiver object and a
 location through an intent. Tries to fetch the address for the location using a Geocoder, and
 sends the result to the ResultReceiver.
 */
public class RESTIntentService extends IntentService{
//begin static
//Intent putextra ID's
static public final String
		RECEIVER    = "RESTIntentServiceRCVR",
		REQUEST     = "RESTIntentServiceRequest",
		JSONRequest = "JSONRequest",
		JSONResult  = "ResultJSON"
;
protected final static pkUtility    mUtility      = pkUtility.getInstance();
//protected final static RequestQueue mRequestQueue = mUtility.getRequestQueue();
//protected final static Toast mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );
private final static   long         TIMEOUT       = 5;


/*
private static final LoggerContext      mLoggerContext      =
		(LoggerContext) LoggerFactory.getILoggerFactory();
private static final ContextInitializer mContextInitializer =
		new ContextInitializer( mLoggerContext );
private static final Logger             mLog     = LoggerFactory.getLogger( RESTIntentService.class );
*/

private final LoggerContext      mLoggerContext      =
		(LoggerContext) LoggerFactory.getILoggerFactory();
private  final ContextInitializer mContextInitializer =
		new ContextInitializer( mLoggerContext );
private  final Logger             mLog     = LoggerFactory.getLogger( RESTIntentService.class );


//end static
//The receiver where results are forwarded from this service.
private ResultReceiver mReceiver;

//This constructor is required, and calls the super IntentService(String) constructor with the name for a worker thread.
public RESTIntentService(){ super( "RESTIntentService" ); }

@Override protected void onHandleIntent( Intent intent ){
	String errorMessage = "";

	mReceiver = intent.getParcelableExtra( RECEIVER );

	if ( mReceiver == null ){// Check if receiver was properly registered.
		mLog.error( "No RESTIntentService receiver received. There is nowhere to send the results." );
		return;
	}


	// Get the pkRequest passed to this service through an extra.
	pkRequest.pkURL URL = pkURL.valueOf( intent.getStringExtra( REQUEST ) );

	JSONObject requestBody = null;
	String aJSONRequest = intent.getStringExtra( JSONRequest );
	if (aJSONRequest != null){
		try{ requestBody = new JSONObject( aJSONRequest );
		}catch ( JSONException aE ){ aE.printStackTrace(); }
	}


	mLog.debug( "RESTIntentService URL: " + URL.toString() );

	// Make sure that the location data was really sent over through an extra. If it wasn't,
	// send an error message and return.
	if ( URL == null ){
		errorMessage = getString( R.string.no_pkRequest_provided );
		mLog.error( errorMessage );
		deliverResultToReceiver( Constants.FAILURE_RESULT, errorMessage );
		return;
	}


	JSONObject response = null;
	//pkRequest request = new pkRequest( URL );
	pkRequest request = new pkRequest( URL, requestBody );

	if (requestBody != null){
		mLog.debug( "requestBody:\t" + requestBody.toString() );
		mUtility.debugToast( "requestBody:\t" + requestBody.toString() );
	}
	request.submit();

	try{
		// TODO THIS BLOCKS the service but not the main UI thread. Consider wrapping in an asynch task:
		// see http://stackoverflow.com/questions/30549268/android-volley-timeout-exception-when-using-requestfuture-get
		while (!request.mFuture.isDone()) ;//mLog.debug( "future not done!" );

		response = request.mFuture.get( TIMEOUT, TimeUnit.SECONDS );
		mLog.debug( "onHandleIntent:\n" + response.toString() );
		if (URL == pkURL.registrations){
			String id = response.getString( "_id" );
			pkURL registrations2 = pkURL.registrations2.addToURL( id );

			requestBody = new JSONObject().put( "receivedCode", "12345678" );

			pkRequest reg2 = new pkRequest( registrations2, requestBody );
			reg2.submit();
			while (!reg2.mFuture.isDone()) ;//mLog.debug( "future not done!" );

			response = request.mFuture.get( TIMEOUT, TimeUnit.SECONDS );
		}

	}catch ( InterruptedException | ExecutionException | TimeoutException  x ){
		mLog.debug( "requestBody:\t" + ((requestBody != null) ? requestBody.toString() :"NULL") );
		errorMessage = x.toString();
		mLog.error( errorMessage, x );
		x.printStackTrace();
	}catch ( JSONException x ){
		mLog.debug( "requestBody:\t" + ((requestBody != null) ? requestBody.toString() :"NULL") );
		errorMessage = x.toString() + ": BAD ID";
		mLog.error( errorMessage, x );
		x.printStackTrace();
	}


	if ( errorMessage.isEmpty() ){
		deliverResultToReceiver( Constants.SUCCESS_RESULT, response.toString() );
	}
	else{ deliverResultToReceiver( Constants.FAILURE_RESULT, errorMessage ); }
}//onHandleIntent()

// Sends a resultCode and message to the receiver.
private void deliverResultToReceiver( int resultCode, String message ){
	Bundle bundle = new Bundle();
	bundle.putString( JSONResult, message );
	mReceiver.send( resultCode, bundle );
}


@Override public void onDestroy() {
	mLog.trace( "onDestroy():\t" );
	mLoggerContext.stop();//flush log
	super.onDestroy();
}

}//class RESTIntentService
