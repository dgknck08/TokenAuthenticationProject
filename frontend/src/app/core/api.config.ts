const LOCAL_API_BASE = 'http://localhost:8080/api';

export function resolveApiBaseUrl(): string {
  if (typeof window === 'undefined') {
    return LOCAL_API_BASE;
  }

  const { origin, hostname, port } = window.location;
  const isAngularDevServer = port === '3000' && (hostname === 'localhost' || hostname === '127.0.0.1');

  return isAngularDevServer ? LOCAL_API_BASE : `${origin}/api`;
}

export const API_BASE_URL = resolveApiBaseUrl();
