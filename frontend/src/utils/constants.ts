// API 端點
export const API_ENDPOINTS = {
  AUTH: {
    REGISTER: '/auth/register',
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REFRESH: '/auth/refresh',
  },
  FRIENDS: {
    SEND_REQUEST: '/friends/requests',
    RECEIVED_REQUESTS: '/friends/requests/received',
    HANDLE_REQUEST: '/friends/requests',
    LIST: '/friends',
    DELETE: '/friends',
  },
  CHAT: {
    CONVERSATIONS: '/conversations',
    MESSAGES: (conversationId: string) => `/conversations/${conversationId}/messages`,
    MARK_READ: (conversationId: string) => `/conversations/${conversationId}/read`,
    UNREAD_COUNT: '/conversations/unread-count',
  },
};

// WebSocket 配置
export const WS_CONFIG = {
  RECONNECT_INTERVAL: 5000,
  HEARTBEAT_INTERVAL: 30000,
};

// 本地儲存鍵
export const STORAGE_KEYS = {
  ACCESS_TOKEN: 'accessToken',
  REFRESH_TOKEN: 'refreshToken',
  USER_ID: 'userId',
  USERNAME: 'username',
};
