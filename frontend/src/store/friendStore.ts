import { create } from 'zustand';
import { Friend, FriendRequest } from '../types/friend';
import { friendService } from '../services/friendService';

interface FriendState {
  friends: Friend[];
  friendRequests: FriendRequest[];
  isLoading: boolean;
  error: string | null;

  // Actions
  loadFriends: () => Promise<void>;
  loadFriendRequests: () => Promise<void>;
  sendFriendRequest: (username: string) => Promise<void>;
  acceptFriendRequest: (requestId: string) => Promise<void>;
  rejectFriendRequest: (requestId: string) => Promise<void>;
  deleteFriend: (friendId: string) => Promise<void>;
  clearError: () => void;
}

export const useFriendStore = create<FriendState>((set, get) => ({
  friends: [],
  friendRequests: [],
  isLoading: false,
  error: null,

  loadFriends: async () => {
    set({ isLoading: true, error: null });
    try {
      const friends = await friendService.getFriends();
      set({ friends, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || '載入好友列表失敗',
        isLoading: false
      });
    }
  },

  loadFriendRequests: async () => {
    set({ isLoading: true, error: null });
    try {
      const requests = await friendService.getReceivedRequests();
      set({ friendRequests: requests, isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || '載入好友請求失敗',
        isLoading: false
      });
    }
  },

  sendFriendRequest: async (username: string) => {
    set({ isLoading: true, error: null });
    try {
      await friendService.sendFriendRequest(username);
      set({ isLoading: false });
    } catch (error: any) {
      set({
        error: error.response?.data?.message || '發送好友請求失敗',
        isLoading: false
      });
      throw error;
    }
  },

  acceptFriendRequest: async (requestId: string) => {
    set({ isLoading: true, error: null });
    try {
      await friendService.handleFriendRequest(requestId, 'accept');
      // 重新載入資料
      await get().loadFriendRequests();
      await get().loadFriends();
    } catch (error: any) {
      set({
        error: error.response?.data?.message || '接受好友請求失敗',
        isLoading: false
      });
    }
  },

  rejectFriendRequest: async (requestId: string) => {
    set({ isLoading: true, error: null });
    try {
      await friendService.handleFriendRequest(requestId, 'reject');
      // 重新載入好友請求列表
      await get().loadFriendRequests();
    } catch (error: any) {
      set({
        error: error.response?.data?.message || '拒絕好友請求失敗',
        isLoading: false
      });
    }
  },

  deleteFriend: async (friendId: string) => {
    set({ isLoading: true, error: null });
    try {
      await friendService.deleteFriend(friendId);
      // 重新載入好友列表
      await get().loadFriends();
    } catch (error: any) {
      set({
        error: error.response?.data?.message || '刪除好友失敗',
        isLoading: false
      });
    }
  },

  clearError: () => set({ error: null }),
}));
