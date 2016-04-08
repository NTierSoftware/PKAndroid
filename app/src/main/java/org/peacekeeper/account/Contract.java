package org.peacekeeper.account;

import android.accounts.*;
import android.content.*;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.peacekeeper.app.R;
import org.peacekeeper.exception.*;
import org.peacekeeper.util.pkUtility;
import org.slf4j.*;


//public  final class Contract{
public class Contract{
public final String HTTPS_URL;
private static Account mSyncAccount; //public?

private final static String ConnectionPropertiesFname = "Connection.properties";
private static String AUTHORITY // The authority for the sync adapter's content provider.
					  , ACCOUNT_TYPE 	// An account type, in the form of a domain name
					  , ACCOUNT
					  ;

//private static Context mContext;

private static final Logger mLog	= LoggerFactory.getLogger(Contract.class);

private static pkUtility pkUtil;

private static Uri.Builder mUriBuilder = new Uri.Builder().scheme("content");

public Contract() {
//	public Contract(Context context) {
	//mContext = context;
//	pkUtil = pkUtility.getInstance(context);
	pkUtil = pkUtility.getInstance();

	java.util.Properties properties = pkUtil.getPropertiesFromAssets(ConnectionPropertiesFname);
	ACCOUNT =  properties.getProperty( "account" );

//	final android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(pkUtil);
	String ServerIPaddr =  prefs.getString("ServerIPaddr",
			                                      pkUtil.getPropertiesFromAssets(ConnectionPropertiesFname)
					                                      .getProperty("ServerIPaddrDefault"));

	HTTPS_URL = new StringBuilder("https://")
		//.append( pkUtility.getServerIPaddr() )
		.append(ServerIPaddr)
				 //.append( properties.getProperty( "ServerIPaddrDefault" ) )
		.append(":")
		.append( properties.getProperty( "ServerPort" ) )
		.append( properties.getProperty( "ServerRESTroot" ))
		.toString()
	;

	Resources resources = pkUtil.getResources();

	// Populate from res/values/strings.xml
	// The authority for the sync adapter's content provider.
	AUTHORITY    = resources.getString( R.string.AUTHORITY );
	ACCOUNT_TYPE = resources.getString(R.string.accountType);
//TODO
    mSyncAccount = getSyncAccount();
    mUriBuilder.authority(AUTHORITY);
}//constructor

	// Create a new account for the sync adapter. 
	// See: http://developer.android.com/training/sync-adapters/creating-sync-adapter.html#CreateAccountTypeAccount
private Account getSyncAccount() {
	    AccountManager accountManager = // Get an instance of the Android account manager
	            (AccountManager) pkUtil.getSystemService( Context.ACCOUNT_SERVICE );
	    /*
	       See: http://stackoverflow.com/questions/7063280/store-additional-data-in-android-account-manager
	       Add the account and account type, pw, no user data.
	       If successful, return the Account object, otherwise report an error.
	     */
	    
    	Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
		final Account syncAccount = new Account( ACCOUNT, ACCOUNT_TYPE);

    	switch (accounts.length) {
		case 1:
			Account existingAccount = accounts[0];
/******       ALL CONDITIONS SATISFIED! Return correct account. *************/
			if (existingAccount.equals(syncAccount)) 
				return existingAccount; 

			pkException  ACCOUNT_VERIFICATION_ERROR = 
					new pkException(pkErrCode.ACCOUNT_VERIFICATION_ERROR)
								.set("syncAccount", syncAccount)
								.set("existingAccount", existingAccount);
			throw ACCOUNT_VERIFICATION_ERROR;

		case 0: // Create the account type and default account
/*If you don't set android:syncable="true" in your <provider> element in the manifest, then call 	    	
	    	final int SYNCABLE = 1; //syncable	>0 denotes syncable, 0 means not syncable, <0 means unknown */
	    	//ContentResolver.setIsSyncable(newAccount, AUTHORITY, 1);     
		    
		    if (accountManager
		    		.addAccountExplicitly(syncAccount 
		    				//TODO: ISOLATED FOR SECURITY 
										, getDeviceAccountPW()
										, null) )
		    {	mLog.info("Account added:\t" + syncAccount.toString() 
		    		//KLUGE: Account.toString() bombs JSON ??
		    									//.replace("{", ":")
		    									//.replace("}", ":") 
		    									);
		    	return syncAccount;
		    }

		    pkException  ACCOUNT_CREATION_ERROR = 
					new pkException(pkErrCode.ACCOUNT_CREATION_ERROR)
								.set("syncAccount", syncAccount.toString());
			throw ACCOUNT_CREATION_ERROR;
		    		    
		    
		default: //There are too many accounts of the same type to choose from.
		}//switch

		    
		//@SuppressWarnings("boxing")
		pkException TOO_MANY_ACCOUNTS = 
			new pkException(pkErrCode.TOO_MANY_ACCOUNTS)
					.set("numAccounts", accounts.length)
					.set("syncAccount", syncAccount.toString());
		int i = 0;
		for (Account account : accounts) {
			TOO_MANY_ACCOUNTS.set("Account " + i++, account.toString()) ;
		}

		mLog.error("", TOO_MANY_ACCOUNTS);
		throw TOO_MANY_ACCOUNTS;
	}//getSyncAccount


static	public String getAuthority(){ return AUTHORITY;}
	
private String getDeviceAccountPW(){
return pkUtil.getPropertiesFromAssets(ConnectionPropertiesFname)
			  .getProperty("accountpw");
}

	
	//put all the SQLite tablenames that will be inserted/queried/CRUD here:
	public static enum TableNames{ uLocations, yetAnotherSqlliteTablename; }

	public TableNames toTableNames(final Uri uri){
		mLog.debug("uri.getFragment(): " + uri.getFragment());
		try { return TableNames.valueOf(uri.getFragment());  }
		catch (IllegalArgumentException e){
			pkException BAD_GAEL_TABLE_URI = 
					new pkException(pkErrCode.BAD_PK_TABLE_URI).set("uri", uri);
			mLog.error("uri.getFragment(): " + uri.getFragment(), BAD_GAEL_TABLE_URI);
			throw BAD_GAEL_TABLE_URI;
		}
	}


static	public Uri toUri(TableNames table){ return mUriBuilder.fragment(table.name()).build(); }

	
//PLACE ALL URL NAMES HERE	
/*
	public static enum URLGet { Test, Status; }//enum GET

	public static enum URLPost{
		registrations, //Certificate Signature Request
		ACRAException; }


	public <E extends Enum<E>> URL contractURL( E URLPostOrGet){
		try { return new URL(HTTPS_URL + URLPostOrGet.name() ); }
		catch (MalformedURLException e) { e.printStackTrace(); }
	return null;
	}
*/

}//class Contract 
