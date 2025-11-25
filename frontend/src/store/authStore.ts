import { create } from 'zustand';
import { User, LoginResponseData } from '../types/auth';
import { STORAGE_KEYS } from '../utils/constants';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  // Actions
  setAuth: (data: LoginResponseData) => void;
  clearAuth: () => void;
  setLoading: (loading: boolean) => void;
  initializeAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: true,

  setAuth: (data: LoginResponseData) => {
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, data.accessToken);
    localStorage.setItem(STORAGE_KEYS.REFRESH_TOKEN, data.refreshToken);
    localStorage.setItem(STORAGE_KEYS.USER_ID, data.userId);
    localStorage.setItem(STORAGE_KEYS.USERNAME, data.username);

    set({
      user: { id: data.userId, username: data.username },
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      isAuthenticated: true,
      isLoading: false,
    });
  },

  clearAuth: () => {
    localStorage.clear();
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  setLoading: (loading: boolean) => {
    set({ isLoading: loading });
  },

  initializeAuth: () => {
    const accessToken = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
    const refreshToken = localStorage.getItem(STORAGE_KEYS.REFRESH_TOKEN);
    const userId = localStorage.getItem(STORAGE_KEYS.USER_ID);
    const username = localStorage.getItem(STORAGE_KEYS.USERNAME);

    if (accessToken && refreshToken && userId && username) {
      set({
        user: { id: userId, username },
        accessToken,
        refreshToken,
        isAuthenticated: true,
        isLoading: false,
      });
    } else {
      set({ isLoading: false });
    }
  },
}));
