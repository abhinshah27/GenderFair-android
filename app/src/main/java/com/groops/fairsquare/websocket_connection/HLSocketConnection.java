package com.groops.fairsquare.websocket_connection;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.groops.fairsquare.BuildConfig;
import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.base.OnApplicationContextNeeded;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.LogUtils;
import com.groops.fairsquare.utility.Utils;
import com.groops.fairsquare.websocket_connection.session.SessionWrapper;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Singleton class handling the web socket connection lifecycle.
 *
 * @author mbaldrighi on 9/1/2017.
 */
public class HLSocketConnection {

	public static final String LOG_TAG = HLSocketConnection.class.getCanonicalName();

	private static final String END_POINT = BuildConfig.USE_PROD_CONNECTION ? Constants.SERVER_END_POINT_PROD : Constants.SERVER_END_POINT_DEV;
	private static final String END_POINT_CHAT = BuildConfig.USE_PROD_CONNECTION ? Constants.SERVER_END_POINT_PROD_CHAT : Constants.SERVER_END_POINT_DEV_CHAT;

	private static HLSocketConnection instance;

	private HLWebSocketClient client, clientChat;
	private ScreenStateReceiver screenStateReceiver;
	private ConnectionChangeReceiver connectionChangeReceiver;

	private final Runnable checkConnectionRunnable = new Runnable() {
		@Override
		public void run() {
			LogUtils.v(LOG_TAG, "Checking CONNECTION STATUS");
			if (!isConnected(false))
				openConnection(false);
			if (!isConnectedChat(false))
				openConnection(true);
		}
	};

	private OnApplicationContextNeeded application;
	private ScheduledExecutorService connectionScheduler;
	private ScheduledFuture scheduledFuture;

	private HLSocketConnection() {
		screenStateReceiver = new ScreenStateReceiver(this);
		connectionChangeReceiver = new ConnectionChangeReceiver(this);
	}

	public static HLSocketConnection getInstance() {
		if (instance == null)
			instance = new HLSocketConnection();

		LogUtils.d(LOG_TAG, "TEST SOCKET Connection singleton ID: " + instance.toString());

		return instance;
	}

	/**
	 * Retrieves the class singleton instance, registering ActivityLifecycle callbacks.
	 * Valid only in the Application's onCreate() method.
	 */
	public static void init() {
		instance = new HLSocketConnection();
	}

	private void startCheckConnection() {
		if(connectionScheduler == null)
			connectionScheduler = Executors.newSingleThreadScheduledExecutor();
		if(scheduledFuture == null)
			scheduledFuture = connectionScheduler.scheduleAtFixedRate(checkConnectionRunnable,
					500, 5000, TimeUnit.MILLISECONDS);
	}

	private void stopCheckConnection() {
		if(scheduledFuture != null) {
			scheduledFuture.cancel(true);
			scheduledFuture = null;
		}
	}


