package org.peacekeeper.app;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.onesignal.*;
import com.onesignal.OneSignal.LOG_LEVEL;

import org.json.JSONObject;
import org.peacekeeper.util.pkUtility;
import org.slf4j.*;

public class pkApplication extends Application{
//https://rtyley.github.io/spongycastle/
/*
static private final Provider SpongyCastleProvider = new org.spongycastle.jce.provider.BouncyCastleProvider();

static { java.security.Security.insertProviderAt(SpongyCastleProvider, 1); }
*/
/*
static private final LoggerContext      mLoggerContext      =
		(LoggerContext) LoggerFactory.getILoggerFactory();
static private final ContextInitializer mContextInitializer =
		new ContextInitializer( mLoggerContext );
*/
static private final Logger mLog = LoggerFactory.getLogger( pkApplication.class );


@Override public void onCreate(){
	super.onCreate();
	mLog.debug( "pkApplication.OnCreate" );
//    mLog.debug("pkApplication.OnCreate:\t name: " + SpongyCastleProvider.getName() + "\t info: " + SpongyCastleProvider.getInfo());

	// Logging set to help debug issues, remove before releasing your app.
	//OneSignal.setLogLevel( OneSignal.LOG_LEVEL.VERBOSE, LOG_LEVEL.FATAL );

/*
	OneSignal.startInit( this )
	         .setNotificationOpenedHandler( new OneSignalNotificationOpenedHandler() )
	         //.setNotificationReceivedHandler( new OneSignalNotificationReceivedHandler() )
	         //.autoPromptLocation( true )
	         .init();
*/
	pkUtility.getInstance( this );//initialize the Utility
	mLog.debug( "pkApplication.OnCreate COMPLETE" );
}

@Override protected void attachBaseContext( Context base ){
	super.attachBaseContext( base );
	MultiDex.install( this );
}


private class OneSignalNotificationOpenedHandler implements OneSignal.NotificationOpenedHandler{
	// This fires when a notification is opened by tapping on it.
	@Override public void notificationOpened( OSNotificationOpenResult result ){
		OSNotificationAction.ActionType actionType = result.action.type;
		JSONObject data = result.notification.payload.additionalData;
		String customKey;

		if ( data != null ){
			customKey = data.optString( "customkey", null );
			if ( customKey != null )
				mLog.debug( "OneSignal", "customkey set with value: " + customKey );
		}

		if ( actionType == OSNotificationAction.ActionType.ActionTaken )
			mLog.debug( "OneSignal", "Button pressed with id: " + result.action.actionID );

		// The following can be used to open an Activity of your choice.

		// Intent intent = new Intent(getApplication(), YourActivity.class);
		// intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
		// startActivity(intent);

		// Add the following to your AndroidManifest.xml to prevent the launching of your main Activity
		//  if you are calling startActivity above.
		 /*
	        <application ...>
              <meta-data android:name="com.onesignal.NotificationOpened.DEFAULT" android:value="DISABLE" />
            </application>
         */
	}
}//class OneSignalNotificationOpenedHandler

/*
private class OneSignalNotificationReceivedHandler implements OneSignal.NotificationReceivedHandler{
	@Override public void notificationReceived( OSNotification notification ){
		mLog.debug( "notification", notification.toJSONObject() );
	}
}//class OneSignalNotificationReceivedHandler
*/


}//pkApplication
