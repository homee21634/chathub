import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useNavigate } from 'react-router-dom';
import { Input } from '../common/Input';
import { Button } from '../common/Button';
import { registerSchema, RegisterFormData } from '../../utils/validation';
import { authService } from '../../services/authService';

export const RegisterForm: React.FC = () => {
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = React.useState(false);
  const [error, setError] = React.useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setError('');

    try {
      await authService.register(data);
      alert('註冊成功！請使用帳號密碼登入');
      navigate('/login');
    } catch (err: any) {
      setError(err.response?.data?.message || '註冊失敗，請稍後再試');
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

      <Input
        label="確認密碼"
        type="password"
        {...register('confirmPassword')}
        error={errors.confirmPassword?.message}
        placeholder="請再次輸入密碼"
      />

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {error}
        </div>
      )}

      <Button type="submit" isLoading={isLoading} className="w-full">
        註冊
      </Button>
    </form>
  );
};
