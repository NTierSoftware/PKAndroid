/**
 * 
 */
package org.peacekeeper.tutorial;


import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import be.kuleuven.cs.msec.x509_tutorial_android.web.DownloadTask;
import be.kuleuven.cs.msec.x509_tutorial_android.web.Util;

/**
 * @author jorn.lapon
 *
 */
public class MainActivity extends android.app.Activity {


	String SERVER_HOST;
	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);



		final android.widget.Spinner txtURL = (android.widget.Spinner)findViewById(R.id.txtURL);

		SERVER_HOST = "www.kuleuven.be";

		
		// Handle 'Connect' Button
		{ 
			final android.webkit.WebView webview = (android.webkit.WebView)findViewById(R.id.webView);
			webview.getSettings().setLoadsImagesAutomatically(true);
			
			android.widget.ImageButton btnConnect = (android.widget.ImageButton)findViewById(R.id.btnConnect);
			btnConnect.setOnClickListener(new android.view.View.OnClickListener() {

				@Override
				public void onClick(android.view.View v) {
					String url = "http://" + SERVER_HOST;
					if (txtURL.getSelectedItem().toString().contains("Https"))
						url = "https://" + SERVER_HOST;

					/**
					 * Fetch Simple Http Page
					 * Since Honeycomb SDK, networking operations are discouraged to run on the main event loop threads.
					 * http://developer.android.com/training/articles/perf-anr.html
					 * The simplest way to create a worker thread for longer operations is to extend the AsyncTask class:
					 */
				
					new TutorialTask(webview, org.peacekeeper.tutorial.MainActivity.this)
							.execute(url);

				}
			});
		}

		//Check for network access:
		if (!Util.isNetworkAvailable(this)){
			android.widget.Toast.makeText(this, "No Network Access", android.widget.Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.download_trust) {
			// Download Certificate Chain Files
			new DownloadTask(this).execute("http://" + SERVER_HOST + "/tutorial/SSL_Server_chain.bks");
		} else if (id == R.id.download_cert) {
			// Download Client KeyStore File
			new DownloadTask(this).execute("http://" + SERVER_HOST + "/tutorial/SSL_Client_B.p12");
		}
		return super.onOptionsItemSelected(item);
	}
}
