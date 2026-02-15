"use client";

import { create } from "zustand";

interface AuthState {
  accessToken: string | null;
  setAccessToken: (token: string | null) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  setAccessToken: (token) => {
    if (typeof window !== "undefined") {
      if (token) window.localStorage.setItem("access_token", token);
      else window.localStorage.removeItem("access_token");
    }
    set({ accessToken: token });
  },
  clear: () => {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem("access_token");
    }
    set({ accessToken: null });
  }
}));

export function hydrateTokenFromStorage() {
  if (typeof window === "undefined") return;
  const token = window.localStorage.getItem("access_token");
  if (token) useAuthStore.getState().setAccessToken(token);
}
