package org.peacekeeper.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.*;

import com.onesignal.OneSignal;

import org.json.JSONObject;
import org.peacekeeper.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

//import org.peacekeeper.crypto.Registrar;


public class MainActivity extends AppCompatActivity{//implements AsyncResponse {
//begin static
static private final Logger mLog = LoggerFactory.getLogger(MainActivity.class);
static private final LoggerContext mLoggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
static private final ContextInitializer mContextInitializer	= new ContextInitializer( mLoggerContext );
//end static

private pkUtility mUtility;

@Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mLog.trace("OnCreate:\t");
    //mUtility = pkUtility.getInstance(this);
    OneSignal.startInit(this)
            .setNotificationOpenedHandler(new pkNotificationOpenedHandler())
            .init();

    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    android.support.design.widget.FloatingActionButton fab = (android.support.design.widget.FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
	    @Override public void onClick(View view) {
		    android.support.design.widget.Snackbar.make(view, "Replace with your own action", android.support.design.widget.Snackbar.LENGTH_LONG)
				    .setAction("Action", null).show();
	    }
    });
}//onCreate

@Override protected void onRestart(){
	super.onRestart();
	// Reload Logback log: http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically/3810936#3810936
	mLoggerContext.reset();

	try{ mContextInitializer.autoConfig(); } //I prefer autoConfig() over JoranConfigurator.doConfigure() so I don't need to find the file myself.
	catch( JoranException X ){ X.printStackTrace(); }
	//setMobileDataEnabled( true );
	//mLog.trace( "onRestart: mGoogleApiClient: " + mGoogleApiClient.isConnected()  );
}//onRestart()

@Override protected void onStart(){
	mLog.trace("onStart():\t");
	super.onStart();
	mUtility = pkUtility.getInstance(this);
	mLog.debug("serialno:\t" + pkUtility.getSystemProperty("ro.serialno"));
	mLog.debug("android.os.Build.SERIAL:\t" + android.os.Build.SERIAL);
	//Registrar registrar = new Registrar(this);
	//registrar.execute();
//	String url = "https://192.168.1.242:8181/GaelWebSvcGF4/rest/GAEL/Status";
	String url = "http://192.168.1.242:8080/GaelWebSvcGF4/rest/GAEL/Status";

	org.peacekeeper.rest.Get myget = new org.peacekeeper.rest.Get(url);
}//onStart()

@Override protected void onStop(){
	mLog.trace("onStop():\t");

	//mGoogleApiClient.disconnect();
	mLoggerContext.stop();//flush log
	super.onStop();
}// onStop()

@Override public void onDestroy(){
	super.onDestroy();
	mLog.trace("onDestroy():\t");
	mLoggerContext.stop();//flush log
	mUtility.close();
}

@Override public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
return true;
}//onCreateOptionsMenu

@Override public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here.

	switch ( item.getItemId() ){
		case R.id.action_settings:
			break;
/*
		case R.id.action_email:
			mGAELUtility.sendEmail();
			break;
		case R.id.action_test:
			Test();
			break;
*/
		case R.id.action_Exit:
			finish();
			//this.onDestroy();
			break;
		default:
	}// switch

return super.onOptionsItemSelected(item);
}//onOptionsItemSelected


// This fires when a notification is opened by tapping on it or one is received while the app is runnning.
private class pkNotificationOpenedHandler implements OneSignal.NotificationOpenedHandler {
    @Override
    public void notificationOpened(String message, JSONObject additionalData, boolean isActive) {
        try {
            if (additionalData != null) {
                if (additionalData.has("actionSelected"))
                    mLog.debug("OneSignalExample", "OneSignal notification button with id " + additionalData.getString("actionSelected") + " pressed");

	            mLog.debug("OneSignalExample", "Full additionalData:\n" + additionalData.toString());
            }
        } catch (Throwable t) { t.printStackTrace(); }
    }
}//pkNotificationOpenedHandler



}//MainActivity


