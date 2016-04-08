package org.peacekeeper.exception;

import org.peacekeeper.app.R;
import org.peacekeeper.util.pkUtility;

import java.util.Properties;
//See file pkErrCodes.properties
public enum pkErrCode
//To preserve errcode numbers, you should add to the bottom and NOT from the top!
{   NULL_LOCATION
	, SYNC_FAILED
	, WEBSERVICE_POST_FAILED
	, BAD_PK_TABLE_URI
	, INSERT_FAILED
	, TOO_MANY_ACCOUNTS
	, ACCOUNT_CREATION_ERROR
	, ACCOUNT_VERIFICATION_ERROR
	, MISSING_DDL
	, ASSETFILE_NOT_FOUND
	, BAD_SQLLITE_PRAGMA
    , CRYPTO
	, INITIALIZATION_NEEDED //The programmer failed to initialize pkUtility()with pkUtility.getInstance(context);
	;

public final String mErrMsg;
private final String name = name();

private static final int errNumIncrement = 9000;
public final int errNum = errNumIncrement + ordinal();


pkErrCode() {
    if (name == "INITIALIZATION_NEEDED") {
        mErrMsg = "The programmer did not init pkUtility with pkUtility.getInstance(context)";
        return;
    }

    pkUtility pkUtil = pkUtility.getInstance();
    Properties properties = pkUtil.getPropertiesFromAssets("pkErrCodes.properties");

    mErrMsg  =  new StringBuilder( pkUtil.getResources().getString(R.string.app_name) )
            .append(" err: ")
            .append(properties.getProperty(name))
            .toString();
}//cstr private pkErrCode()


	
private String toString;
@Override
public String toString() {
    if (toString == null)
        toString = new StringBuilder()
                .append(" errNum: ")
                .append(errNum)
                .append(": ")
                .append(name)
                .append(": ")
                .append(mErrMsg)
                .toString();

    return toString;
}

}//enum pkErrCode

