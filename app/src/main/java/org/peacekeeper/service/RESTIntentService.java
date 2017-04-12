//http://afzaln.com/volley/com/android/volley/toolbox/RequestFuture.html
//http://stackoverflow.com/questions/36735682/android-synchronizing-methods-across-processes/36737001#36737001
// http://stackoverflow.com/questions/16904741/can-i-do-a-synchronous-request-with-volley


package org.peacekeeper.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.*;

import com.android.volley.RequestQueue;

import org.json.JSONObject;
import org.peacekeeper.app.R;
import org.peacekeeper.service.pkRequest.pkURL;
import org.peacekeeper.util.*;
import org.slf4j.*;

import java.util.concurrent.*;


/** Asynchronously handles an intent using a worker thread. Receives a ResultReceiver object and a
 location through an intent. Tries to fetch the address for the location using a Geocoder, and
 sends the result to the ResultReceiver. */
public class RESTIntentService extends IntentService{
//begin static
//Intent putextra ID's
static public final String //LATLNG = "LTLN",
		RECEIVER = "RESTIntentServiceRCVR",
		JSONResult = "JSONResult",
		REQUEST = "RESTIntentServiceRequest"
		;
protected final static pkUtility    mUtility      = pkUtility.getInstance();
protected final static RequestQueue mRequestQueue = mUtility.getRequestQueue();
//protected final static Toast mToast = Toast.makeText( mUtility.getBaseContext(), "", Toast.LENGTH_LONG );
private final static  long timeOut = 5;

//end static


//The receiver where results are forwarded from this service.
private ResultReceiver mReceiver;

private static final Logger mLog = LoggerFactory.getLogger( RESTIntentService.class );
//This constructor is required, and calls the super IntentService(String) constructor with the name for a worker thread.
public RESTIntentService(){ super( "RESTIntentService" ); }

/** Tries to get the location address using a Geocoder. If successful, sends an address to a
 result receiver. If unsuccessful, sends an error message instead.
 Note: We define a {@link android.os.ResultReceiver} in * MainActivity to process content
 sent from this service.

 This service calls this method from the default worker thread with the intent that started
 the service. When this method returns, the service automatically stops. */
@Override protected void onHandleIntent( Intent intent ){
	String errorMessage = "";

	mReceiver = intent.getParcelableExtra( RECEIVER );

	if ( mReceiver == null ){// Check if receiver was properly registered.
		mLog.error( "No RESTIntentService receiver received. There is nowhere to send the results." );
		return;
	}



	// Get the pkRequest passed to this service through an extra.
	pkRequest.pkURL URL = pkURL.valueOf( intent.getStringExtra( REQUEST )) ;
	mLog.debug( "RESTIntentService URL: " + URL.toString() );
	// Make sure that the location data was really sent over through an extra. If it wasn't,
	// send an error message and return.
	if ( URL == null ){
		errorMessage = getString( R.string.no_pkRequest_provided );
		mLog.error( errorMessage );
		deliverResultToReceiver( Constants.FAILURE_RESULT, errorMessage );
		return;
	}


	//Request retval = null;
	JSONObject response = null;
//	try{
	pkRequest request = new pkRequest( URL );
	mLog.debug( "onHandleIntent:\n" + request.toString() );

	request.submit();

	try {
		while (!request.mFuture.isDone()) {;}
		response = request.mFuture.get( timeOut, TimeUnit.SECONDS);
		//response = request.mFuture.get(  );
		mLog.debug( "onHandleIntent:\n" + response.toString() );

/*
	} catch (InterruptedException |ExecutionException x) {
		errorMessage = getString( R.string.failed_future_request );
		mLog.error( errorMessage , x );
		x.printStackTrace();
	}
*/

} catch (InterruptedException |ExecutionException |TimeoutException x) {
	errorMessage = getString( R.string.failed_future_request );
	mLog.error( errorMessage , x );
	x.printStackTrace();
}


/*
	}catch ( IllegalArgumentException illegalArgumentException ){
		// Catch invalid values.
		errorMessage = getString( R.string.failed_future_request );
		mLog.error( errorMessage , illegalArgumentException );
	}
*/


		if ( errorMessage.isEmpty() )
			{ deliverResultToReceiver( Constants.SUCCESS_RESULT, response.toString() ); }
		else
			{ deliverResultToReceiver( Constants.FAILURE_RESULT, errorMessage ); }
}//onHandleIntent()

// Sends a resultCode and message to the receiver.
private void deliverResultToReceiver( int resultCode, String message ){
	Bundle bundle = new Bundle();
	bundle.putString( JSONResult, message );
	mReceiver.send( resultCode, bundle );
}
}//class RESTIntentService
