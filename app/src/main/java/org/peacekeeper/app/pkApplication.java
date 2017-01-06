package org.peacekeeper.app;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.onesignal.OneSignal;

import org.slf4j.*;

import java.security.Provider;

public class pkApplication extends Application {
//https://rtyley.github.io/spongycastle/
/*
static private final Provider SpongyCastleProvider = new org.spongycastle.jce.provider.BouncyCastleProvider();

static { java.security.Security.insertProviderAt(SpongyCastleProvider, 1); }
*/

static private final Logger				mLog	= LoggerFactory.getLogger( pkApplication.class );


@Override
public void onCreate() {
    super.onCreate();
	//OneSignal.startInit( this ).init();
    //mLog.debug("pkApplication.OnCreate:\t name: " + SpongyCastleProvider.getName() + "\t info: " + SpongyCastleProvider.getInfo());

}

@Override protected void attachBaseContext(Context base ) {
	super.attachBaseContext(base);
	MultiDex.install( this );
}


}//pkApplication
