//http://stackoverflow.com/questions/34582370/how-can-i-show-current-location-on-a-google-map-on-android-marshmallow/34582595#34582595

/**
 Getting the Location Address.

 Demonstrates how to use the {@link android.location.Geocoder} API and reverse geocoding to
 display a device's location as an address. Uses an IntentService to fetch the location address,
 and a ResultReceiver to process results sent by the IntentService.
*/
package org.peacekeeper.app;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.*;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.GoogleMap.*;
import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONObject;
//import org.peacekeeper.rest.LinkedRequest;
//import org.peacekeeper.rest.LinkedRequest.pkURL;
import org.peacekeeper.crypto.SecurityGuard;
import org.peacekeeper.rest.LinkedRequest;
import org.peacekeeper.service.*;
import org.peacekeeper.util.pkUtility;
import org.slf4j.*;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

public class actGeocoder extends AppCompatActivity
		implements OnMapReadyCallback,
		           GoogleApiClient.ConnectionCallbacks,
		           GoogleApiClient.OnConnectionFailedListener,
		           LocationListener,
		           OnMapLongClickListener,
		           OnMarkerClickListener{

//begin static
private static final LoggerContext mLoggerContext =
		(LoggerContext) LoggerFactory.getILoggerFactory();
private static final ContextInitializer mContextInitializer =
		new ContextInitializer( mLoggerContext );
private static final Logger mLog = LoggerFactory.getLogger( actGeocoder.class );

private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
//end static


private GoogleMap mGoogleMap;
private SupportMapFragment mapFrag;
private LocationRequest mLocationRequest;
private GoogleApiClient mGoogleApiClient;
private MarkerOptions mMarkerOptions;
private Marker mMarker;
private AddressResultReceiver mResultReceiver = new AddressResultReceiver( new Handler() );
private RESTResultReceiver mRESTResultReceiver = new RESTResultReceiver( new Handler() );
private pkUtility mUtility;

public String getPeaceKeeperStatus(){
	LinkedRequest linkedRequest = new LinkedRequest( LinkedRequest.pkURL.status, null, null )
	{
		@Override public JSONObject getRequest( final JSONObject response ){
			//mLog.debug( "getPeaceKeeperStatus:\t" + response.toString() );
			return null;
		}

	};

	com.android.volley.Request debugRequest = linkedRequest.submit();
	mLog.debug( debugRequest.toString() );
	mLog.debug( linkedRequest.toString() );
	return linkedRequest.toString();
}

public void newPeaceKeeperStatus(){
	startRESTService( pkRequest.pkURL.status );
}




@Override protected void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );
	mUtility = pkUtility.getInstance( this );
	SecurityGuard.initSecurity();

	newPeaceKeeperStatus();
	setContentView( R.layout.geocoder );

	//getPeaceKeeperStatus();

	getSupportActionBar().setTitle( R.string.RegisterYourLocn );
	buildGoogleApiClient();
	mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById( R.id.geocoder );
	mapFrag.getMapAsync( this );
}//onCreate


@Override public void onResume(){
	super.onResume();
	mGoogleApiClient.connect();
}


@Override protected void onRestart(){
	super.onRestart();
	// Reload Logback log: http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically/3810936#3810936
	mLoggerContext.reset();

	//I prefer autoConfig() over JoranConfigurator.doConfigure() so I don't need to find the file myself.
	try{ mContextInitializer.autoConfig(); }
	catch ( JoranException X ){ X.printStackTrace(); }
}//onRestart()

@Override protected void onStop(){
	mGoogleApiClient.disconnect();
	mLoggerContext.stop();//flush log
	super.onStop();
}

@Override public void onDestroy(){
	mLog.trace( "onDestroy():\t" );
	mLoggerContext.stop();//flush log
	super.onDestroy();
}

@Override public void onRequestPermissionsResult( int requestCode, String permissions[], int[] grantResults ){
	switch ( requestCode ){
	case MY_PERMISSIONS_REQUEST_LOCATION:{
		// If request is cancelled, the result arrays are empty.
		if ( grantResults.length > 0
		     && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ){

			// permission was granted, yay! Do the location-related task you need to do.
			if ( ContextCompat.checkSelfPermission( this,
			                                        Manifest.permission.ACCESS_FINE_LOCATION )
			     == PackageManager.PERMISSION_GRANTED ){

				if ( mGoogleApiClient == null ){ buildGoogleApiClient(); }
				//mGoogleMap.setMyLocationEnabled( true );
			}

		}
			// permission denied. Disable the functionality that depends on this permission.
		else{ Toast.makeText( this, "permission denied", Toast.LENGTH_LONG ).show(); }
		return;
	}

	}//switch
}

protected synchronized void buildGoogleApiClient(){
	mGoogleApiClient = new GoogleApiClient.Builder( this )
			.addConnectionCallbacks( this )
			.addOnConnectionFailedListener( this )
			.addApi( LocationServices.API )
			.build();

	mGoogleApiClient.connect();
}

