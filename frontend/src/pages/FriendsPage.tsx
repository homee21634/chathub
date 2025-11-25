import React, { useState } from 'react';
import { SearchUser } from '../components/friend/SearchUser';
import { FriendList } from '../components/friend/FriendList';
import { FriendRequestList } from '../components/friend/FriendRequestList';

type TabType = 'friends' | 'requests';

export const FriendsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('friends');

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto py-8 px-4">
        {/* 標題 */}
        <div className="mb-6">
          <h1 className="text-3xl font-bold text-gray-900">好友管理</h1>
          <p className="text-gray-600 mt-1">管理您的好友和好友請求</p>
        </div>

        {/* 搜尋用戶 */}
        <div className="mb-6">
          <SearchUser />
        </div>

        {/* Tab 切換 */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="flex border-b">
            <button
              onClick={() => setActiveTab('friends')}
              className={`flex-1 px-6 py-3 font-medium ${
                activeTab === 'friends'
                  ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              好友列表
            </button>
            <button
              onClick={() => setActiveTab('requests')}
              className={`flex-1 px-6 py-3 font-medium ${
                activeTab === 'requests'
                  ? 'text-blue-600 border-b-2 border-blue-600 bg-blue-50'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
              }`}
            >
              好友請求
            </button>
          </div>

          {/* Tab 內容 */}
          <div className="p-6">
            {activeTab === 'friends' ? <FriendList /> : <FriendRequestList />}
          </div>
        </div>
      </div>
    </div>
  );
};
