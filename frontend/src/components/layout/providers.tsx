"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PropsWithChildren, useEffect, useState } from "react";
import { Toaster } from "sonner";
import { ThemeProvider } from "@/components/layout/theme-provider";
import { hydrateTokenFromStorage } from "@/store/auth-store";

export function Providers({ children }: PropsWithChildren) {
  const [queryClient] = useState(() =>
    new QueryClient({
      defaultOptions: {
        queries: {
          staleTime: 30_000,
          refetchOnWindowFocus: false
        }
      }
    })
  );

  useEffect(() => {
    hydrateTokenFromStorage();
  }, []);

  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        {children}
        <Toaster richColors position="top-right" />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
