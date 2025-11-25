import React, { useEffect } from 'react';
import { useFriendStore } from '../../store/friendStore';
import { FriendRequestCard } from './FriendRequestCard';

export const FriendRequestList: React.FC = () => {
  const { friendRequests, isLoading, error, loadFriendRequests } = useFriendStore();

  useEffect(() => {
    loadFriendRequests();
  }, [loadFriendRequests]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center p-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-red-600">
        {error}
      </div>
    );
  }

  const pendingRequests = friendRequests.filter(req => req.status === 'PENDING');

  if (pendingRequests.length === 0) {
    return (
      <div className="text-center p-8 bg-gray-50 rounded-lg">
        <p className="text-gray-600">目前沒有待處理的好友請求</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {pendingRequests.map((request) => (
        <FriendRequestCard key={request.requestId} request={request} />
      ))}
    </div>
  );
};
