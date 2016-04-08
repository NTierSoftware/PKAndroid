package org.peacekeeper.tutorial;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.widget.Toast;

public class DownloadTask extends android.os.AsyncTask<String, Integer, Void>{

	private android.app.ProgressDialog progressDialog;
	private String error="";
	private android.content.Context context;
	public DownloadTask(android.content.Context context) {
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		progressDialog= android.app.ProgressDialog.show(context, "Download","Downloading Certificate", false, true, new android.content.DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(android.content.DialogInterface dialog) {
				org.peacekeeper.tutorial.DownloadTask.this.cancel(true);
			}
		});
		super.onPreExecute();
	}

	@Override
	protected Void doInBackground(String... params) {
		error = "";
		try {
			java.net.URL url = null;
			url = new java.net.URL(params[0]);

			java.io.FileOutputStream fileOutput = null;
			java.net.HttpURLConnection urlConnection = null;
			try {
				urlConnection = (java.net.HttpURLConnection) url.openConnection();
				urlConnection.connect();

				fileOutput = context.openFileOutput(url.getFile().substring(url.getFile().lastIndexOf("/")+1), android.content.Context.MODE_PRIVATE);

				java.io.InputStream inputStream = urlConnection.getInputStream();
				int totalSize = urlConnection.getContentLength();
				int downloadedSize = 0;
				byte[] buffer = new byte[1024];
				int bufferLength = 0; 

				while ( (bufferLength = inputStream.read(buffer)) > 0 ) 
				{
					fileOutput.write(buffer, 0, bufferLength);
					downloadedSize += bufferLength;

					int progress=(int)(downloadedSize*100/totalSize);
					publishProgress(progress);
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			} finally {
				if (fileOutput!=null)
					try {
						fileOutput.close();
					} catch (java.io.IOException e) {}
				if (urlConnection!=null)
					urlConnection.disconnect();
			}		
		} catch (Exception e) {
			e.printStackTrace();
			error = "Error: " + e.getLocalizedMessage();

		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		super.onProgressUpdate(values);
		progressDialog.setProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Void v) {
		super.onPostExecute(v);
		progressDialog.dismiss();
		if (!error.equals(""))
			android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show();
		else {
			android.widget.Toast.makeText(context, "Certificate has been downloaded", android.widget.Toast.LENGTH_LONG).show();
		}
	}


}