	public void openConnection(boolean isChat) {
		if (isChat)
			if (clientChat != null) clientChat.close();
		else
			if (client != null) client.close();
		try {
			if (isChat) {
				if (clientChat == null)
					clientChat = new HLWebSocketClient(application, END_POINT_CHAT, true);
			}
			else {
				if (client == null)
					client = new HLWebSocketClient(application, END_POINT, false);
			}

			if (Utils.isDeviceConnected(application.getHLContext())) {
				if (isChat) {
					clientChat.connect();
					LogUtils.d(LOG_TAG, "Device connected for CHAT");
				}
				else {
					client.connect();
					LogUtils.d(LOG_TAG, "Device connected");

					// resets pagination vars -> after reconnection always get posts and interactions from scratch
					HLApp.resetPaginationIds();
				}
			}
			else {
				LogUtils.e(LOG_TAG, "NO CONNECTION AVAILABLE");
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtils.e(LOG_TAG, e.getMessage(), e);
		}
		initScreenStateListener();
//		initConnectionChangeListener();
		startCheckConnection();
	}

	public void closeConnection() {
		if (client != null) {
			client.close();
			client = null;
		}
		if (clientChat != null) {
			clientChat.close();
			clientChat = null;
		}
		releaseScreenStateListener();
//		releaseConnectionChangeListener();
		stopCheckConnection();
	}

	/**
	 * This method sends calls to server for socket /rtcom.
	 * @param message the provided message to be sent.
	 */
	public void sendMessage(@NonNull String message) {
		LogUtils.d(LOG_TAG, "Original message: " + message);
		sendMessage(Crypto.encryptData(message));
	}

	private void sendMessage(byte[] message) {
		if (message != null && message.length > 0) {
			if (isConnected(false)) {
				client.getWebSocket().sendBinary(message);
				LogUtils.d(LOG_TAG, "Encrypted Bytes sent: " + Arrays.toString(message));
				return;
			}

			LogUtils.d(LOG_TAG, "Socket not connected. Bytes: " + Arrays.toString(message) + "\tNOT SENT");
			return;
		}

		LogUtils.d(LOG_TAG, "Original bytes null or empty\tNOT SENT");
	}

	/**
	 * This method sends calls to server for socket /chat.
	 * @param message the provided message to be sent.
	 */
	public void sendMessageChat(@NonNull String message) {
		LogUtils.d(LOG_TAG, "Original message: " + message);
		sendMessageChat(Crypto.encryptData(message));
	}

	private void sendMessageChat(byte[] message) {
		if (message != null && message.length > 0) {
			if (isConnectedChat(false)) {
				clientChat.getWebSocket().sendBinary(message);
				LogUtils.d(LOG_TAG, "Encrypted Bytes sent: " + Arrays.toString(message));
				return;
			}

			LogUtils.d(LOG_TAG, "Socket not connected. Bytes: " + Arrays.toString(message) + "\tNOT SENT");
			return;
		}

		LogUtils.d(LOG_TAG, "Original bytes null or empty\tNOT SENT");
	}

	/**
	 * Screen state listener for socket life cycle
	 */
	private void initScreenStateListener() {
//		application.getHLContext().registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
//		application.getHLContext().registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		LocalBroadcastManager.getInstance(application.getHLContext()).registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		LocalBroadcastManager.getInstance(application.getHLContext()).registerReceiver(screenStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
	}

	private void releaseScreenStateListener() {
		try {
//			application.getHLContext().unregisterReceiver(screenStateReceiver);
			LocalBroadcastManager.getInstance(application.getHLContext()).unregisterReceiver(screenStateReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Connection changes listener for socket life cycle
	 */
	private void initConnectionChangeListener() {
//		application.getHLContext().registerReceiver(connectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		LocalBroadcastManager.getInstance(application.getHLContext()).registerReceiver(connectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void releaseConnectionChangeListener() {
		try {
//			application.getHLContext().unregisterReceiver(connectionChangeReceiver);
			LocalBroadcastManager.getInstance(application.getHLContext()).unregisterReceiver(connectionChangeReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to check if RTCOM socket is connected AND subscribed.
	 * @return The connection status of the socket.
	 */
	public boolean isConnected() {
		return isConnected(true);
	}

	/**
	 * This method is used to check if RTCOM socket is both connected or connected AND subscribed.
	 * @param forCalls whether the check must be performed also on subscription.
	 * @return The connection status of the socket.
	 */
	public boolean isConnected(boolean forCalls) {
		boolean basicCondition = Utils.isDeviceConnected(application.getHLContext()) &&
				client != null && client.hasOpenConnection();

		if (forCalls && SessionWrapper.isValid())
			return basicCondition && client.isSocketSubscribed();
		else
			return basicCondition;
	}

	/**
	 * This method is used to check if CHAT socket is both connected AND subscribed.
	 * @return The connection status of the socket.
	 */
	public boolean isConnectedChat() {
		return isConnectedChat(true);
	}

	/**
	 * This method is used to check if CHAT socket is just connected or connected AND subscribed.
	 * @param forCalls whether the check must be performed also on subscription.
	 * @return The connection status of the socket.
	 */
	public boolean isConnectedChat(boolean forCalls) {
		boolean basicCondition = Utils.isDeviceConnected(application.getHLContext()) &&
				clientChat != null && clientChat.hasOpenConnection();

		if (forCalls && SessionWrapper.isValid())
			return  basicCondition && clientChat.isSocketSubscribed();
		else
			return basicCondition;
	}

	/**
	 * It serves as only entry point for the socket subscription process, other than the {@link HLWebSocketAdapter} itself.
	 * @param context the activity's/application's {@link Context}
	 */
	public void subscribeSockets(Context context) {
		if (getClient() != null && getClient().getAdapter() != null)
			getClient().getAdapter().subscribeSocketRTCom(context);
		if (getClientChat() != null && getClientChat().getAdapter() != null)
			getClientChat().getAdapter().subscribeSocketChat(context);
	}

	/**
	 * It serves as only entry point for the socket subscription listeners.
	 * @param observer the provided {@link com.groops.fairsquare.websocket_connection.HLWebSocketAdapter.ConnectionObserver}
	 */
	public void attachSubscriptionObservers(HLWebSocketAdapter.ConnectionObserver observer) {
		if (getClient() != null && getClient().getAdapter() != null)
			getClient().getAdapter().attachConnectionObserver(observer);
		if (getClientChat() != null && getClientChat().getAdapter() != null)
			getClientChat().getAdapter().attachConnectionObserver(observer);
	}


	public void setContextListener(OnApplicationContextNeeded application) { this.application = application; }

	public HLWebSocketClient getClient() {
		return client;
	}
	public HLWebSocketClient getClientChat() {
		return clientChat;
	}


	/**
	 * This method is the common entry point to update the socket header OUTSIDE the socket's creation.
	 * <p>
	 * IMPORTANT: only disconnecting and reconnecting, headers can be successfully added.
	 */
	public void updateSocketHeader() {
		// only disconnecting and reconnecting seem to do the trick with headers
		closeConnection();
		openConnection(false);
		openConnection(true);
	}


	@Override
	public String toString() {
		return String.valueOf(this.hashCode());
	}
}
