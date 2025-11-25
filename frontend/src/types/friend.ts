export interface Friend {
  userId: string;
  username: string;
  isOnline: boolean;
  friendsSince: string;
}

export interface FriendRequest {
  requestId: string;
  fromUserId: string;
  fromUsername: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}

export interface SendFriendRequestDto {
  username: string;
}

export interface HandleFriendRequestDto {
  action: 'accept' | 'reject';
}
