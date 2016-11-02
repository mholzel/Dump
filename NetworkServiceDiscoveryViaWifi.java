package com.example.wificoms;

import android.app.Activity;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.DiscoveryListener;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdManager.ResolveListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * This {@link Activity} demonstrates Network Service Discovery capabilities
 * over wifi. This {@link Activity} simply demonstrates how you would register a
 * service you are providing, as well as how you would discover a service that a
 * peer device is providing. No connections are established, and this Activity
 * simply shows you what is available, along with the detailed lifecycle in a
 * {@link TextView}.
 *
 * <p/>
 *
 * Note that the discovery of services with wifi may be slow and could take 5 or
 * 10 seconds. If you still aren't finding services, make sure that this app is
 * open on two devices, and try to re-force the discovery by pushing the
 * {@link Button} (although this should not be necessary).
 */
public class NetworkServiceDiscoveryViaWifi extends Activity {

	/**
	 * ---------------------------------------------
	 *
	 * Private Fields
	 *
	 * ---------------------------------------------
	 */
	private static final String		TAG						= NetworkServiceDiscoveryViaWifi.class.getSimpleName();
	private TextView				logView					= null;
	private static final int		logViewID				= View.generateViewId();
	private String					backlog					= "";
	private static final String		originalServiceName		= TAG;
	private String					serviceName				= originalServiceName;
	private static final String		serviceType				= "_http._tcp.";

	private NsdManager				nsdManager				= null;
	private RegistrationListener	registrationListener	= null;
	private DiscoveryListener		discoveryListener		= null;

