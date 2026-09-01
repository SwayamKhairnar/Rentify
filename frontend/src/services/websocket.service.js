import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

let stompClient = null;
let activeSubscriptions = new Map();

function getWebSocketUrl() {
  const apiUrl = import.meta.env.VITE_API_URL || '';
  if (apiUrl.startsWith('http://') || apiUrl.startsWith('https://')) {
    const url = new URL(apiUrl);
    return `${url.protocol}//${url.host}/ws`;
  }
  return '/ws';
}

export const websocketService = {
  connect(onConnected, onError) {
    if (stompClient && stompClient.connected) {
      if (onConnected) onConnected();
      return;
    }

    const wsUrl = getWebSocketUrl();
    stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        if (onConnected) onConnected();
      },
      onStompError: (frame) => {
        console.error('STOMP Broker error: ', frame.headers['message']);
        if (onError) onError(frame);
      },
      onWebSocketClose: () => {
        // Handled by auto reconnect
      }
    });

    stompClient.activate();
  },

  subscribeToConversation(conversationId, onMessageReceived) {
    if (!stompClient || !stompClient.connected) {
      this.connect(() => {
        this.subscribeToConversation(conversationId, onMessageReceived);
      });
      return () => {};
    }

    const destination = `/topic/conversations/${conversationId}`;
    if (activeSubscriptions.has(destination)) {
      activeSubscriptions.get(destination).unsubscribe();
    }

    const subscription = stompClient.subscribe(destination, (message) => {
      try {
        const payload = JSON.parse(message.body);
        if (onMessageReceived) onMessageReceived(payload);
      } catch (err) {
        console.error('Error parsing WebSocket message payload:', err);
      }
    });

    activeSubscriptions.set(destination, subscription);

    return () => {
      subscription.unsubscribe();
      activeSubscriptions.delete(destination);
    };
  },

  disconnect() {
    if (stompClient) {
      stompClient.deactivate();
      stompClient = null;
      activeSubscriptions.clear();
    }
  }
};
