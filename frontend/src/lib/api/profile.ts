"use client";

import { apiClient } from "@/lib/http";
import { UserProfile } from "@/types/api";

export const profileApi = {
  get: async () => {
    const { data } = await apiClient.get<UserProfile>("/profile");
    return data;
  },
  update: async (payload: Partial<UserProfile>) => {
    const { data } = await apiClient.put<UserProfile>("/profile", payload);
    return data;
  }
};
