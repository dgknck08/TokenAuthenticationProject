import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { authApi, User, clearAccessToken, setAccessToken } from './api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (data: any) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    checkAuth();
  }, []);

  const mapRole = (roles: string[] = []): string => {
    if (roles.includes('ROLE_ADMIN')) return 'ADMIN';
    if (roles.includes('ROLE_MODERATOR')) return 'MODERATOR';
    return 'USER';
  };

  const checkAuth = async () => {
    try {
      const refresh = await authApi.refreshToken();
      setAccessToken(refresh.data.accessToken);
      const response = await authApi.verifyToken();
      if (response.data.valid) {
        setUser({
          id: '',
          username: response.data.username,
          email: '',
          firstName: '',
          lastName: '',
          role: mapRole(response.data.roles),
        });
      }
    } catch (error) {
      clearAccessToken();
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (username: string, password: string) => {
    const response = await authApi.login({ username, password });
    setAccessToken(response.data.accessToken);
    const verify = await authApi.verifyToken();
    const role = verify.data.valid ? mapRole(verify.data.roles) : 'USER';
    setUser({
      id: '',
      username: response.data.username,
      email: response.data.email,
      firstName: '',
      lastName: '',
      role,
    });
  };

  const register = async (data: any) => {
    const response = await authApi.register(data);
    setAccessToken(response.data.accessToken);
    const verify = await authApi.verifyToken();
    const role = verify.data.valid ? mapRole(verify.data.roles) : 'USER';
    setUser({
      id: '',
      username: response.data.username,
      email: response.data.email,
      firstName: data.firstName || '',
      lastName: data.lastName || '',
      role,
    });
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      clearAccessToken();
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
