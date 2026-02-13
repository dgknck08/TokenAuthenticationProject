import axios, { AxiosResponse } from 'axios';

const API_BASE_URL =
  process.env.REACT_APP_API_URL || '';

let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

export const clearAccessToken = () => {
  accessToken = null;
};

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshResponse = await apiClient.post('/api/auth/refresh-token');
        const { accessToken: refreshedAccessToken } = refreshResponse.data;
        setAccessToken(refreshedAccessToken);
        if (!originalRequest.headers) originalRequest.headers = {};
        originalRequest.headers.Authorization = `Bearer ${refreshedAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        clearAccessToken();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  accessToken: string;
  username: string;
  email: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  createdAt: string;
  updatedAt: string;
}

export const authApi = {
  login: (data: LoginRequest): Promise<AxiosResponse<AuthResponse>> =>
    apiClient.post('/api/auth/login', data),
  register: (data: RegisterRequest): Promise<AxiosResponse<AuthResponse>> =>
    apiClient.post('/api/auth/register', data),
  logout: (): Promise<AxiosResponse> =>
    apiClient.post('/api/auth/logout'),
  logoutAll: (): Promise<AxiosResponse> =>
    apiClient.post('/api/auth/logout-all'),
  verifyToken: (): Promise<AxiosResponse<{ valid: boolean; username: string; roles: string[] }>> =>
    apiClient.post('/api/auth/verify-token'),
  refreshToken: (): Promise<AxiosResponse<{ accessToken: string }>> =>
    apiClient.post('/api/auth/refresh-token'),
};

export const profileApi = {
  getProfile: (): Promise<AxiosResponse<User>> =>
    apiClient.get('/api/profile'),
  updateProfile: (data: Partial<User>): Promise<AxiosResponse<User>> =>
    apiClient.put('/api/profile', data),
  deleteProfile: (): Promise<AxiosResponse> =>
    apiClient.delete('/api/profile'),
};

export const productsApi = {
  getProducts: (): Promise<AxiosResponse<Product[]>> =>
    apiClient.get('/api/products'),
  getProduct: (id: string): Promise<AxiosResponse<Product>> =>
    apiClient.get(`/api/products/${id}`),
  createProduct: (data: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>): Promise<AxiosResponse<Product>> =>
    apiClient.post('/api/products', data),
  updateProduct: (id: string, data: Partial<Omit<Product, 'id' | 'createdAt' | 'updatedAt'>>): Promise<AxiosResponse<Product>> =>
    apiClient.put(`/api/products/${id}`, data),
  deleteProduct: (id: string): Promise<AxiosResponse> =>
    apiClient.delete(`/api/products/${id}`),
};

export const adminApi = {
  getCacheStats: (): Promise<AxiosResponse<any>> =>
    apiClient.get('/api/admin/cache/stats'),
  clearUserCache: (username: string): Promise<AxiosResponse> =>
    apiClient.post(`/api/admin/cache/clear/${username}`),
  clearAllCache: (): Promise<AxiosResponse> =>
    apiClient.post('/api/admin/cache/clear/all'),
};
