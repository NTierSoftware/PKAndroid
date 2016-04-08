package org.peacekeeper.tutorial;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509ExtendedKeyManager;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;

/**
 * Simple activity based test that exercises the KeyChain API
 */

public class KeyChainManager extends javax.net.ssl.X509ExtendedKeyManager {

	private android.app.Activity activity;
	private final Object aliasLock = new Object();
	private String alias = null;

	public KeyChainManager(android.app.Activity activity){
		this.activity = activity;
	}
	@Override public String chooseClientAlias(String[] keyTypes,
			java.security.Principal[] issuers,
			java.net.Socket socket) {
		android.content.SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(activity);

		if (prefs.getString("DefaultAlias", "").equals("")){
			android.security.KeyChain.choosePrivateKeyAlias(activity, new org.peacekeeper.tutorial.KeyChainManager.AliasResponse(),keyTypes, issuers, socket.getInetAddress().getHostName(), socket.getPort(), "My Test Certificate");
			String a = null;
			synchronized (aliasLock) {
				while (alias == null) {
					try {
						aliasLock.wait();
					} catch (InterruptedException ignored) {
					}
				}
				a = alias;
			}
			prefs.edit().putString("DefaultAlias", a).commit();
		}
		
		return prefs.getString("DefaultAlias", "");
	}
	@Override public String chooseServerAlias(String keyType,
			java.security.Principal[] issuers,
			java.net.Socket socket) {
		// not a client SSLSocket callback
		throw new UnsupportedOperationException();
	}
	@Override public java.security.cert.X509Certificate[] getCertificateChain(String alias) {
		try {
			java.security.cert.X509Certificate[] certificateChain = android.security.KeyChain.getCertificateChain(activity, alias);
			return certificateChain;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (android.security.KeyChainException e) {
			throw new RuntimeException(e);
		}
	}
	@Override public String[] getClientAliases(String keyType, java.security.Principal[] issuers) {
		// not a client SSLSocket callback
		throw new UnsupportedOperationException();
	}
	@Override public String[] getServerAliases(String keyType, java.security.Principal[] issuers) {
		// not a client SSLSocket callback
		throw new UnsupportedOperationException();
	}
	@Override public java.security.PrivateKey getPrivateKey(String alias) {
		try {
			java.security.PrivateKey privateKey = android.security.KeyChain.getPrivateKey(activity, alias);
			return privateKey;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (android.security.KeyChainException e) {
			throw new RuntimeException(e);
		}
	}

	private class AliasResponse implements android.security.KeyChainAliasCallback {
		@Override public void alias(String alias) {
			if (alias == null) {
				return;
			}
			synchronized (aliasLock) {
				org.peacekeeper.tutorial.KeyChainManager.this.alias = alias;
				aliasLock.notifyAll();
			}
		}
	}
}


