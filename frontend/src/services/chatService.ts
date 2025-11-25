import api from './api';
import { API_ENDPOINTS } from '../utils/constants';
import { ApiResponse } from '../types/auth';
import { Conversation, ChatMessage, PaginatedConversations, PaginatedMessages } from '../types/chat';

export const chatService = {
  // 獲取對話列表
  getConversations: async (page: number = 0, size: number = 20): Promise<PaginatedConversations> => {
    const response = await api.get<ApiResponse<PaginatedConversations>>(
      `${API_ENDPOINTS.CHAT.CONVERSATIONS}?page=${page}&size=${size}`
    );
    return response.data.data;
  },

  // 獲取對話的歷史訊息
  getMessages: async (conversationId: string, page: number = 0, size: number = 50): Promise<PaginatedMessages> => {
    const response = await api.get<ApiResponse<PaginatedMessages>>(
      `${API_ENDPOINTS.CHAT.MESSAGES(conversationId)}?page=${page}&size=${size}`
    );
    return response.data.data;
  },

  // 標記訊息為已讀
  markAsRead: async (conversationId: string): Promise<void> => {
    await api.put<ApiResponse<void>>(
      API_ENDPOINTS.CHAT.MARK_READ(conversationId)
    );
  },

  // 獲取未讀訊息總數
  getUnreadCount: async (): Promise<number> => {
    const response = await api.get<ApiResponse<number>>(
      API_ENDPOINTS.CHAT.UNREAD_COUNT
    );
    return response.data.data;
  },
};
