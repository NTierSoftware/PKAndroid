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
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.*;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.peacekeeper.service.FetchAddressIntentService;
import org.peacekeeper.util.*;

import org.slf4j.*;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;


public class actRegistration extends AppCompatActivity implements
		ConnectionCallbacks, OnConnectionFailedListener{

/**
 Getting the Location Address.

 Demonstrates how to use the {@link android.location.Geocoder} API and reverse geocoding to
 display a device's location as an address. Uses an IntentService to fetch the location address,
 and a ResultReceiver to process results sent by the IntentService.

 For a starter example that displays the last known location of a device using a longitude and
 latitude,
 see https://github.com/googlesamples/android-play-location/tree/master/BasicLocation.

 For an example that shows location updates using the Fused Location Provider API, see
 https://github.com/googlesamples/android-play-location/tree/master/LocationUpdates.

 This sample uses Google Play services (GoogleApiClient) but does not need to authenticate a user.
 For an example that uses authentication, see
 https://github.com/googlesamples/android-google-accounts/tree/master/QuickStart.
 */

protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
//protected static final String LOCATION_ADDRESS_KEY  = "LOCN";

// Provides the entry point to Google Play services.
protected GoogleApiClient mGoogleApiClient;
//Represents a geographical location.
protected Location mLastLocation;

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
protected TextView mEditHomeLocation;
// Visible while the address is being fetched.
ProgressBar mProgressBar;
// Kicks off the request to fetch an address when pressed.
Button mbuttonGeocode;

static private final Logger mLog = LoggerFactory.getLogger( actRegistration.class );
static private final LoggerContext mLoggerContext =
		(LoggerContext) LoggerFactory.getILoggerFactory();
static private final ContextInitializer mContextInitializer =
		new ContextInitializer( mLoggerContext );
// Receiver registered with this activity to get the response from FetchAddressIntentService.
//private AddressResultReceiver mResultReceiver;
private pkUtility mUtility;
private LatLng mLatLng;
@Override
public void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );
	mLog.trace( "OnCreate:\t" );

	setContentView( R.layout.actregistration );

	//mResultReceiver = new AddressResultReceiver( new Handler() );
	Intent intent = getIntent();

	mEditHomeLocation = (TextView) findViewById( R.id.editHomeLocation );
	mEditHomeLocation.setText( intent.getStringExtra(FetchAddressIntentService.LOCATION) );
	mProgressBar = (ProgressBar) findViewById( R.id.progress_bar );
	mbuttonGeocode = (Button) findViewById( R.id.buttonGeocode );

	updateValuesFromBundle( savedInstanceState );

	updateUIWidgets();
	//buildGoogleApiClient();
	mLatLng =  intent.getParcelableExtra( FetchAddressIntentService.LATLNG );
}

//Runs when user clicks the Fetch Address button. Starts the service to fetch the address if GoogleApiClient is connected.
/*
public void buttonGeocode( View view ){
	// We only start the service to fetch the address if GoogleApiClient is connected.
	if ( mGoogleApiClient.isConnected() && mLastLocation != null ){ startIntentService(); }
	// If GoogleApiClient isn't connected, we process the user's request by setting
	// mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
	// fetch the address. As far as the user is concerned, pressing the Fetch Address button
	// immediately kicks off the process of getting the address.
	mAddressRequested = true;


	startActivity(new Intent(this, MyLocationDemoActivity.class));
	updateUIWidgets();
}
*/

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
// Gets the best and most recent location currently available, which may be null in rare cases when a location is not available.
/*
	mLastLocation = LocationServices.FusedLocationApi.getLastLocation( mGoogleApiClient );
	if ( mLastLocation != null ){
		if ( !Geocoder.isPresent() ){
			Toast.makeText( this, R.string.no_geocoder_available, Toast.LENGTH_LONG ).show();
			return;
		}
		// It is possible that the user presses the button to get the address before the
		// GoogleApiClient object successfully connects. In such a case, mAddressRequested
		// is set to true, but no attempt is made to fetch the address (see
		// buttonGeocode()) . Instead, we start the intent service here if the
		// user has requested an address, since we now have a connection to GoogleApiClient.
		if ( mAddressRequested ){ startIntentService(); }
	}
*/
}//onConnected

