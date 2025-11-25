import React from 'react';
import { Link } from 'react-router-dom';
import { RegisterForm } from '../components/auth/RegisterForm';

export const RegisterPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-xl shadow-lg">
        <div className="text-center">
          <h2 className="text-3xl font-bold text-gray-900">ChatHub</h2>
          <p className="mt-2 text-sm text-gray-600">建立新帳號</p>
        </div>

        <RegisterForm />

        <div className="text-center">
          <p className="text-sm text-gray-600">
            已有帳號？{' '}
            <Link to="/login" className="text-primary hover:text-blue-600 font-medium">
              立即登入
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};
