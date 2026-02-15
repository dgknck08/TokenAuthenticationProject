"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { authApi, LoginRequest, RegisterRequest } from "@/lib/api/auth";
import { useAuthStore } from "@/store/auth-store";
import { queryKeys } from "@/lib/query-keys";

export function useMe() {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: authApi.me,
    retry: false
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: LoginRequest) => authApi.login(payload),
    onSuccess: (data) => {
      useAuthStore.getState().setAccessToken(data.accessToken);
      queryClient.invalidateQueries({ queryKey: queryKeys.me });
      queryClient.invalidateQueries({ queryKey: queryKeys.cart });
    }
  });
}

export function useRegister() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: RegisterRequest) => authApi.register(payload),
    onSuccess: (data) => {
      useAuthStore.getState().setAccessToken(data.accessToken);
      queryClient.invalidateQueries({ queryKey: queryKeys.me });
    }
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: authApi.logout,
    onSuccess: () => {
      useAuthStore.getState().clear();
      queryClient.clear();
    }
  });
}
