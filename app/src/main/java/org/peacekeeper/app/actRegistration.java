package org.peacekeeper.app;

import android.Manifest.permission;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.*;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.*;

import org.json.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.peacekeeper.service.*;
import org.peacekeeper.util.*;

import org.slf4j.*;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;


public class actRegistration extends AppCompatActivity implements
		ConnectionCallbacks, OnConnectionFailedListener, View.OnFocusChangeListener{


//protected Location mLastLocation;

/**
 Tracks whether the user has requested an address. Becomes true when the user requests an
 address and false when the address (or an error message) is delivered.
 The user requests an address by pressing the Fetch Address button. This may happen
 before GoogleApiClient connects. This activity uses this boolean to keep track of the
 user's intent. If the value is true, the activity tries to fetch the address as soon as
 GoogleApiClient connects.
 */
protected boolean mAddressRequested = false;
//The formatted location address.
protected String mAddressOutput = "";
//Displays the location address.
private EditText mEditName, mEditEmail, mEditHomeLocation;
// Visible while the address is being fetched.
//ProgressBar mProgressBar;

static private final Logger mLog = LoggerFactory.getLogger( actRegistration.class );
static private final LoggerContext mLoggerContext =
		(LoggerContext) LoggerFactory.getILoggerFactory();
static private final ContextInitializer mContextInitializer =
		new ContextInitializer( mLoggerContext );
private pkUtility mUtility;
//private LatLng mLatLng;
private Button mbuttonGeocode, buttonNext;
private actRegistration.RESTResultReceiver mRESTResultReceiver = new actRegistration.RESTResultReceiver( new Handler() );

@Override
public void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );
	mLog.trace( "OnCreate:\t" );

	setContentView( R.layout.actregistration );

	Intent intent = getIntent();

	mEditHomeLocation = (EditText) findViewById( R.id.editHomeLocation );
	mEditHomeLocation.setText( intent.getStringExtra(FetchAddressIntentService.LOCATION) );
	mEditName = (EditText)findViewById(R.id.editName);
	mEditEmail = (EditText)findViewById(R.id.editEmail);

	//mProgressBar = (ProgressBar) findViewById( R.id.progress_bar );

	updateValuesFromBundle( savedInstanceState );

	mEditHomeLocation.setOnFocusChangeListener(this);
	mEditName.setOnFocusChangeListener(this);
	mEditEmail.setOnFocusChangeListener(this);

	buttonNext = (Button) findViewById( R.id.buttonNext );


	//updateUIWidgets();
	//mLatLng =  intent.getParcelableExtra( FetchAddressIntentService.LATLNG );
}

