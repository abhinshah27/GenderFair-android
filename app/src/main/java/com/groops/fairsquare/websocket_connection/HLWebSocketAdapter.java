package com.groops.fairsquare.websocket_connection;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.groops.fairsquare.base.HLApp;
import com.groops.fairsquare.base.OnApplicationContextNeeded;
import com.groops.fairsquare.services.ChatSetUserOnlineService;
import com.groops.fairsquare.services.GetConfigurationDataService;
import com.groops.fairsquare.services.HandleChatsUpdateService;
import com.groops.fairsquare.services.SendFCMTokenService;
import com.groops.fairsquare.services.SubscribeToSocketService;
import com.groops.fairsquare.services.SubscribeToSocketServiceChat;
import com.groops.fairsquare.utility.LogUtils;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mbaldrighi on 9/1/2017.
 */
public class HLWebSocketAdapter extends WebSocketAdapter implements Handler.Callback {

	private static final String LOG_TAG = HLWebSocketAdapter.class.getCanonicalName();

	private OnApplicationContextNeeded application;

	private OnConnectionChangedListener mListener;

	private Set<ConnectionObserver> connectionObservers = new HashSet<>();

	private boolean isChat;

	private Handler subscribeRTComHandler, subscribeChatHandler;
	private boolean rtComSubscribed, chatSubscribed;


	HLWebSocketAdapter(OnApplicationContextNeeded application, boolean isChat) {
		this.application = application;
		this.isChat = isChat;

		if (isChat) subscribeChatHandler = new Handler(this);
		else subscribeRTComHandler = new Handler(this);
	}


	@Override
	public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
		super.onStateChanged(websocket, newState);
		LogUtils.d(LOG_TAG, "WS STATE CHANGED: " + newState + " for WebSocket: " + websocket.toString());

		if (newState != WebSocketState.OPEN)
			mListener.onConnectionChange();
	}

	@Override
	public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
		super.onConnected(websocket, headers);
		LogUtils.d(LOG_TAG, "WS CONNECTION SUCCESS for WebSocket: " + websocket.toString());

		if (isChat)
			// automatically re-subscribe client to real-time communication
			SubscribeToSocketServiceChat.startService(application.getHLContext(), subscribeChatHandler);
		else {
			// automatically re-subscribe client to real-time communication
			SubscribeToSocketService.startService(application.getHLContext(), subscribeRTComHandler);
		}
	}

	@Override
	public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame,
	                           WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
		super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
		LogUtils.d(LOG_TAG, "WS DISCONNECTION SUCCESS" + " for WebSocket: " + websocket.toString());

		HLApp.subscribedToSocket = false;
		HLApp.subscribedToSocketChat = false;

		if (closedByServer)
			HLApp.getSocketConnection().openConnection(isChat);
	}

	@Override
	public void onTextMessage(WebSocket websocket, String text) throws Exception {
		super.onTextMessage(websocket, text);
		LogUtils.d(LOG_TAG, "WS MESSAGE RECEIVED: " + text);

		HLRequestTracker.getInstance(application).onDataReceivedAsync(text);
	}

	@Override
	public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
		super.onBinaryMessage(websocket, binary);
		LogUtils.d(LOG_TAG, "WS ENCRYPTED BYTE[] MESSAGE RECEIVED");

		HLRequestTracker.getInstance(application).onDataReceivedAsync(binary);
	}

	/*
	 * ERRORS
	 */
	@Override
	public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
		super.onError(websocket, cause);
		LogUtils.e(LOG_TAG, "WS GENERIC ERROR: " + cause.getMessage() + " for WebSocket: " + websocket.toString(), cause);
	}

	@Override
	public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
		super.onConnectError(websocket, exception);
		LogUtils.e(LOG_TAG, "WS CONNECTION ERROR: " + exception.getMessage() + " for WebSocket: " + websocket.toString(), exception);
	}

	@Override
	public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
		super.onTextMessageError(websocket, cause, data);
		LogUtils.e(LOG_TAG, "WS TEXT MESSAGE ERROR: " + cause.getMessage(), cause);
	}

	@Override
	public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
		super.onUnexpectedError(websocket, cause);
		LogUtils.e(LOG_TAG, "WS UNEXPECTED ERROR: " + cause.getMessage(), cause);
	}

	@Override
	public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
		super.onSendError(websocket, cause, frame);
		LogUtils.e(LOG_TAG, "WS SEND ERROR: " + cause.getMessage() + " for WebSocket: " + websocket.toString(), cause);
	}

	/*
		* PINGs&PONGs
		*/
	@Override
	public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
		super.onPingFrame(websocket, frame);
		LogUtils.v(LOG_TAG, "WS PING RECEIVED" + " for WebSocket: " + websocket.toString());
		websocket.sendPong();
	}

	@Override
	public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
		super.onPongFrame(websocket, frame);
		LogUtils.v(LOG_TAG, "WS PONG RECEIVED" + " for WebSocket: " + websocket.toString());
	}



	private void notifyAllObservers(boolean isChat) {
		if (connectionObservers != null) {
			for (ConnectionObserver observer : connectionObservers) {
				if (observer != null)
					observer.onConnectionEstablished(isChat);
			}
		}
	}

	public void attachConnectionObserver(ConnectionObserver observer) {
		if (observer == null) return;

		if (connectionObservers == null)
			connectionObservers = new HashSet<>();

		connectionObservers.add(observer);
	}

	void subscribeSocketRTCom(Context context) {
		SubscribeToSocketService.startService(context, subscribeRTComHandler);
	}

	// INFO: 4/2/19    also Interests can have chat sessions
	void subscribeSocketChat(Context context) {
		SubscribeToSocketServiceChat.startService(context, subscribeChatHandler);
	}


	public interface ConnectionObserver {

		void onConnectionEstablished(boolean isChat);

	}

	@Override
	public boolean handleMessage(Message msg) {

		if (msg != null) {
			switch (msg.what) {
				case SubscribeToSocketService.SUCCESS_RTCOM:

					rtComSubscribed = true;

                    // notification for custom actions
				    notifyAllObservers(false);

					HLRequestTracker.getInstance(application).checkPendingAndRetry(false);
					if (HLSocketConnection.getInstance().isConnectedChat()) {
						HLRequestTracker.getInstance(application).checkPendingAndRetry(true);

						HandleChatsUpdateService.startService(application.getHLContext());
						ChatSetUserOnlineService.startService(application.getHLContext());

						// notification for custom actions
						notifyAllObservers(true);
					}

					// automatically re-sends client's FCM notifications token
					SendFCMTokenService.startService(application.getHLContext(), null);
					// automatically re-fetches user's general configuration data
					GetConfigurationDataService.startService(application.getHLContext());
					break;

				case SubscribeToSocketServiceChat.SUCCESS_CHAT:

					chatSubscribed = true;

					// INFO: 4/18/19    socket CHAT has to wait that main socket is connected to attempt retry ops
					if (HLSocketConnection.getInstance().isConnected()) {
						HLRequestTracker.getInstance(application).checkPendingAndRetry(true);

						// notification for custom actions
						notifyAllObservers(true);

						HandleChatsUpdateService.startService(application.getHLContext());
						ChatSetUserOnlineService.startService(application.getHLContext());
					}

					break;
			}
			return true;
		}

		return false;
	}


	//region == Getters and setters ==

	public void setListener(OnConnectionChangedListener listener) {
		this.mListener = listener;
	}

	public boolean isRtComSubscribed() {
		return rtComSubscribed;
	}

	public boolean isChatSubscribed() {
		return chatSubscribed;
	}

	//endregion

}
