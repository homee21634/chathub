import api from './api';
import { LoginRequest, RegisterRequest, LoginResponseData, RegisterResponseData, ApiResponse } from '../types/auth';
import { API_ENDPOINTS } from '../utils/constants';

export const authService = {
  login: async (data: LoginRequest): Promise<LoginResponseData> => {
    const response = await api.post<ApiResponse<LoginResponseData>>(
      API_ENDPOINTS.AUTH.LOGIN,
      data
    );
    return response.data.data;
  },

  register: async (data: RegisterRequest): Promise<RegisterResponseData> => {
    const response = await api.post<ApiResponse<RegisterResponseData>>(
      API_ENDPOINTS.AUTH.REGISTER,
      data
    );
    return response.data.data;
  },

  logout: async (): Promise<void> => {
    await api.post(API_ENDPOINTS.AUTH.LOGOUT);
  },

  refresh: async (refreshToken: string): Promise<LoginResponseData> => {
    const response = await api.post<ApiResponse<LoginResponseData>>(
      API_ENDPOINTS.AUTH.REFRESH,
      { refreshToken }
    );
    return response.data.data;
  },
};
