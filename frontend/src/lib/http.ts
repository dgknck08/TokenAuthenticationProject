"use client";

import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/store/auth-store";
import { ApiErrorResponse } from "@/types/api";

type RetriableRequest = InternalAxiosRequestConfig & { _retry?: boolean };

export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api",
  timeout: 20_000,
  withCredentials: true,
  headers: { "Content-Type": "application/json" }
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const original = error.config as RetriableRequest | undefined;
    if (!original) return Promise.reject(error);

    const is401 = error.response?.status === 401;
    const isAuthRoute = original.url?.includes("/auth/login") || original.url?.includes("/auth/register");
    const isRefreshRoute = original.url?.includes("/auth/refresh-token");

    if (is401 && !original._retry && !isAuthRoute && !isRefreshRoute) {
      original._retry = true;
      try {
        const refresh = await apiClient.post("/auth/refresh-token");
        const token = refresh.data?.accessToken as string | undefined;
        if (!token) throw new Error("Refresh token failed");
        useAuthStore.getState().setAccessToken(token);
        original.headers.Authorization = `Bearer ${token}`;
        return apiClient(original);
      } catch (refreshError) {
        useAuthStore.getState().clear();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export function mapApiError(error: unknown, fallback: string) {
  const axiosError = error as AxiosError<ApiErrorResponse>;
  return axiosError.response?.data?.message || fallback;
}