	/**
	 * ---------------------------------------------
	 *
	 * {@link Activity} Methods
	 *
	 * ---------------------------------------------
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		logd("onCreate");
		super.onCreate(savedInstanceState);

		/** Create the main LinearLayout */
		LinearLayout linearLayout = new LinearLayout(this);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		/** Add a Button to force registration */
		Button registerButton = new Button(this);
		registerButton.setText("Register");
		registerButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				registerService();
			}
		});
		linearLayout.addView(registerButton, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));

		/** Add a Button to re-force discovery */
		Button discoverButton = new Button(this);
		discoverButton.setText("Discover");
		discoverButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				startServiceDiscovery();
			}
		});
		linearLayout.addView(discoverButton, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));

		/** Add a button to clear the logView */
		Button clearButton = new Button(this);
		clearButton.setText("Clear");
		clearButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						logView.setText("");
					}
				});
			}
		});
		linearLayout.addView(clearButton, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));

		/** Create a ScrollView that will hold the textView of the log */
		ScrollView logScrollView = new ScrollView(this);
		linearLayout.addView(logScrollView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));

		/** Create the TextView that will hold everything written to the log */
		logView = new TextView(this);
		logView.setId(logViewID);
		logScrollView.addView(logView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		/** Set the view for this activity */
		setContentView(linearLayout);
	}

	@Override
	protected void onResume() {
		logd("onResume");
		registerService();
		startServiceDiscovery();
		super.onResume();
	}

	@Override
	protected void onPause() {
		logd("onPause");
		stopServiceDiscovery();
		unregisterService();
		setNsdManager(null);
		super.onPause();
	}

	/**
	 * ---------------------------------------------
	 *
	 * Getters
	 *
	 * ---------------------------------------------
	 */
	private NsdManager getNsdManager() {
		if (nsdManager == null) {
			setNsdManager((NsdManager) getSystemService(Context.NSD_SERVICE));
		}
		return nsdManager;
	}

	private RegistrationListener getRegistrationListener() {
		if (registrationListener == null) {
			setRegistrationListener(new RegistrationListener() {

				@Override
				public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
					/**
					 * Save the service name since Android may have changed it
					 * in order to resolve a conflict.
					 */
					serviceName = nsdServiceInfo.getServiceName();
					logd("onServiceRegistered: NsdServiceInfo: " + nsdServiceInfo.toString());
				}

				@Override
				public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
					logd("onRegistrationFailed: NsdServiceInfo: " + nsdServiceInfo.toString() + ", errorCode: "
							+ errorCode);
				}

				@Override
				public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
					logd("onServiceUnregistered: NsdServiceInfo: " + nsdServiceInfo.toString());
				}

				@Override
				public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
					logd("onUnregistrationFailed: NsdServiceInfo: " + nsdServiceInfo.toString() + ", errorCode: "
							+ errorCode);
				}
			});
		}
		return registrationListener;
	}

	/**
	 * ---------------------------------------------
	 *
	 * Setters
	 *
	 * ---------------------------------------------
	 */
	private void setNsdManager(NsdManager nsdManager) {
		this.nsdManager = nsdManager;
	}

	private void setRegistrationListener(RegistrationListener registrationListener) {
		this.registrationListener = registrationListener;
	}

	/**
	 * ---------------------------------------------
	 *
	 * Other Methods
	 *
	 * ---------------------------------------------
	 */
	/** Register this device so that other peers can discover us. */
	private void registerService() {

		/**
		 * Service information. Pass it an instance name, service type
		 * _protocol._transportlayer , and the map containing information other
		 * devices will want once they connect to this one.
		 */
		NsdServiceInfo serviceInfo = new NsdServiceInfo();
		serviceInfo.setServiceName(TAG + Math.random());
		serviceInfo.setServiceType(serviceType);
		serviceInfo.setPort(8080);
		serviceInfo.setAttribute("DeviceID", Secure.getString(getContentResolver(), Secure.ANDROID_ID));

		unregisterService();
		getNsdManager().registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, getRegistrationListener());
	}

	/** Unregister this service. */
	private void unregisterService() {
		if (registrationListener != null) {
			getNsdManager().unregisterService(getRegistrationListener());
			setRegistrationListener(null);
		}
	}

	/** Start looking for peer services. */
	private void startServiceDiscovery() {

		// stopServiceDiscovery();
		discoveryListener = new DiscoveryListener() {
			@Override
			public void onDiscoveryStarted(String regType) {
				logd("onDiscoveryStarted: regType: " + regType);
			}

			@Override
			public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
				if (!nsdServiceInfo.getServiceType().equals(serviceType)) {
					logd("onServiceFound: Unknown Service Type: " + nsdServiceInfo.getServiceType());
				} else if (nsdServiceInfo.getServiceName().equals(serviceName)) {
					logd("onServiceFound: Same machine: " + nsdServiceInfo.getServiceName());
				} else if (nsdServiceInfo.getServiceName().contains(originalServiceName)) {
					logd("onServiceFound: Service from peer. NsdServiceInfo: " + nsdServiceInfo.toString());
					getNsdManager().resolveService(nsdServiceInfo, new ResolveListener() {

						@Override
						public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
							logd("onServiceResolved: NsdServiceInfo: " + nsdServiceInfo.toString());
						}

						@Override
						public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int errorCode) {
							logd("onResolveFailed: NsdServiceInfo: " + nsdServiceInfo.toString() + ", errorCode: "
									+ errorCode);
						}
					});
				}
			}

			@Override
			public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
				logd("onDiscoveryStopped: nsdServiceInfo: " + nsdServiceInfo.toString());
			}

			@Override
			public void onDiscoveryStopped(String serviceType) {
				logd("onDiscoveryStopped: serviceType: " + serviceType);
			}

			@Override
			public void onStartDiscoveryFailed(String serviceType, int errorCode) {
				logd("onStartDiscoveryFailed: serviceType: " + serviceType + ", errorCode: " + errorCode);
				getNsdManager().stopServiceDiscovery(this);
			}

			@Override
			public void onStopDiscoveryFailed(String serviceType, int errorCode) {
				logd("onStopDiscoveryFailed: serviceType: " + serviceType + ", errorCode: " + errorCode);
				getNsdManager().stopServiceDiscovery(this);
			}
		};
		getNsdManager().discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
	}

	/** Stop looking for peer services. */
	private void stopServiceDiscovery() {
		if (discoveryListener != null) {
			getNsdManager().stopServiceDiscovery(discoveryListener);
			discoveryListener = null;
		}
	}

	/**
	 * Write the specified String to the log, and show it on the
	 * {@link TextView} that we created. If the {@link TextView} was not yet
	 * initialized, add it to a backlog of Strings to be written once the
	 * {@link TextView} is created.
	 */
	private void logd(String loggable) {
		Log.d(TAG, loggable);
		if (logView != null) {
			if (!backlog.isEmpty()) {
				loggable = append(backlog, loggable);
				backlog = "";
			}
			final String toLog = loggable;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					logView.setText(append(logView.getText().toString(), toLog));
				}
			});
		} else {
			backlog = append(backlog, loggable);
		}
	}

	private String append(String a, String b) {
		return a + "\n\n" + b;
	}
}