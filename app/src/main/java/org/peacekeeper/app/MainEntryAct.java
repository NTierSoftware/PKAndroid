package org.peacekeeper.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

//http://stackoverflow.com/questions/17474793/conditionally-set-first-activity-in-android
//public class MainEntryAct extends AppCompatActivity{
public class MainEntryAct extends Activity{

@Override
public void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );

	// launch a different activity
	Intent launchIntent = new Intent();
	Class< ? > launchActivity;
	try{
		String className = getScreenClassName();
		launchActivity = Class.forName( className );
	}catch ( ClassNotFoundException e ){
		launchActivity = actGeocoder.class;
	}
	launchIntent.setClass( getApplicationContext(), launchActivity );
	startActivity( launchIntent );

	finish();
}

/** return Class name of Activity to show **/
private String getScreenClassName(){
	// NOTE - Place logic here to determine which screen to show next
	// Default is used in this demo code
	String activity = actGeocoder.class.getName();
	return activity;
}

}
