import React, { useState } from 'react';
import { Friend } from '../../types/friend';
import { Button } from '../common/Button';
import { useFriendStore } from '../../store/friendStore';

interface FriendCardProps {
  friend: Friend;
}

export const FriendCard: React.FC<FriendCardProps> = ({ friend }) => {
  const [showConfirm, setShowConfirm] = useState(false);
  const { deleteFriend, isLoading } = useFriendStore();

  const handleDelete = async () => {
    await deleteFriend(friend.userId);
    setShowConfirm(false);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('zh-TW', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="bg-white p-4 rounded-lg shadow hover:shadow-md transition-shadow">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          {/* 頭像 */}
          <div className="w-12 h-12 bg-gradient-to-br from-blue-400 to-blue-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
            {friend.username.charAt(0).toUpperCase()}
          </div>

          <div>
            <div className="flex items-center space-x-2">
              <h3 className="font-semibold text-gray-900">{friend.username}</h3>
              {friend.isOnline && (
                <span className="w-2 h-2 bg-green-500 rounded-full"></span>
              )}
            </div>
            <p className="text-sm text-gray-500">
              好友時間：{formatDate(friend.friendsSince)}
            </p>
          </div>
        </div>

        {/* 刪除按鈕 */}
        {!showConfirm ? (
          <button
            onClick={() => setShowConfirm(true)}
            className="text-red-600 hover:text-red-700 text-sm font-medium"
          >
            刪除
          </button>
        ) : (
          <div className="flex space-x-2">
            <Button
              onClick={handleDelete}
              isLoading={isLoading}
              className="bg-red-600 hover:bg-red-700 text-white px-3 py-1 text-sm"
            >
              確認
            </Button>
            <button
              onClick={() => setShowConfirm(false)}
              className="text-gray-600 hover:text-gray-700 px-3 py-1 text-sm"
            >
              取消
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
