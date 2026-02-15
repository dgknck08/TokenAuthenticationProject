"use client";

import { apiClient } from "@/lib/http";
import { AuthResponse, UserProfile } from "@/types/api";

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

export const authApi = {
  login: async (payload: LoginRequest) => {
    const { data } = await apiClient.post<AuthResponse>("/auth/login", payload);
    return data;
  },
  register: async (payload: RegisterRequest) => {
    const { data } = await apiClient.post<AuthResponse>("/auth/register", payload);
    return data;
  },
  logout: async () => {
    await apiClient.post("/auth/logout");
  },
  refreshToken: async () => {
    const { data } = await apiClient.post<AuthResponse>("/auth/refresh-token");
    return data;
  },
  me: async () => {
    const { data } = await apiClient.get<UserProfile>("/profile");
    return data;
  }
};
