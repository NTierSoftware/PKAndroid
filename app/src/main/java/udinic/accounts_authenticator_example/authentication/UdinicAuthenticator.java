package udinic.accounts_authenticator_example.authentication;

import android.accounts.*;
import android.content.*;
import android.os.Bundle;
import android.text.TextUtils;

import org.slf4j.*;


import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static udinic.accounts_authenticator_example.authentication.AccountGeneral.*;


/**
 Created with IntelliJ IDEA.
 User: Udini
 Date: 19/03/13
 Time: 18:58
 */
public class UdinicAuthenticator extends AbstractAccountAuthenticator{
static private final Logger mLog = LoggerFactory.getLogger( UdinicAuthenticator.class );

private final Context mContext;

public UdinicAuthenticator( Context context ){
	super( context );

	// I hate you! Google - set mContext as protected!
	this.mContext = context;
}

@Override public Bundle addAccount( AccountAuthenticatorResponse response
		, String accountType
		, String authTokenType
		, String[] requiredFeatures
		, Bundle options )
throws NetworkErrorException{

	mLog.debug( "UdinicAuthenticator addAccount" );

	final Intent intent = new Intent( mContext, AuthenticatorActivity.class )
			.putExtra( AuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType )
			.putExtra( AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType )
			.putExtra( AuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true )
			.putExtra( AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response );

	final Bundle bundle = new Bundle();
	bundle.putParcelable( AccountManager.KEY_INTENT, intent );
	return bundle;
}

/*
http://blog.udinic.com/2013/04/24/write-your-own-android-authenticator/
Gets a stored auth-token for the account type from a previous successful log-in on this device.
If there’s no such thing - the user will be prompted to log-in.
After a successful sign-in, the requesting app will get the long-awaited auth-token.
To do all that, we need to check the AccountManager if there’s an available auth-token by using
AccountManager#peekAuthToken().
If there isn’t we return the same result as for addAccount().
*/

@Override public Bundle getAuthToken( AccountAuthenticatorResponse response
		, Account account
		, String authTokenType
		, Bundle options )
throws NetworkErrorException{

	mLog.debug( "getAuthToken" );

	// If the caller requested an authToken type we don't support, then return an error
	if ( !authTokenType.equals( AccountGeneral.AUTHTOKEN_TYPE_READ_ONLY ) && !authTokenType.equals(
			AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS ) ){
		final Bundle result = new Bundle();
		result.putString( AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType" );
	return result;
	}

// Extract the username and password from the Account Manager, and ask the server for an appropriate AuthToken.
	final AccountManager am = AccountManager.get( mContext );

	String authToken = am.peekAuthToken( account, authTokenType );

	mLog.debug( "peekAuthToken returned:\t" + authToken );

	// Lets give another try to authenticate the user
	if ( TextUtils.isEmpty( authToken ) ){
		final String password = am.getPassword( account );
		if ( password != null ){
			try{
				mLog.debug( "re-authenticating with the existing password" );
				authToken = sServerAuthenticate.userSignIn( account.name, password, authTokenType );
			}catch ( Exception e ){ mLog.error( e.getMessage() ); }
		}
	}

	// If we get an authToken - we return it
	if ( !TextUtils.isEmpty( authToken ) ){
		final Bundle result = new Bundle();
		result.putString( AccountManager.KEY_ACCOUNT_NAME, account.name );
		result.putString( AccountManager.KEY_ACCOUNT_TYPE, account.type );
		result.putString( AccountManager.KEY_AUTHTOKEN, authToken );
	return result;
	}

/*If we get here, then we couldn't access the user's password - so we need to re-prompt them
for their credentials. We do that by creating an intent to display our AuthenticatorActivity.*/

	final Intent intent = new Intent( mContext, AuthenticatorActivity.class );
	intent.putExtra( AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response )
	      .putExtra( AuthenticatorActivity.ARG_ACCOUNT_TYPE, account.type )
	      .putExtra( AuthenticatorActivity.ARG_AUTH_TYPE, authTokenType )
	      .putExtra( AuthenticatorActivity.ARG_ACCOUNT_NAME, account.name );

	final Bundle bundle = new Bundle();
	bundle.putParcelable( AccountManager.KEY_INTENT, intent );
return bundle;
}//getAuthToken()


@Override public String getAuthTokenLabel( String authTokenType ){
	if ( AUTHTOKEN_TYPE_FULL_ACCESS.equals( authTokenType ) )
		return AUTHTOKEN_TYPE_FULL_ACCESS_LABEL;

	if ( AUTHTOKEN_TYPE_READ_ONLY.equals( authTokenType ) )
		return AUTHTOKEN_TYPE_READ_ONLY_LABEL;

return authTokenType + " (Label)";
}

@Override public Bundle hasFeatures( AccountAuthenticatorResponse response, Account account,
                           String[] features ) throws NetworkErrorException{
	final Bundle result = new Bundle();
	result.putBoolean( KEY_BOOLEAN_RESULT, false );
return result;
}

@Override public Bundle editProperties( AccountAuthenticatorResponse response, String accountType ){ return null;}

@Override public Bundle confirmCredentials( AccountAuthenticatorResponse response
								, Account account,
                                  Bundle options )
throws NetworkErrorException{ return null; }

@Override public Bundle updateCredentials( AccountAuthenticatorResponse response
								, Account account
								, String authTokenType
								, Bundle options )
throws NetworkErrorException { return null; }


}//class UdinicAuthenticator
