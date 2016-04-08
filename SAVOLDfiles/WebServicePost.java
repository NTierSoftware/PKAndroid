package org.peacekeeper.rest;

import java.io.*;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.peacekeeper.account.Contract.URLPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import org.peacekeeper.account.Contract;
import org.peacekeeper.exception.*;
//TODO: enable gzip content encoding!!!
public class WebServicePost extends WebServiceGet{

private static final Logger mLog = LoggerFactory.getLogger( WebServicePost.class );

public WebServicePost(Contract contract) { super(contract); }

public String post(Contract.URLPost urlPost, JSONArray JSON){

	String result = "Nothing to post. JSONArray is null";
	if (JSON == null) return result;

	HttpsURLConnection urlConnection = null;

	try{
	 URL url = mContract.contractURL( urlPost);

	 urlConnection = (HttpsURLConnection) url.openConnection();

	 urlConnection.setDoOutput(true);
	 urlConnection.setRequestMethod("POST");
	 //urlConnection.setDoOutput(true);
	 urlConnection.setChunkedStreamingMode(0); //0 = default chunk length
	 urlConnection.setRequestProperty("Content-Type","application/json");

	/*		     StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	     StrictMode.setThreadPolicy(policy);
	*/
	 urlConnection.connect();
				/* S E N D   the   J S O N !!! */
	 writeJsonStream(JSON, urlConnection );

	 InputStream in = new BufferedInputStream(urlConnection.getInputStream());
	 result = inputStreamToString(in);
	}//try
	catch (IOException e) {
	   mLog.error(e.getLocalizedMessage());
	   InputStream errIn = new BufferedInputStream(urlConnection.getErrorStream());
	   result = inputStreamToString(errIn);
	}
	finally { if (urlConnection != null) urlConnection.disconnect(); }

return result;
}//post

	
static private void writeJsonStream(final JSONArray JSON, HttpsURLConnection urlConnection ) throws IOException {
	OutputStream outputStream = null,
				 bufferedOutputStream = null;

	OutputStreamWriter writer = null;

	try{
		outputStream = urlConnection.getOutputStream();
		bufferedOutputStream = new BufferedOutputStream(outputStream);
		writer = new OutputStreamWriter(bufferedOutputStream);

		mLog.debug("writeJsonStream.JSON:\t " + JSON.toString() );
		          /* !!! S E N D   the   J S O N !!! */
		new Gson().toJson(JSON, writer );
	}
	catch(Exception X){
		mLog.error(X.getLocalizedMessage());
		throw new pkException(pkErrCode.WEBSERVICE_POST_FAILED).set("exception", X);
	}
	finally{
		if (writer != null) writer.close();
		if (bufferedOutputStream != null) bufferedOutputStream.close();
		if (outputStream != null) outputStream.close();
	}

}//writeJsonStream
}//WebServicePost