@Override public void onConnectionFailed( ConnectionResult result ){
// Refer to the javadoc for ConnectionResult to see what error codes might be returned in onConnectionFailed.
	mLog.error( "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode() );
}//onConnectionFailed

@Override public void onConnectionSuspended( int cause ){
// The connection to Google Play services was lost for some reason. We call connect() to attempt to re-establish the connection.
	mLog.error( "Connection suspended" );
	mGoogleApiClient.connect();
}//onConnectionSuspended

@Override public void onSaveInstanceState( Bundle savedInstanceState ){
	// Save whether the address has been requested.
	savedInstanceState.putBoolean( ADDRESS_REQUESTED_KEY, mAddressRequested );

	// Save the address string.
	savedInstanceState.putString( FetchAddressIntentService.LOCATION, mAddressOutput );
	super.onSaveInstanceState( savedInstanceState );
}

//Updates fields based on data stored in the bundle.
private void updateValuesFromBundle( Bundle savedInstanceState ){
	if ( savedInstanceState != null ){
		// Check savedInstanceState to see if the address was previously requested.
		if ( savedInstanceState.keySet().contains( ADDRESS_REQUESTED_KEY ) ){
			mAddressRequested = savedInstanceState.getBoolean( ADDRESS_REQUESTED_KEY );
		}
		// Check savedInstanceState to see if the location address string was previously found
		// and stored in the Bundle. If it was found, display the address string in the UI.
		if ( savedInstanceState.keySet().contains( FetchAddressIntentService.LOCATION ) ){
			mAddressOutput = savedInstanceState.getString( FetchAddressIntentService.LOCATION );
			displayAddressOutput();
		}
	}
}

//Builds a GoogleApiClient. Uses {@code #addApi} to request the LocationServices API.
protected synchronized void buildGoogleApiClient(){
	mGoogleApiClient = new Builder( this )
			.addConnectionCallbacks( this )
			.addOnConnectionFailedListener( this )
			.addApi( LocationServices.API )
			.build();
}

@Override protected void onStart(){
	super.onStart();
	mLog.trace( "onStart():\t" );
	mUtility = pkUtility.getInstance( this );
	mGoogleApiClient.connect();
}

@Override protected void onStop(){
	mLog.trace( "onStop():\t" );
	mLoggerContext.stop();//flush log
	mGoogleApiClient.disconnect();
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
	//mLog.trace( "onRestart: mGoogleApiClient: " + mGoogleApiClient.isConnected()  );
}//onRestart()



@Override public void onDestroy(){
	super.onDestroy();
	mLog.trace( "onDestroy():\t" );
	mLoggerContext.stop();//flush log
	mUtility.close();
}



//Creates an intent, adds location data to it as an extra, and starts the intent service for fetching an address.
/*
protected void startIntentService(){
	// Create an intent for passing to the intent service responsible for fetching the address.
	Intent intent = new Intent( this, FetchAddressIntentService.class );

	// Pass the result receiver as an extra to the service.
	intent.putExtra( Constants.RECEIVER, mResultReceiver );

	// Pass the location data as an extra to the service.
	intent.putExtra( Constants.LOCATION_DATA_EXTRA, mLastLocation );

	// Start the service. If the service isn't already running, it is instantiated and started
	// (creating a process for it if needed); if it is running then it remains running. The
	// service kills itself automatically once all intents are processed.
	startService( intent );
}
*/

//Updates the address in the UI.
protected void displayAddressOutput(){ mEditHomeLocation.setText( mAddressOutput ); }

// Toggles the visibility of the progress bar. Enables or disables the Fetch Address button.
private void updateUIWidgets(){
	if ( mAddressRequested ){
		mProgressBar.setVisibility( ProgressBar.VISIBLE );
		mbuttonGeocode.setEnabled( false );
	}
	else{
		mProgressBar.setVisibility( ProgressBar.GONE );
		mbuttonGeocode.setEnabled( true );
	}
}


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
protected void showToast( String text ){ Toast.makeText( this, text, Toast.LENGTH_SHORT ).show(); }

// Receiver for data sent from FetchAddressIntentService.
/*
class AddressResultReceiver extends ResultReceiver{
	public AddressResultReceiver( Handler handler ){ super( handler ); }

	//Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
	@Override protected void onReceiveResult( int resultCode, Bundle resultData ){

		// Display the address string or an error message sent from the intent service.
		mAddressOutput = resultData.getString( Constants.RESULT_DATA_KEY );
		displayAddressOutput();

		// Show a toast message if an address was found.
		if ( resultCode == Constants.SUCCESS_RESULT ){ showToast( getString( R.string.address_found ) ); }

		// Reset. Enable the Fetch Address button and stop showing the progress bar.
		mAddressRequested = false;
		updateUIWidgets();
	}//onReceiveResult()
}//class AddressResultReceiver
*/

}//class actRegistration

