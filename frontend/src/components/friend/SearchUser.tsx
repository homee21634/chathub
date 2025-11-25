import React, { useState } from 'react';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import { useFriendStore } from '../../store/friendStore';

export const SearchUser: React.FC = () => {
  const [username, setUsername] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const { sendFriendRequest, error, clearError, isLoading } = useFriendStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim()) return;

    setSuccessMessage('');
    clearError();

    try {
      await sendFriendRequest(username);
      setSuccessMessage('好友請求已發送！');
      setUsername('');
      setTimeout(() => setSuccessMessage(''), 3000);
    } catch (err) {
      // 錯誤已在 store 中處理
    }
  };

  return (
    <div className="bg-white p-6 rounded-lg shadow">
      <h2 className="text-xl font-semibold mb-4">新增好友</h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <Input
          label="搜尋用戶"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="輸入用戶名稱"
          error={error || undefined}
        />

        {successMessage && (
          <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-green-600 text-sm">
            {successMessage}
          </div>
        )}

        <Button type="submit" isLoading={isLoading} className="w-full">
          發送好友請求
        </Button>
      </form>
    </div>
  );
};
