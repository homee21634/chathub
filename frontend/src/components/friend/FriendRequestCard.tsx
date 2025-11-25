import React from 'react';
import { FriendRequest } from '../../types/friend';
import { Button } from '../common/Button';
import { useFriendStore } from '../../store/friendStore';

interface FriendRequestCardProps {
  request: FriendRequest;
}

export const FriendRequestCard: React.FC<FriendRequestCardProps> = ({ request }) => {
  const { acceptFriendRequest, rejectFriendRequest, isLoading } = useFriendStore();

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('zh-TW', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="bg-white p-4 rounded-lg shadow">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          {/* 頭像 */}
          <div className="w-12 h-12 bg-gradient-to-br from-purple-400 to-purple-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
            {request.fromUsername.charAt(0).toUpperCase()}
          </div>

          <div>
            <h3 className="font-semibold text-gray-900">{request.fromUsername}</h3>
            <p className="text-sm text-gray-500">{formatDate(request.createdAt)}</p>
          </div>
        </div>

        {/* 操作按鈕 */}
        {request.status === 'PENDING' && (
          <div className="flex space-x-2">
            <Button
              onClick={() => acceptFriendRequest(request.requestId)}
              isLoading={isLoading}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 text-sm"
            >
              接受
            </Button>
            <Button
              onClick={() => rejectFriendRequest(request.requestId)}
              isLoading={isLoading}
              className="bg-gray-200 hover:bg-gray-300 text-gray-700 px-4 py-2 text-sm"
            >
              拒絕
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
