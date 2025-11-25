import api from './api';
import { Friend, FriendRequest, SendFriendRequestDto, HandleFriendRequestDto } from '../types/friend';
import { ApiResponse } from '../types/auth';
import { API_ENDPOINTS } from '../utils/constants';

export const friendService = {
  // 發送好友請求
  sendFriendRequest: async (username: string): Promise<FriendRequest> => {
    const response = await api.post<ApiResponse<FriendRequest>>(
      API_ENDPOINTS.FRIENDS.SEND_REQUEST,
      { username }
    );
    return response.data.data;
  },

  // 獲取收到的好友請求
  getReceivedRequests: async (): Promise<FriendRequest[]> => {
    const response = await api.get<ApiResponse<FriendRequest[]>>(
      API_ENDPOINTS.FRIENDS.RECEIVED_REQUESTS
    );
    return response.data.data;
  },

  // 處理好友請求
  handleFriendRequest: async (
    requestId: string,
    action: 'accept' | 'reject'
  ): Promise<void> => {
    await api.put<ApiResponse<void>>(
      `${API_ENDPOINTS.FRIENDS.HANDLE_REQUEST}/${requestId}`,
      { action }
    );
  },

  // 獲取好友列表
  getFriends: async (): Promise<Friend[]> => {
    const response = await api.get<ApiResponse<Friend[]>>(
      API_ENDPOINTS.FRIENDS.LIST
    );
    return response.data.data;
  },

  // 刪除好友
  deleteFriend: async (friendId: string): Promise<void> => {
    await api.delete<ApiResponse<void>>(
      `${API_ENDPOINTS.FRIENDS.DELETE}/${friendId}`
    );
  },
};