//http://stackoverflow.com/questions/31328143/android-google-maps-onmapready-store-googlemap
@Override public void onMapReady( GoogleMap googleMap ){
	//Initialize Google Play Services
	if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ){
		if ( ContextCompat.checkSelfPermission( this,
		                                        Manifest.permission.ACCESS_FINE_LOCATION )
		     != PackageManager.PERMISSION_GRANTED ){
			//Location Permission already granted
			checkLocationPermission();
			return;  //Request Location Permission
		}

	}

	mGoogleMap = googleMap;
	//mGoogleMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );

	mGoogleMap.setOnMapLongClickListener( this );
	mGoogleMap.setOnMarkerClickListener(this);
	mGoogleMap.setMyLocationEnabled( true );
	mMarkerOptions = new MarkerOptions()
			.title( "Tap this marker again to register your location" )
			.icon( BitmapDescriptorFactory.defaultMarker( BitmapDescriptorFactory.HUE_MAGENTA) );
}



private void checkLocationPermission(){
	if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION )
	     != PackageManager.PERMISSION_GRANTED ){

		// Should we show an explanation?
		if ( ActivityCompat.shouldShowRequestPermissionRationale( this,
		                                                          Manifest.permission.ACCESS_FINE_LOCATION ) ){

// Show an explanation to the user *asynchronously* -- don't block this thread waiting for the user's response!
// After the user sees the explanation, try again to request the permission.
			new AlertDialog.Builder( this )
					.setTitle( "Location Permission Needed" )
					.setMessage(
							"This app needs the Location permission, please accept to use location functionality" )
					.setPositiveButton( "OK", new DialogInterface.OnClickListener(){
						@Override public void onClick( DialogInterface dialogInterface, int i ){
							//Prompt the user once explanation has been shown
							ActivityCompat.requestPermissions( actGeocoder.this,
							                                   new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
							                                   MY_PERMISSIONS_REQUEST_LOCATION );
						}
					} )
					.create()
					.show();		}
		else{ // No explanation needed, we can request the permission.
			ActivityCompat.requestPermissions( this,
			                                   new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
			                                   MY_PERMISSIONS_REQUEST_LOCATION );
		}
	}
}

@Override public void onConnected( Bundle bundle ){
	mLocationRequest = new LocationRequest()
			.setInterval( 1000 )
			.setFastestInterval( 1000 )
			.setPriority( LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );

	if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION )
	     == PackageManager.PERMISSION_GRANTED ){
					LocationServices.FusedLocationApi.
                     requestLocationUpdates( mGoogleApiClient, mLocationRequest, this );
	}
}


private final static float ZOOM = 18;
@Override public void onLocationChanged( Location location ){//this is called only once on startup.
	//stop location updates since only current location is needed
	LocationServices.FusedLocationApi
			.removeLocationUpdates( mGoogleApiClient, this );

	LatLng latLng = new LatLng( location.getLatitude(), location.getLongitude() );
	mGoogleMap.moveCamera( CameraUpdateFactory.newLatLngZoom( latLng, ZOOM ) );

	onMapLongClick(latLng);
}


@Override public void onMapLongClick( final LatLng latLng ){
	startGeocodeService( latLng );

	if ( mMarker != null ) mMarker.remove();

	mMarkerOptions.position( latLng );
	mMarker = mGoogleMap.addMarker( mMarkerOptions );
}//onMapLongClick

@Override public boolean onMarkerClick( Marker marker) {
	startActivity(
			new Intent(this, actRegistration.class)
					.putExtra( FetchAddressIntentService.LOCATION, marker.getSnippet() )
					.putExtra( FetchAddressIntentService.LATLNG, marker.getPosition() )

	             );
	return true;
}//onMarkerClick


protected void startGeocodeService( final LatLng latLng ){
	// Start the service. If the service isn't already running, it is instantiated and started
	// (creating a process for it if needed); if it is running then it remains running. The
	// service kills itself automatically once all intents are processed.
	startService(
			new Intent( this, FetchAddressIntentService.class )
					.putExtra( FetchAddressIntentService.RECEIVER, mResultReceiver )
					.putExtra( FetchAddressIntentService.LATLNG, latLng )
	            );
}//startGeocodeService()

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



//Receiver for data sent from FetchAddressIntentService.
class AddressResultReceiver extends ResultReceiver{
	public AddressResultReceiver( Handler handler ){ super( handler ); }

	//Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
	@Override protected void onReceiveResult( int resultCode, Bundle resultData ){
		mMarker.setSnippet( resultData.getString( FetchAddressIntentService.LOCATION ) );
		mMarker.showInfoWindow();
	}//onReceiveResult
}//class AddressResultReceiver

//Receiver for data sent from RESTIntentService.
class RESTResultReceiver extends ResultReceiver{
	public RESTResultReceiver( Handler handler ){ super( handler ); }

	//Receives data sent from RESTIntentService and updates the UI in MainActivity.
	@Override protected void onReceiveResult( int resultCode, Bundle resultData ){
		String snippet = resultData.getString( RESTIntentService.JSONResult );
		mLog.debug( "RESTResultReceiver:\t" + snippet );
		//mMarker.setSnippet( snippet );
		//mMarker.showInfoWindow();
	}//onReceiveResult
}//class RESTResultReceiver


@Override public void onConnectionSuspended( int i ){ mLog.info("onConnectionSuspended: " + i  );}
@Override public void onConnectionFailed( ConnectionResult connectionResult ){
	mLog.error( R.string.GoogleApiClientConnFailed + ":\t" + connectionResult.getErrorMessage() );
	Toast.makeText(this, R.string.GoogleApiClientConnFailed, Toast.LENGTH_LONG).show();
}
}//class actGeocoder

