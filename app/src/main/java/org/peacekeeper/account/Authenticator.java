//See http://developer.android.com/training/sync-adapters/creating-authenticator.html
package org.peacekeeper.account;

import android.accounts.*;
import android.content.Context;
import android.os.Bundle;

//Implement AbstractAccountAuthenticator and stub out all  of its methods
public class Authenticator extends AbstractAccountAuthenticator {
    // Simple constructor
    public Authenticator(Context context) { super(context); }
    
    @Override // Editing properties is not supported
    public Bundle editProperties( AccountAuthenticatorResponse r, String s) {
    	throw new UnsupportedOperationException(); }

    
    @Override // Don't add additional accounts
    public Bundle addAccount(
            AccountAuthenticatorResponse r,
            String s,
            String s2,
            String[] strings,
            Bundle bundle)
	throws NetworkErrorException { return null; }
    
    
    @Override // Ignore attempts to confirm credentials
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse r,
            android.accounts.Account account,
            Bundle bundle)
	throws NetworkErrorException { return null;     }

    
    @Override // Getting an authentication token is not supported
    public Bundle getAuthToken(
            AccountAuthenticatorResponse r,
            android.accounts.Account account,
            String s,
            Bundle bundle)
	throws NetworkErrorException { throw new UnsupportedOperationException(); }
    
    
    @Override // Getting a label for the auth token is not supported
    public String getAuthTokenLabel(String s) { throw new UnsupportedOperationException(); }

    
    @Override // Updating user credentials is not supported
    public Bundle updateCredentials(
            AccountAuthenticatorResponse r,
            android.accounts.Account account,
            String s, Bundle bundle)
	throws NetworkErrorException { throw new UnsupportedOperationException(); }

    
    @Override // Checking features for the account is not supported
    public Bundle hasFeatures(
        AccountAuthenticatorResponse r,
        android.accounts.Account account, String[] strings)
	throws NetworkErrorException { throw new UnsupportedOperationException(); }

}//class Authenticator 
