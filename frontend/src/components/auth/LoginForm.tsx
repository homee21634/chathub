import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import { loginSchema, LoginFormData } from '../../utils/validation';
import { authService } from '../../services/authService';
import { useAuthStore } from '../../store/authStore';

export const LoginForm: React.FC = () => {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);
  const [isLoading, setIsLoading] = React.useState(false);
  const [error, setError] = React.useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true);
    setError('');

    try {
      const response = await authService.login(data);
      setAuth(response);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || '登入失敗，請稍後再試');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <Input
        label="帳號"
        {...register('username')}
        error={errors.username?.message}
        placeholder="請輸入帳號"
      />

      <Input
        label="密碼"
        type="password"
        {...register('password')}
        error={errors.password?.message}
        placeholder="請輸入密碼"
      />

      <div className="flex items-center">
        <input
          type="checkbox"
          {...register('rememberMe')}
          className="h-4 w-4 text-primary focus:ring-primary border-gray-300 rounded"
        />
        <label className="ml-2 text-sm text-gray-700">記住我</label>
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {error}
        </div>
      )}

      <Button type="submit" isLoading={isLoading} className="w-full">
        登入
      </Button>
    </form>
  );
};
