import { z } from 'zod';

export const loginSchema = z.object({
  username: z.string().min(4, '帳號至少 4 個字元').max(20, '帳號最多 20 個字元'),
  password: z.string().min(1, '密碼不能為空'),
  rememberMe: z.boolean().optional(),
});

export const registerSchema = z.object({
  username: z
    .string()
    .min(4, '帳號至少 4 個字元')
    .max(20, '帳號最多 20 個字元')
    .regex(/^[a-zA-Z0-9_]+$/, '帳號只能包含字母、數字和底線'),
  password: z
    .string()
    .min(8, '密碼至少 8 個字元')
    .regex(/[A-Z]/, '密碼需包含至少一個大寫字母')
    .regex(/[a-z]/, '密碼需包含至少一個小寫字母')
    .regex(/[0-9]/, '密碼需包含至少一個數字')
    .regex(/[@$!%*?&]/, '密碼需包含至少一個符號 (@$!%*?&)'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: '密碼不一致',
  path: ['confirmPassword'],
});

export type LoginFormData = z.infer<typeof loginSchema>;
export type RegisterFormData = z.infer<typeof registerSchema>;
