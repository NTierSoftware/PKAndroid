package udinic.accounts_authenticator_example.authentication;

import android.accounts.*;
import android.content.Intent;
import android.os.*;
import android.view.View;
import android.widget.*;

import org.peacekeeper.app.R;
import org.slf4j.*;

import static udinic.accounts_authenticator_example.authentication.AccountGeneral.sServerAuthenticate;

//import udinic.accounts_example.R;


/**
 The Authenticator activity.
 Called by the Authenticator and in charge of identifing the user.
 It sends back to the Authenticator the result.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity{
public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE"
						, ARG_AUTH_TYPE = "AUTH_TYPE"
						, ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
						, ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT"
						, KEY_ERROR_MESSAGE = "ERR_MSG"
						, PARAM_USER_PASS = "USER_PASS";

static private final Logger mLog = LoggerFactory.getLogger( AuthenticatorActivity.class );
static private final int REQ_SIGNUP = 1;

private AccountManager mAccountManager;
private String mAuthTokenType;

@Override public void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );
	setContentView( R.layout.act_login );
	mAccountManager = AccountManager.get( getBaseContext() );

	final Intent intent = getIntent();
	String accountName = intent.getStringExtra( ARG_ACCOUNT_NAME );
	mAuthTokenType = intent.getStringExtra( ARG_AUTH_TYPE );
	if ( mAuthTokenType == null )
		mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS;

	if ( accountName != null ){
		( (TextView) findViewById( R.id.accountName ) ).setText( accountName );
	}

	findViewById( R.id.submit ).setOnClickListener( new View.OnClickListener(){
		@Override public void onClick( View v ){ submit(); }
	} );
	findViewById( R.id.signUp ).setOnClickListener( new View.OnClickListener(){
		@Override public void onClick( View v ){
			// Since there can only be one AuthenticatorActivity, we call the sign up activity, get his results,
			// and return them in setAccountAuthenticatorResult(). See finishLogin().
			Intent signup = new Intent( getBaseContext(), SignUpActivity.class )
							.putExtras( intent.getExtras() );

			startActivityForResult( signup, REQ_SIGNUP );
		}
	} );
}//onCreate

public void submit(){

	final String userName = ( (TextView) findViewById( R.id.accountName ) ).getText()
	                                                                       .toString(),

			userPass = null, //( (TextView) findViewById( R.id.accountPassword ) ).getText().toString(),

			accountType = getIntent().getStringExtra( ARG_ACCOUNT_TYPE );

	new AsyncTask< String, Void, Intent >(){

		@Override protected Intent doInBackground( String... params ){
			mLog.debug( "udinic > Started authenticating" );

			String authtoken;
			Bundle data = new Bundle();
			try{
				authtoken = sServerAuthenticate.userSignIn( userName, userPass, mAuthTokenType );

				data.putString( AccountManager.KEY_ACCOUNT_NAME, userName );
				data.putString( AccountManager.KEY_ACCOUNT_TYPE, accountType );
				data.putString( AccountManager.KEY_AUTHTOKEN, authtoken );
				data.putString( PARAM_USER_PASS, userPass );

			}catch ( Exception e ){
				mLog.error( e.getMessage() );
				data.putString( KEY_ERROR_MESSAGE, e.getMessage() );
			}

			final Intent res = new Intent().putExtras( data );
		return res;
		}//doInBackground

		@Override protected void onPostExecute( Intent intent ){
			if ( intent.hasExtra( KEY_ERROR_MESSAGE ) ){
				Toast.makeText( getBaseContext(), intent.getStringExtra( KEY_ERROR_MESSAGE ),
				                Toast.LENGTH_SHORT ).show();
			}
			else{ finishLogin( intent ); }
		}//onPostExecute
	}.execute();
}//submit

@Override
protected void onActivityResult( int requestCode, int resultCode, Intent data ){
	// The sign up activity returned that the user has successfully created an account
	if ( requestCode == REQ_SIGNUP && resultCode == RESULT_OK ){ finishLogin( data ); }
	else super.onActivityResult( requestCode, resultCode, data );
}

private void finishLogin( Intent intent ){
	mLog.debug( "udinic > finishLogin" );

	String accountName = intent.getStringExtra( AccountManager.KEY_ACCOUNT_NAME )
			, accountPassword = intent.getStringExtra( PARAM_USER_PASS );

	final Account account = new Account( accountName,
	                                     intent.getStringExtra( AccountManager.KEY_ACCOUNT_TYPE ) );

	if ( getIntent().getBooleanExtra( ARG_IS_ADDING_NEW_ACCOUNT, false ) ){
		mLog.debug( "udinic > > finishLogin > addAccountExplicitly" );
		String authtoken = intent.getStringExtra( AccountManager.KEY_AUTHTOKEN )
				, authtokenType = mAuthTokenType;

		// Creating the account on the device and setting the auth token we got
		// (Not setting the auth token will cause another call to the server to authenticate the user)
		mAccountManager.addAccountExplicitly( account, accountPassword, null );
		mAccountManager.setAuthToken( account, authtokenType, authtoken );
	}
	else{
		mLog.debug( "udinic > > finishLogin > setPassword" );
		mAccountManager.setPassword( account, accountPassword );
	}

	setAccountAuthenticatorResult( intent.getExtras() );
	setResult( RESULT_OK, intent );
	finish();
}//finishLogin

}//AuthenticatorActivity

/*
private void finishLogin( Intent intent ){
	mLog.debug( "udinic > finishLogin" );

	String accountName = intent.getStringExtra( AccountManager.KEY_ACCOUNT_NAME )
			, accountPassword = intent.getStringExtra( PARAM_USER_PASS );

	final Account account = new Account( accountName,
	                                     intent.getStringExtra( AccountManager.KEY_ACCOUNT_TYPE ) );

	if ( getIntent().getBooleanExtra( ARG_IS_ADDING_NEW_ACCOUNT, false ) ){
		mLog.debug( "udinic > > finishLogin > addAccountExplicitly" );
		String authtoken = intent.getStringExtra(
				AccountManager.KEY_AUTHTOKEN ), authtokenType = mAuthTokenType;

		// Creating the account on the device and setting the auth token we got
		// (Not setting the auth token will cause another call to the server to authenticate the user)
		mAccountManager.addAccountExplicitly( account, accountPassword, null );
		mAccountManager.setAuthToken( account, authtokenType, authtoken );
	}
	else{
		mLog.debug( "udinic > > finishLogin > setPassword" );
		mAccountManager.setPassword( account, accountPassword );
	}

	setAccountAuthenticatorResult( intent.getExtras() );
	setResult( RESULT_OK, intent );
	finish();
}//finishLogin
*/
