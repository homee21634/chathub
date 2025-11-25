export interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  senderUsername: string;
  content: string;
  timestamp: string;
  status?: 'sending' | 'sent' | 'delivered' | 'read';
  clientMessageId?: string;
}

export interface Conversation {
  id: string;
  participantId: string;
  participantUsername: string;
  lastMessage?: string;
  lastMessageTime?: string;
  unreadCount: number;
  isOnline?: boolean;
}

export interface SendMessageRequest {
  recipientId: string;
  content: string;
  clientMessageId: string;
}
