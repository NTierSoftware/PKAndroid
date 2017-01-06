package udinic.accounts_authenticator_example.authentication;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.view.View;
import android.widget.*;

import org.peacekeeper.app.R;
import org.slf4j.LoggerFactory;


import static udinic.accounts_authenticator_example.authentication.AccountGeneral.sServerAuthenticate;
import static udinic.accounts_authenticator_example.authentication.AuthenticatorActivity.*;


/**
 In charge of the Sign up process. Since it's not an AuthenticatorActivity decendent,
 it returns the result back to the calling activity, which is an AuthenticatorActivity,
 and it return the result back to the Authenticator

 User: udinic
 */
public class SignUpActivity extends Activity{
static private final org.slf4j.Logger mLog = LoggerFactory.getLogger( SignUpActivity.class );

private String mAccountType;

@Override
protected void onCreate( Bundle savedInstanceState ){
	super.onCreate( savedInstanceState );

	mAccountType = getIntent().getStringExtra( ARG_ACCOUNT_TYPE );

	setContentView( R.layout.act_register );

	findViewById( R.id.alreadyMember ).setOnClickListener( new View.OnClickListener(){
		@Override
		public void onClick( View v ){
			setResult( RESULT_CANCELED );
			finish();
		}
	} );
	findViewById( R.id.submit ).setOnClickListener( new View.OnClickListener(){
		@Override
		public void onClick( View v ){ createAccount(); }
	} );
}//onCreate()

private void createAccount(){// Validation!

		String //name = ( (TextView) findViewById( R.id.name ) ).getText().toString().trim()
				accountName = ( (TextView) findViewById( R.id.accountName ) ).getText().toString().trim()
				, accountEmail = ( (TextView) findViewById( R.id.accountEmail  ) ).getText().toString().trim()
			, accountZIP = ( (TextView) findViewById(R.id.ZIP ) ).getText().toString().trim();

	try{
		String authtoken = sServerAuthenticate.userSignUp( this, accountName, accountEmail, accountZIP,
		                                                   AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS );
	}catch ( Exception aE ){
		aE.printStackTrace();
	}

}//createAccount()


@Override public void onBackPressed(){
	setResult( RESULT_CANCELED );
	super.onBackPressed();
}

}//SignUpActivity


/*
private void createAccount(){// Validation!
	new AsyncTask< String, Void, Intent >(){

		String name = ( (TextView) findViewById( R.id.name ) ).getText().toString().trim()
//				, accountName = ( (TextView) findViewById( R.id.accountName ) ).getText().toString().trim()
				, accountEmail = ( (TextView) findViewById( R.id.accountEmail  ) ).getText().toString().trim()
				, accountZIP = ( (TextView) findViewById(R.id.ZIP ) ).getText().toString().trim();

//			, accountPassword = null;
//		, accountPassword = ( (TextView) findViewById(R.id.accountPassword ) ).getText().toString().trim();

		@Override protected Intent doInBackground( String... params ){
			mLog.debug( "udinic > Started authenticating" );

			Bundle data = new Bundle();
			try{
				String authtoken = sServerAuthenticate.userSignUp( name, accountEmail, accountZIP,
				                                                   AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS );

				data.putString( AccountManager.KEY_AUTHTOKEN, authtoken );
				data.putString( AccountManager.KEY_USERDATA, accountEmail );
				data.putString( AccountManager.KEY_ACCOUNT_TYPE, mAccountType );
//				data.putString( PARAM_USER_PASS, accountPassword );
				data.putString( PARAM_USER_PASS, accountZIP );
			}
			catch ( Exception e ){ data.putString( KEY_ERROR_MESSAGE, e.getMessage() ); }

			final Intent res = new Intent().putExtras( data );
			return res;
		}//doInBackground

		@Override protected void onPostExecute( Intent intent ){
			if ( intent.hasExtra( KEY_ERROR_MESSAGE ) ){
				Toast.makeText( getBaseContext(), intent.getStringExtra( KEY_ERROR_MESSAGE ),
				                Toast.LENGTH_LONG ).show();
			}
			else{
				setResult( RESULT_OK, intent );
				finish();
			}
		}//onPostExecute
	}
			.execute();
}//createAccount()
*/
