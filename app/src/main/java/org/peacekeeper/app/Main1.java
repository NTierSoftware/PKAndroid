package org.peacekeeper.app;

import android.accounts.*;
import android.app.*;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;

import org.json.*;
import org.peacekeeper.crypto.SecurityGuard;
import org.peacekeeper.rest.LinkedRequest;
import org.peacekeeper.rest.LinkedRequest.pkURL;
import org.peacekeeper.util.pkUtility;
import org.slf4j.LoggerFactory;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import udinic.accounts_authenticator_example.authentication.AccountGeneral;


import static udinic.accounts_authenticator_example.authentication.AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS;


/**
 Created with IntelliJ IDEA.
 User: Udini
 Date: 21/03/13
 Time: 13:50
 */
public class Main1 extends Activity{
//begin static
static private final org.slf4j.Logger mLog = LoggerFactory.getLogger( Main1.class );
static private final LoggerContext mLoggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
static private final ContextInitializer mContextInitializer = new ContextInitializer( mLoggerContext );

private static final String STATE_DIALOG = "state_dialog", STATE_INVALIDATE = "state_invalidate";
private pkUtility mUtility;

//end static
private String accountType;

private AccountManager mAccountManager;
private AlertDialog mAlertDialog;
private boolean mInvalidate;

@Override public void onDestroy(){
	super.onDestroy();
	mLog.trace( "onDestroy():\t" );
	mLoggerContext.stop();//flush log
}

@Override protected void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );
	SecurityGuard.initSecurity();
	mUtility = pkUtility.getInstance( this );

	setContentView( R.layout.main );
	mAccountManager = AccountManager.get( this );

	accountType = getString(R.string.accountType);

	findViewById( R.id.btnAddAccount ).setOnClickListener( new View.OnClickListener(){
		@Override public void onClick( View v ){
			addNewAccount( accountType, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS );
		}
	} );

	findViewById( R.id.btnGetAuthToken ).setOnClickListener( new View.OnClickListener(){
		@Override public void onClick( View v ){
			showAccountPicker( AUTHTOKEN_TYPE_FULL_ACCESS, false );
		}
	} );

	findViewById( R.id.btnGetAuthTokenConvenient ).setOnClickListener( new View.OnClickListener(){
		@Override public void onClick( View v ){
			getTokenForAccountCreateIfNeeded( accountType,
			                                  AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS );
		}
	} );
	findViewById( R.id.btnInvalidateAuthToken ).setOnClickListener( new View.OnClickListener(){
		@Override public void onClick( View v ){
			showAccountPicker( AUTHTOKEN_TYPE_FULL_ACCESS, true );
		}
	} );

	if ( savedInstanceState != null ){
		boolean showDialog = savedInstanceState.getBoolean(
				STATE_DIALOG ), invalidate = savedInstanceState.getBoolean( STATE_INVALIDATE );

		if ( showDialog ) showAccountPicker( AUTHTOKEN_TYPE_FULL_ACCESS, invalidate );
	}

}//onCreate

@Override protected void onRestart(){
	super.onRestart();
	// Reload Logback log: http://stackoverflow.com/questions/3803184/setting-logback-appender-path-programmatically/3810936#3810936
	mLoggerContext.reset();

	try{
		mContextInitializer.autoConfig();
	} //I prefer autoConfig() over JoranConfigurator.doConfigure() so I don't need to find the file myself.
	catch ( JoranException X ){ X.printStackTrace(); }
}//onRestart()

