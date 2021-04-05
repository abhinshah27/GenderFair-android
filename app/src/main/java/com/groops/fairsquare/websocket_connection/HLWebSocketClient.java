package com.groops.fairsquare.websocket_connection;

import com.groops.fairsquare.base.OnApplicationContextNeeded;
import com.groops.fairsquare.models.HLUser;
import com.groops.fairsquare.utility.Constants;
import com.groops.fairsquare.utility.LogUtils;
import com.groops.fairsquare.utility.realm.RealmUtils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;

import io.realm.Realm;

/**
 * Instance of the web socket client.
 * <p>
 * It handles the instantiation of {@link WebSocket} and {@link HLWebSocketAdapter} objects.
 *
 * @author mbaldrighi on 9/1/2017.
 */
public class HLWebSocketClient {

	private static final String SOCKET_HEADER_ID = "x-id";
	private static final String SOCKET_HEADER_DEVICE = "x-device-id";
	public static final String LOG_TAG = HLWebSocketClient.class.getCanonicalName();
	private static final int TIMEOUT = 3000;

	private OnApplicationContextNeeded application;

	private String host;
	private WebSocket webSocket;

	private HLWebSocketAdapter mAdapter;

	private boolean isChat;


	HLWebSocketClient(OnApplicationContextNeeded application, String host, boolean isChat) {
		this.application = application;
		this.host = host;
		this.isChat = isChat;

		LogUtils.d(LOG_TAG, "TEST SOCKET Client ID: " + this.toString());
	}

	void connect() {
		try {
			if (webSocket == null) {
				webSocket = new WebSocketFactory().createSocket(host, TIMEOUT);
				webSocket.addListener(mAdapter = new HLWebSocketAdapter(application, isChat));
				webSocket.addHeader(SOCKET_HEADER_ID, getUserId());
				webSocket.addHeader(SOCKET_HEADER_DEVICE, getDeviceId());
				webSocket.addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
				webSocket.setPingInterval(Constants.TIME_UNIT_SECOND * 60);
				webSocket.connectAsynchronously();

				LogUtils.d(LOG_TAG, "TEST SOCKET WebSocket ID: " + webSocket.toString());
			}
			else reconnect();
		}
		catch (IOException e) {
			e.printStackTrace();
			LogUtils.e(LOG_TAG, e.getMessage());
		}
	}

	private void reconnect() throws IOException {
		webSocket = webSocket.recreate().connectAsynchronously();

		LogUtils.d(LOG_TAG, "WebSocket ID: " + webSocket.toString());
	}

	public void close() {
		if (webSocket != null)
			webSocket.disconnect();
	}

	boolean hasOpenConnection() {
		return webSocket != null && webSocket.isOpen();
	}

	boolean isSocketSubscribed() {
		return mAdapter != null && (mAdapter.isRtComSubscribed() || mAdapter.isChatSubscribed());
	}


	//region == Getters and setters ==

	WebSocket getWebSocket() {
		return webSocket;
	}

	public HLWebSocketAdapter getAdapter() {
		return mAdapter;
	}

	//endregion


	private String getUserId() {
		Realm realm = null;
		String userId = null;
		try {
			realm = RealmUtils.getCheckedRealm();
			userId = new HLUser().readUser(realm).getUserId();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			RealmUtils.closeRealm(realm);
		}
		return userId;
	}

	private String getDeviceId() {
		return HLServerCalls.getSecureID(application.getHLContext());
	}

}
