// API Response 包裝型別
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  username: string;
  password: string;
  confirmPassword: string;
}

export interface LoginResponseData {
  accessToken: string;
  refreshToken: string;
  username: string;
  userId: string;
  expiresIn: number;
}

export interface RegisterResponseData {
  userId: string;
  username: string;
  createdAt: string;
}

export interface User {
  id: string;
  username: string;
}
