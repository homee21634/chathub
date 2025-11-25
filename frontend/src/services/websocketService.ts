import { WebSocketMessage } from '../types/chat';

type MessageHandler = (message: WebSocketMessage) => void;
type ConnectionHandler = () => void;

class WebSocketService {
  private ws: WebSocket | null = null;
  private messageHandlers: Set<MessageHandler> = new Set();
  private connectionHandlers: Set<ConnectionHandler> = new Set();
  private disconnectionHandlers: Set<ConnectionHandler> = new Set();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private reconnectTimeout: NodeJS.Timeout | null = null;
  private isIntentionalClose = false;

  connect(token: string): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.warn('WebSocket already connected');
      return;
    }

    this.isIntentionalClose = false;
    const wsUrl = `ws://localhost:8080/ws/chat?token=${token}`;

    try {
      this.ws = new WebSocket(wsUrl);
      this.setupEventListeners();
      this.setupHeartbeat();
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.handleReconnect(token);
    }
  }

  private setupEventListeners(): void {
    if (!this.ws) return;

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.connectionHandlers.forEach(handler => handler());
    };

    this.ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data);
        this.messageHandlers.forEach(handler => handler(message));
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.cleanup();
      this.disconnectionHandlers.forEach(handler => handler());

      if (!this.isIntentionalClose && this.reconnectAttempts < this.maxReconnectAttempts) {
        const token = localStorage.getItem('accessToken');
        if (token) {
          this.handleReconnect(token);
        }
      }
    };
  }

  private setupHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({
          type: 'PING',
          payload: {},
          timestamp: new Date().toISOString(),
        });
      }
    }, 30000); // 每 30 秒發送心跳
  }

  private handleReconnect(token: string): void {
    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts;

    console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

    this.reconnectTimeout = setTimeout(() => {
      this.connect(token);
    }, delay);
  }

  private cleanup(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  send(message: WebSocketMessage): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('WebSocket is not connected');
    }
  }

  sendMessage(recipientId: string, content: string, clientMessageId: string): void {
    this.send({
      type: 'SEND_MESSAGE',
      payload: {
        recipientId,
        content,
        clientMessageId,
      },
      timestamp: new Date().toISOString(),
    });
  }

  sendTypingStart(recipientId: string): void {
    this.send({
      type: 'TYPING_START',
      payload: { recipientId },
      timestamp: new Date().toISOString(),
    });
  }

  sendTypingStop(recipientId: string): void {
    this.send({
      type: 'TYPING_STOP',
      payload: { recipientId },
      timestamp: new Date().toISOString(),
    });
  }

  markAsRead(conversationId: string): void {
    this.send({
      type: 'MESSAGE_READ',
      payload: { conversationId },
      timestamp: new Date().toISOString(),
    });
  }

  onMessage(handler: MessageHandler): () => void {
    this.messageHandlers.add(handler);
    return () => this.messageHandlers.delete(handler);
  }

  onConnect(handler: ConnectionHandler): () => void {
    this.connectionHandlers.add(handler);
    return () => this.connectionHandlers.delete(handler);
  }

  onDisconnect(handler: ConnectionHandler): () => void {
    this.disconnectionHandlers.add(handler);
    return () => this.disconnectionHandlers.delete(handler);
  }

  disconnect(): void {
    this.isIntentionalClose = true;
    this.cleanup();

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }
}

export const websocketService = new WebSocketService();