@Override protected void onStart(){
	mLog.trace( "onStart():\t" );
	super.onStart();

	//SecurityGuard.listAlgorithms(null);
	//new Get( URLGet.status ).submit();
	//new Post( URLPost.registrations ).submit();

//chained HTTP requests for registration:
// 1) POST registrations ->  2) PATCH registrations ->  3) GET devices

	LinkedRequest ChainReg3 = new LinkedRequest( pkURL.devices, null )	{
		@Override public JSONObject getRequest( final JSONObject response ){
			SecurityGuard.SetEntries( response );
			return null; }
	} ;

	LinkedRequest ChainReg2 = new LinkedRequest( pkURL.registrations2, ChainReg3  ){
		//LinkedRequest ChainReg2 = new LinkedRequest(pkURL.registrations2, null  ){
		@Override public JSONObject getRequest( final JSONObject response ){
			//mLog.debug("ChainReg2 header:\t" + this.mPkURL.mHeader.toString());
			return getReceivedCode(); }
	};


	LinkedRequest ChainReg1 = new LinkedRequest( pkURL.registrations, getRegistration(), ChainReg2  ){
		@Override public JSONObject getRequest( final JSONObject response ){ return null; }
	};

	ChainReg1.submit();
}//onStart()


private JSONObject getRegistration(){
	PKCS10CertificationRequest CSR = SecurityGuard.genCSR();
	mLog.debug( SecurityGuard.toPEM( CSR ) );

	JSONObject registration = new JSONObject();
	try{
		//boolean accepted = false;
		String CSRstr = Base64.toBase64String( CSR.getEncoded() );

		//registration.put( "_id", msg_id.toString() );
		registration.put( "accepted", false )
		            .put( "csr", CSRstr )
		            .put( "deviceId", mUtility.getUniqDeviceID().toString() )
		            .put( "deviceOSType", "Android" )
		            .put( "deviceOSVersion", mUtility.getVersion().toString() )
		            .put( "keeperId", "" )
		            .put( "receivedCode", "" )
		            .put( "referredBy", "JDtest@boosh.com" );

	}catch ( IOException | JSONException X ){
		mLog.error( X.getMessage() );
		registration = pkUtility.errJSONObject;
	}

	mLog.debug( "\nPOST :\t" + registration.toString() );
	return registration;
}//getRegistration


private JSONObject getReceivedCode(){
	JSONObject ReceivedCode = new JSONObject();
	try{
		ReceivedCode.put( "receivedCode", "12345678" );
	}catch ( JSONException X ){
		mLog.error( X.getMessage() );
		ReceivedCode = pkUtility.errJSONObject;
	}

	mLog.debug( "\nReceivedCode PATCH :\t" + ReceivedCode.toString() );
	return ReceivedCode;
}//getReceivedCode



@Override protected void onStop(){
	mLog.trace( "onStop():\t" );

	mLoggerContext.stop();//flush log
	super.onStop();
}// onStop()

@Override protected void onSaveInstanceState( Bundle outState ){
	super.onSaveInstanceState( outState );
	if ( mAlertDialog != null && mAlertDialog.isShowing() ){
		outState.putBoolean( STATE_DIALOG, true );
		outState.putBoolean( STATE_INVALIDATE, mInvalidate );
	}
}

/**
 Add new account to the account manager

 @param accountType
 @param authTokenType
 */
private void addNewAccount( String accountType, String authTokenType ){
	showMessage( "addNewAccount" );

	AccountManagerCallback< Bundle > acctMgrCallback = new AccountManagerCallback< Bundle >(){
		@Override public void run( AccountManagerFuture< Bundle > future ){
			try{
				Bundle bnd = future.getResult();
				showMessage( "Account was created" );
				mLog.debug( "udinic", "AddNewAccount Bundle is " + bnd );
			}catch ( Exception e ){
				mLog.error( e.getMessage() );
				e.printStackTrace();
				showMessage( e.getMessage() );
			}
		}//run
	};


	final AccountManagerFuture< Bundle > future =
			mAccountManager.addAccount( accountType, authTokenType, null, null, this,
			                            acctMgrCallback, null );
}//addNewAccount

/**
 Show all the accounts registered on the account manager. Request an auth token upon user select.

 @param authTokenType
 */
