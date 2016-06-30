package udinic.accounts_authenticator_example.authentication;

import com.belladati.httpclientandroidlib.client.methods.*;
import com.belladati.httpclientandroidlib.impl.client.DefaultHttpClient;
import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.*;

import java.io.*;
import java.net.URLEncoder;


/*
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
*/

/**
 Handles the comminication with Parse.com

 User: udinic
 Date: 3/27/13
 Time: 3:30 AM
 */
public class ParseComServerAuthenticate implements ServerAuthenticate{
private static final Logger mLog	= LoggerFactory.getLogger( ParseComServerAuthenticate.class );

@Override
public String userSignUp( String name, String email, String pass, String authType )throws Exception{

	String url = "https://api.parse.com/1/users";

	DefaultHttpClient httpClient = new DefaultHttpClient();
	HttpPost httpPost = new HttpPost( url );

	httpPost.addHeader( "X-Parse-Application-Id", "XUafJTkPikD5XN5HxciweVuSe12gDgk2tzMltOhr" );
	httpPost.addHeader( "X-Parse-REST-API-Key", "8L9yTQ3M86O4iiucwWb4JS7HkxoSKo7ssJqGChWx" );
	httpPost.addHeader( "Content-Type", "application/json" );

	String user = "{\"username\":\"" + email + "\",\"password\":\"" + pass + "\",\"phone\":\"415-392-0202\"}";
	HttpEntity entity = new StringEntity( user );
	httpPost.setEntity( (com.belladati.httpclientandroidlib.HttpEntity) entity );

	String authtoken = null;
	try{
		CloseableHttpResponse response = httpClient.execute( httpPost );
		String responseString = EntityUtils.toString( (HttpEntity) response.getEntity() );

		if ( response.getStatusLine().getStatusCode() != 201 ){
			ParseComError error = new Gson().fromJson( responseString, ParseComError.class );
			throw new Exception( "Error creating user[" + error.code + "] - " + error.error );
		}

		User createdUser = new Gson().fromJson( responseString, User.class );

		authtoken = createdUser.sessionToken;
	}catch ( IOException e ){ mLog.error( e.getMessage() ); }

return authtoken;
}//userSignUp

@Override
public String userSignIn( String user, String pass, String authType ) throws Exception{

	mLog.debug( "userSignIn" );

	DefaultHttpClient httpClient = new DefaultHttpClient();
	String url = "https://api.parse.com/1/login"
		 , query = null;

	try{
		query = String.format( "%s=%s&%s=%s", "username", URLEncoder.encode( user, "UTF-8" ),
		                       "password", pass );
	}catch ( UnsupportedEncodingException e ){ mLog.error( e.getMessage() );}
	url += "?" + query;

	HttpGet httpGet = new HttpGet( url );

	httpGet.addHeader( "X-Parse-Application-Id", "XUafJTkPikD5XN5HxciweVuSe12gDgk2tzMltOhr" );
	httpGet.addHeader( "X-Parse-REST-API-Key", "8L9yTQ3M86O4iiucwWb4JS7HkxoSKo7ssJqGChWx" );

	HttpParams params = new BasicHttpParams();
	params.setParameter( "username", user )
		  .setParameter( "password", pass );

	httpGet.setParams( (com.belladati.httpclientandroidlib.params.HttpParams) params );
//        httpGet.getParams().setParameter("username", user).setParameter("password", pass);

	String authtoken = null;
	try{
		//HttpResponse response = httpClient.execute(httpGet);
		CloseableHttpResponse response = httpClient.execute( httpGet );
		String responseString = EntityUtils.toString( (HttpEntity) response.getEntity() );
		if ( response.getStatusLine().getStatusCode() != 200 ){
			ParseComError error = new Gson().fromJson( responseString, ParseComError.class );
			throw new Exception( "Error signing-in [" + error.code + "] - " + error.error );
		}

		User loggedUser = new Gson().fromJson( responseString, User.class );
		authtoken = loggedUser.sessionToken;

	}catch ( IOException e ){ mLog.error( e.getMessage() );}

return authtoken;
}//userSignIn()


private class ParseComError implements Serializable{ int code; String error; }

private class User implements Serializable{
	public String sessionToken;

	private String firstName
			, lastName
			, username
			, phone
			, objectId
			, gravatarId
			, avatarUrl;


	public String getFirstName(){ return firstName; }
	public void setFirstName( String firstName ){ this.firstName = firstName; }

	public String getLastName(){ return lastName; }
	public void setLastName( String lastName ){ this.lastName = lastName; }

	public String getUsername(){ return username; }
	public void setUsername( String username ){ this.username = username; }

	public String getPhone(){ return phone; }
	public void setPhone( String phone ){ this.phone = phone; }

	public String getObjectId(){ return objectId; }
	public void setObjectId( String objectId ){ this.objectId = objectId; }

	public String getSessionToken(){ return sessionToken; }
	public void setSessionToken( String sessionToken ){ this.sessionToken = sessionToken; }

	public String getGravatarId(){ return gravatarId; }
	public void setGravatarId( String gravatarId ){ this.gravatarId = gravatarId; }

	public String getAvatarUrl(){ return avatarUrl; }
	public void setAvatarUrl( String avatarUrl ){ this.avatarUrl = avatarUrl; }
}//class User
}//class ParseComServerAuthenticate

