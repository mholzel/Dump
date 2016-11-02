package com.example.wificoms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.WifiP2pManager.ServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.UpnpServiceResponseListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceRequest;
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
 * enabled by Wifi Direct. This {@link Activity} simply demonstrates how you
 * would register a service you are providing, as well as how you would discover
 * a service that a peer device is providing. No connections are established,
 * and this Activity simply shows you what is available, along with the detailed
 * lifecycle in a {@link TextView}.
 *
 * <p/>
 *
 * Note that the discovery of services with Wifi Direct seems to be slow and
 * could take 5 or 10 seconds. If you still aren't finding services, make sure
 * that this app is open on two devices, and try to re-force the discovery by
 * pushing the {@link Button} (although this should not be necessary).
 *
 * <p/>
 *
 * Finally, note that there are other (probably better) mechanisms for
 * establishing a Wifi Direct connection with a peer. This is primarily intended
 * just to see services that are available, not just peers that are available to
 * connect to.
 */
public class NetworkServiceDiscoveryViaWifiDirect extends Activity {

	/**
	 * ---------------------------------------------
	 *
	 * Private Fields
	 *
	 * ---------------------------------------------
	 */
	private static final String	TAG					= NetworkServiceDiscoveryViaWifiDirect.class.getSimpleName();
	private TextView			logView				= null;
	private static final int	logViewID			= View.generateViewId();
	private String				backlog				= "";
	private static final String	originalServiceName	= TAG;
	private String				serviceName			= originalServiceName;
	private static final String	serviceType			= "_presence._tcp";

	private WifiP2pManager		wifiP2pManager		= null;
	private Channel				channel				= null;

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
		setWifiP2pManager(null);
		super.onPause();
	}

	/**
	 * ---------------------------------------------
	 *
	 * Getters
	 *
	 * ---------------------------------------------
	 */
	/**
	 * Get the {@link WifiP2pManager}, creating a new instance if we don't have
	 * one yet.
	 */
	private WifiP2pManager getWifiP2pManager() {
		if (wifiP2pManager == null) {
			setWifiP2pManager((WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE));
		}
		return wifiP2pManager;
	}

	/**
	 * Get the {@link Channel}, creating a new instance if we don't have one
	 * yet.
	 */
	private Channel getChannel() {
		if (channel == null) {
			setChannel(getWifiP2pManager().initialize(getApplicationContext(), getMainLooper(), null));
		}
		return channel;
	}

	/**
	 * ---------------------------------------------
	 *
	 * Setters
	 *
	 * ---------------------------------------------
	 */
	private void setWifiP2pManager(WifiP2pManager wifiP2pManager) {
		this.wifiP2pManager = wifiP2pManager;
	}

	private void setChannel(Channel channel) {
		this.channel = channel;
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

		unregisterService();

		/** Create a string map containing information about your service. */
		Map<String, String> record = new HashMap<String, String>();
		record.put("DeviceID", Secure.getString(getContentResolver(), Secure.ANDROID_ID));

		/**
		 * Service information. Pass it an instance name, service type
		 * _protocol._transportlayer , and the map containing information other
		 * devices will want once they connect to this one.
		 */
		WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(serviceName, serviceType, record);

		/**
		 * Add the local service, sending the service info, network channel, and
		 * listener that will be used to indicate success or failure of the
		 * request.
		 */
		getWifiP2pManager().addLocalService(getChannel(), serviceInfo, new ActionListener() {
			@Override
			public void onSuccess() {
				logd("addLocalService.onSuccess");
			}

			@Override
			public void onFailure(int arg0) {
				logd("addLocalService.onFailure: " + arg0);
			}
		});
	}

	/** Unregister this service. */
	private void unregisterService() {
		getWifiP2pManager().clearLocalServices(getChannel(), null);
	}

	/** Start looking for peer services. */
	private void startServiceDiscovery() {

		// stopServiceDiscovery();

		/** Setup listeners for vendor-specific services. */
		getWifiP2pManager().setServiceResponseListener(getChannel(), new ServiceResponseListener() {

			@Override
			public void onServiceAvailable(int protocolType, byte[] responseData, WifiP2pDevice srcDevice) {
				logd("onServiceAvailable: protocolType:" + protocolType + ", responseData: " + responseData.toString()
						+ ", WifiP2pDevice: " + srcDevice.toString());
			}
		});

		/** Setup listeners for the Bonjour services */
		getWifiP2pManager().setDnsSdResponseListeners(getChannel(), new DnsSdServiceResponseListener() {
			@Override
			public void onDnsSdServiceAvailable(String instanceName, String registrationType,
					WifiP2pDevice wifiDirectDevice) {
				logd("onDnsSdServiceAvailable: instanceName:" + instanceName + ", registrationType: " + registrationType
						+ ", WifiP2pDevice: " + wifiDirectDevice.toString());
			}
		}, new DnsSdTxtRecordListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void onDnsSdTxtRecordAvailable(String fullDomain, Map record, WifiP2pDevice device) {
				logd("onDnsSdTxtRecordAvailable: fullDomain: " + fullDomain + ", record: " + record.toString()
						+ ", WifiP2pDevice: " + device.toString());
			}
		});

		/** Setup listeners for Upnp services */
		getWifiP2pManager().setUpnpServiceResponseListener(getChannel(), new UpnpServiceResponseListener() {

			@Override
			public void onUpnpServiceAvailable(List<String> uniqueServiceNames, WifiP2pDevice srcDevice) {
				logd("onUpnpServiceAvailable: uniqueServiceNames:" + uniqueServiceNames.toString() + ", WifiP2pDevice: "
						+ srcDevice.toString());
			}
		});

		/** Register to receive all possible types of service requests. */
		addServiceRequest(WifiP2pServiceRequest.newInstance(WifiP2pServiceInfo.SERVICE_TYPE_ALL));
		addServiceRequest(WifiP2pDnsSdServiceRequest.newInstance());
		addServiceRequest(WifiP2pUpnpServiceRequest.newInstance());
		getWifiP2pManager().discoverServices(getChannel(), new ActionListener() {

			@Override
			public void onSuccess() {
				logd("discoverServices.onSuccess()");
			}

			@Override
			public void onFailure(int code) {
				logd("discoverServices.onFailure: " + code);
			}
		});
	}

	/** Stop searching for peer services */
	private void stopServiceDiscovery() {
		getWifiP2pManager().clearServiceRequests(getChannel(), null);
	}

	/**
	 * Add the service discovery request for {@link WifiP2pServiceRequest}s of
	 * the specified type.
	 */
	private void addServiceRequest(final WifiP2pServiceRequest request) {
		getWifiP2pManager().addServiceRequest(getChannel(), request, new ActionListener() {
			@Override
			public void onSuccess() {
				logd("addServiceRequest.onSuccess() for requests of type: " + request.getClass().getSimpleName());
			}

			@Override
			public void onFailure(int code) {
				logd("addServiceRequest.onFailure: " + code + ", for requests of type: "
						+ request.getClass().getSimpleName());
			}
		});
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