private void showAccountPicker( final String authTokenType, final boolean invalidate ){
	mInvalidate = invalidate;
	final Account availableAccounts[] = mAccountManager.getAccountsByType( accountType );

	if ( availableAccounts.length == 0 ){
		Toast.makeText( this, "No accounts", Toast.LENGTH_SHORT ).show();
	}
	else{
		String name[] = new String[ availableAccounts.length ];
		for ( int i = 0; i < availableAccounts.length; i++ ){
			name[ i ] = availableAccounts[ i ].name;
		}

		// Account picker
		mAlertDialog = new AlertDialog.Builder( this ).setTitle( "Pick Account" ).setAdapter(
				new ArrayAdapter< String >( getBaseContext(), android.R.layout.simple_list_item_1,
				                            name ), new DialogInterface.OnClickListener(){
					@Override
					public void onClick( DialogInterface dialog, int which ){
						if ( invalidate )
							invalidateAuthToken( availableAccounts[ which ], authTokenType );
						else
							getExistingAccountAuthToken( availableAccounts[ which ],
							                             authTokenType );
					}
				} ).create();
		mAlertDialog.show();
	}
}

/**
 Get the auth token for an existing account on the AccountManager

 @param account
 @param authTokenType
 */
private void getExistingAccountAuthToken( Account account, String authTokenType ){
	final AccountManagerFuture< Bundle > future = mAccountManager.getAuthToken( account,
	                                                                            authTokenType, null,
	                                                                            this, null, null );

	new Thread( new Runnable(){
		@Override
		public void run(){
			try{
				Bundle bnd = future.getResult();

				final String authtoken = bnd.getString( AccountManager.KEY_AUTHTOKEN );
				showMessage( ( authtoken != null ) ? "SUCCESS!\ntoken: " + authtoken : "FAIL" );
				mLog.debug( "udinic", "GetToken Bundle is " + bnd );
			}catch ( Exception e ){
				mLog.error( e.getMessage() );
				e.printStackTrace();
				showMessage( e.getMessage() );
			}
		}
	} ).start();
}

/**
 Invalidates the auth token for the account

 @param account
 @param authTokenType
 */
private void invalidateAuthToken( final Account account, String authTokenType ){
	final AccountManagerFuture< Bundle > future = mAccountManager.getAuthToken( account,
	                                                                            authTokenType, null,
	                                                                            this, null, null );

	new Thread( new Runnable(){
		@Override
		public void run(){
			try{
				Bundle bnd = future.getResult();

				final String authtoken = bnd.getString( AccountManager.KEY_AUTHTOKEN );
				mAccountManager.invalidateAuthToken( account.type, authtoken );
				showMessage( account.name + " invalidated" );
			}catch ( Exception e ){
				mLog.error( e.getMessage() );
				e.printStackTrace();
				showMessage( e.getMessage() );
			}
		}
	} ).start();
}

/**
 Get an auth token for the account.
 If not exist - add it and then return its auth token.
 If one exist - return its auth token.
 If more than one exists - show a picker and return the select account's auth token.

 @param accountType
 @param authTokenType
 */
private void getTokenForAccountCreateIfNeeded( String accountType, String authTokenType ){
	final AccountManagerFuture< Bundle > future = mAccountManager.getAuthTokenByFeatures(
			accountType, authTokenType, null, this, null, null,
			new AccountManagerCallback< Bundle >(){
				@Override
				public void run( AccountManagerFuture< Bundle > future ){
					Bundle bnd = null;
					try{
						bnd = future.getResult();
						final String authtoken = bnd.getString( AccountManager.KEY_AUTHTOKEN );
						showMessage( ( ( authtoken != null ) ? "SUCCESS!\ntoken: " + authtoken
						                                     : "FAIL" ) );
						mLog.debug( "udinic", "GetTokenForAccount Bundle is " + bnd );

					}catch ( Exception e ){
						mLog.error( e.getMessage() );
						e.printStackTrace();
						showMessage( e.getMessage() );
					}
				}
			}
			, null );
}


//private Context mBaseContext = getBaseContext();
private void showMessage( final String msg ){
	if ( TextUtils.isEmpty( msg ) ) return;

	runOnUiThread( new Runnable(){
		@Override public void run(){ Toast.makeText( getBaseContext(), msg, Toast.LENGTH_SHORT ).show(); }
	} );
}//showMessage
}
