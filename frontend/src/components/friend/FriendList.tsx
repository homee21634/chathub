import React, { useEffect } from 'react';
import { useFriendStore } from '../../store/friendStore';
import { FriendCard } from './FriendCard';

export const FriendList: React.FC = () => {
  const { friends, isLoading, error, loadFriends } = useFriendStore();

  useEffect(() => {
    loadFriends();
  }, [loadFriends]);

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

  if (friends.length === 0) {
    return (
      <div className="text-center p-8 bg-gray-50 rounded-lg">
        <p className="text-gray-600">目前沒有好友</p>
        <p className="text-sm text-gray-500 mt-2">試著搜尋並新增一些好友吧！</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {friends.map((friend) => (
        <FriendCard key={friend.userId} friend={friend} />
      ))}
    </div>
  );
};
