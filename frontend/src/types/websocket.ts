export enum WebSocketMessageType {
  SEND_MESSAGE = 'SEND_MESSAGE',
  NEW_MESSAGE = 'NEW_MESSAGE',
  TYPING = 'TYPING',
  READ_RECEIPT = 'READ_RECEIPT',
  DELIVERY_RECEIPT = 'DELIVERY_RECEIPT',
  FRIEND_REQUEST = 'FRIEND_REQUEST',
  FRIEND_ONLINE = 'FRIEND_ONLINE',
  FRIEND_OFFLINE = 'FRIEND_OFFLINE',
}

export interface WebSocketMessage<T = any> {
  type: WebSocketMessageType;
  payload: T;
  timestamp: string;
}