//Runs when a GoogleApiClient object successfully connects.
@Override public void onConnected( Bundle connectionHint ){
	if ( ActivityCompat.checkSelfPermission( this, permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED )
	{// TODO: Consider calling
		//    ActivityCompat#requestPermissions
		// here to request the missing permissions, and then overriding
		//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
		//                                          int[] grantResults)
		// to handle the case where the user grants the permission. See the documentation
		// for ActivityCompat#requestPermissions for more details.
		Toast.makeText( this, R.string.NoLocationPermission, Toast.LENGTH_LONG ).show();
		finish();
	}

	hasGPS();
}//onConnected

@Override public void onConnectionFailed( ConnectionResult result ){
// Refer to the javadoc for ConnectionResult to see what error codes might be returned in onConnectionFailed.
	mLog.error( "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode() );
}//onConnectionFailed

@Override public void onConnectionSuspended( int cause ){
// The connection to Google Play services was lost for some reason. We call connect() to attempt to re-establish the connection.
	mLog.error( "Connection suspended" );
}//onConnectionSuspended

@Override public void onSaveInstanceState( Bundle savedInstanceState ){
	// Save the address string.
	savedInstanceState.putString( FetchAddressIntentService.LOCATION, mAddressOutput );
	super.onSaveInstanceState( savedInstanceState );
}

public void buttonNext( View view ){
//	startRESTService( pkRequest.pkURL.status );
//	mUtility.debugToast( "buttonNext" );

	JSONObject registration = getLocation();
	startService(
			new Intent( this, RESTIntentService.class )
					.putExtra( RESTIntentService.RECEIVER, mRESTResultReceiver )
					.putExtra( RESTIntentService.REQUEST, pkRequest.pkURL.status.name() )
					.putExtra( RESTIntentService.JSONRequest, SecurityGuard.getRegistration( registration ).toString() )
	            );
}

static private JSONObject getLocation(){//BOGUS
//	{ "type": "Point", "coordinates": [ 35.850607,-76.734215 ] }
	JSONObject location = new JSONObject();
	JSONArray coords = new JSONArray();
	try{
		coords.put( 35.850607  )
		      .put( 76.734215  );
		location
				.put("type", "Point")
				.put( "coordinates", coords);

	}catch ( JSONException aE ){ aE.printStackTrace(); }

	return location;
}

/**
 Called when the focus state of a view has changed.
 @param v The view whose state has changed.
 @param hasFocus The new focus state of v.
 */
@Override public void onFocusChange( final View v, final boolean hasFocus ){
	final boolean textsCompleted = !mEditName.getText().toString().isEmpty()
	                         && !mEditEmail.getText().toString().isEmpty()
	                         && !mEditHomeLocation.getText().toString().isEmpty();

	buttonNext.setEnabled( textsCompleted );
	//if ( textsCompleted )buttonNext.setVisibility( View.VISIBLE );
}//onFocusChange

//Updates fields based on data stored in the bundle.
private void updateValuesFromBundle( Bundle savedInstanceState ){
	if ( savedInstanceState != null ){
		// Check savedInstanceState to see if the address was previously requested.
/*		if ( savedInstanceState.keySet().contains( ADDRESS_REQUESTED_KEY ) ){
			mAddressRequested = savedInstanceState.getBoolean( ADDRESS_REQUESTED_KEY );
		}*/
		// Check savedInstanceState to see if the location address string was previously found
		// and stored in the Bundle. If it was found, display the address string in the UI.
		if ( savedInstanceState.keySet().contains( FetchAddressIntentService.LOCATION ) ){
			mAddressOutput = savedInstanceState.getString( FetchAddressIntentService.LOCATION );

			//displayAddressOutput();
			mEditHomeLocation.setText( mAddressOutput );

		}
	}
}

@Override protected void onStart(){
	super.onStart();
	mLog.trace( "onStart():\t" );
	mUtility = pkUtility.getInstance( this );
}

@Override protected void onStop(){
	mLog.trace( "onStop():\t" );
	mLoggerContext.stop();//flush log
	super.onStop();
}


@Override protected void onRestart(){
	super.onRestart();
	// Reload Logback log: http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically/3810936#3810936
	mLoggerContext.reset();

	try{
		mContextInitializer.autoConfig();
	} //I prefer autoConfig() over JoranConfigurator.doConfigure() so I don't need to find the file myself.
	catch ( JoranException X ){ X.printStackTrace(); }
	//setMobileDataEnabled( true );
}//onRestart()



@Override public void onDestroy(){
	super.onDestroy();
	mLog.trace( "onDestroy():\t" );
	mLoggerContext.stop();//flush log
	mUtility.close();
}


// Toggles the visibility of the progress bar. Enables or disables the Fetch Address button.
/*
private void updateUIWidgets(){
	if ( mAddressRequested ){
		mProgressBar.setVisibility( ProgressBar.VISIBLE );
		//mbuttonGeocode.setEnabled( false );
	}
	else{
		mProgressBar.setVisibility( ProgressBar.GONE );
		//mbuttonGeocode.setEnabled( true );
	}
}
*/


public void hasGPS(){
	LocationManager locMgr = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

	if ( !locMgr.isProviderEnabled( LocationManager.GPS_PROVIDER ) ){
		new AlertDialog.Builder( this)
				.setMessage("Please Enable High Accuracy GPS")
				.setCancelable(false)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override public void onClick(final DialogInterface dialog, final int id) {
						startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				})
				.setNegativeButton("Cancel PeaceKeeper", new DialogInterface.OnClickListener() {
					@Override public void onClick(final DialogInterface dialog, final int id) {
						dialog.cancel();
						finish();
					}
				})
				.show();
	}//if
}//hasGPS()


//Shows a toast with the given text.
/*
protected void showToast( String text ){
	Toast.makeText( this, text, Toast.LENGTH_SHORT ).show(); }
*/


protected void startRESTService( final pkRequest.pkURL aURL ){
	// Start the service. If the service isn't already running, it is instantiated and started
	// (creating a process for it if needed); if it is running then it remains running. The
	// service kills itself automatically once all intents are processed.

	startService(
			new Intent( this, RESTIntentService.class )
					.putExtra( RESTIntentService.RECEIVER, mRESTResultReceiver )
					.putExtra( RESTIntentService.REQUEST, aURL.name() )
	            );
}//startRESTService()


//Receiver for data sent from RESTIntentService.
class RESTResultReceiver extends ResultReceiver{
	public RESTResultReceiver( Handler handler ){ super( handler ); }

	//Receives data sent from RESTIntentService and updates the UI in MainActivity.
	@Override protected void onReceiveResult( int resultCode, Bundle resultData ){
		String snippet = resultData.getString( RESTIntentService.JSONResult );
		mUtility.debugToast( "RESTResultReceiver:\t" + snippet );

		//mLog.debug( "RESTResultReceiver:\t" + snippet );
	}//onReceiveResult
}//class RESTResultReceiver


}//class actRegistration

