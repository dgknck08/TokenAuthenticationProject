import React, { useState, useEffect } from 'react';
import { Menu, X, ShoppingCart, Heart, User, Search, Bell, ChevronDown, LogOut, Settings, Package } from 'lucide-react';

const Navbar = () => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [cartCount, setCartCount] = useState(3);
  
  // Mock auth state
  const isAuthenticated = true;
  const user = {
    firstName: 'Ahmet',
    lastName: 'Yılmaz',
    role: 'ADMIN'
  };

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <>
      <nav
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          isScrolled
            ? 'bg-white/80 backdrop-blur-xl shadow-lg'
            : 'bg-white'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-20">
            {/* Logo */}
            <div className="flex items-center gap-12">
              <a href="/" className="flex items-center gap-3 group">
                <div className="w-12 h-12 bg-gradient-to-br from-blue-600 to-purple-600 rounded-2xl flex items-center justify-center shadow-lg group-hover:shadow-xl transition-all transform group-hover:scale-105">
                  <span className="text-white font-bold text-2xl">E</span>
                </div>
                <span className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent hidden sm:block">
                  E-Ticaret
                </span>
              </a>

              {/* Desktop Menu */}
              <div className="hidden lg:flex items-center gap-8">
                <a
                  href="/"
                  className="relative text-gray-700 hover:text-blue-600 font-medium transition-colors group"
                >
                  Ana Sayfa
                  <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-600 to-purple-600 group-hover:w-full transition-all duration-300"></span>
                </a>
                <a
                  href="/products"
                  className="relative text-gray-700 hover:text-blue-600 font-medium transition-colors group"
                >
                  Ürünler
                  <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-600 to-purple-600 group-hover:w-full transition-all duration-300"></span>
                </a>
                {isAuthenticated && user?.role === 'ADMIN' && (
                  <a
                    href="/admin"
                    className="relative text-gray-700 hover:text-blue-600 font-medium transition-colors group"
                  >
                    Admin Panel
                    <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-gradient-to-r from-blue-600 to-purple-600 group-hover:w-full transition-all duration-300"></span>
                  </a>
                )}
              </div>
            </div>

            {/* Right Side */}
            <div className="flex items-center gap-4">
              {/* Search Button */}
              <button className="hidden sm:flex items-center gap-2 px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-xl transition-colors">
                <Search className="w-5 h-5 text-gray-600" />
                <span className="text-sm text-gray-600 hidden md:block">Ara...</span>
              </button>

              {/* Icons */}
              <div className="hidden md:flex items-center gap-2">
                {/* Notifications */}
                <button className="relative p-3 hover:bg-gray-100 rounded-xl transition-colors">
                  <Bell className="w-6 h-6 text-gray-600" />
                  <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full"></span>
                </button>

                {/* Favorites */}
                <button className="relative p-3 hover:bg-gray-100 rounded-xl transition-colors">
                  <Heart className="w-6 h-6 text-gray-600" />
                  <span className="absolute top-1.5 right-1.5 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-semibold">
                    2
                  </span>
                </button>

                {/* Cart */}
                <button className="relative p-3 hover:bg-gray-100 rounded-xl transition-colors">
                  <ShoppingCart className="w-6 h-6 text-gray-600" />
                  {cartCount > 0 && (
                    <span className="absolute top-1.5 right-1.5 w-5 h-5 bg-blue-600 text-white text-xs rounded-full flex items-center justify-center font-semibold">
                      {cartCount}
                    </span>
                  )}
                </button>
              </div>

              {/* User Menu */}
              {isAuthenticated ? (
                <div className="relative">
                  <button
                    onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                    className="hidden md:flex items-center gap-3 px-4 py-2 hover:bg-gray-100 rounded-xl transition-colors"
                  >
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-purple-500 rounded-full flex items-center justify-center shadow-md">
                      <span className="text-white font-semibold text-sm">
                        {user?.firstName?.charAt(0)}{user?.lastName?.charAt(0)}
                      </span>
                    </div>
                    <div className="text-left">
                      <p className="text-sm font-semibold text-gray-900">{user?.firstName}</p>
                      <p className="text-xs text-gray-500">{user?.role}</p>
                    </div>
                    <ChevronDown className={`w-4 h-4 text-gray-600 transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`} />
                  </button>

                  {/* Dropdown Menu */}
                  {isUserMenuOpen && (
                    <div className="absolute right-0 mt-2 w-64 bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden">
                      <div className="p-4 bg-gradient-to-r from-blue-50 to-purple-50">
                        <p className="font-semibold text-gray-900">{user?.firstName} {user?.lastName}</p>
                        <p className="text-sm text-gray-600">@{user?.firstName?.toLowerCase()}</p>
                      </div>
                      <div className="p-2">
                        <a href="/profile" className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 rounded-xl transition-colors">
                          <User className="w-5 h-5 text-gray-600" />
                          <span className="text-gray-700">Profilim</span>
                        </a>
                        <a href="/orders" className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 rounded-xl transition-colors">
                          <Package className="w-5 h-5 text-gray-600" />
                          <span className="text-gray-700">Siparişlerim</span>
                        </a>
                        <a href="/settings" className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 rounded-xl transition-colors">
                          <Settings className="w-5 h-5 text-gray-600" />
                          <span className="text-gray-700">Ayarlar</span>
                        </a>
                        <hr className="my-2 border-gray-200" />
                        <button className="w-full flex items-center gap-3 px-4 py-3 hover:bg-red-50 rounded-xl transition-colors text-red-600">
                          <LogOut className="w-5 h-5" />
                          <span>Çıkış Yap</span>
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="hidden md:flex items-center gap-3">
                  <a
                    href="/login"
                    className="px-6 py-2 text-gray-700 hover:text-blue-600 font-medium transition-colors"
                  >
                    Giriş Yap
                  </a>
                  <a
                    href="/register"
                    className="px-6 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-xl font-medium shadow-lg hover:shadow-xl transform hover:-translate-y-0.5 transition-all"
                  >
                    Kayıt Ol
                  </a>
                </div>
              )}

              {/* Mobile Menu Button */}
              <button
                onClick={() => setIsMenuOpen(!isMenuOpen)}
                className="lg:hidden p-2 hover:bg-gray-100 rounded-xl transition-colors"
              >
                {isMenuOpen ? (
                  <X className="w-6 h-6 text-gray-600" />
                ) : (
                  <Menu className="w-6 h-6 text-gray-600" />
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Mobile Menu */}
        {isMenuOpen && (
          <div className="lg:hidden border-t border-gray-200 bg-white/95 backdrop-blur-xl">
            <div className="px-4 py-6 space-y-3">
              <a href="/" className="block px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                Ana Sayfa
              </a>
              <a href="/products" className="block px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                Ürünler
              </a>
              {isAuthenticated && user?.role === 'ADMIN' && (
                <a href="/admin" className="block px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                  Admin Panel
                </a>
              )}
              
              {/* Mobile Icons */}
              <div className="flex items-center gap-2 pt-4 border-t">
                <button className="flex-1 flex items-center justify-center gap-2 px-4 py-3 hover:bg-gray-50 rounded-xl transition-colors">
                  <Heart className="w-5 h-5 text-gray-600" />
                  <span className="text-sm">Favoriler</span>
                </button>
                <button className="flex-1 flex items-center justify-center gap-2 px-4 py-3 hover:bg-gray-50 rounded-xl transition-colors">
                  <ShoppingCart className="w-5 h-5 text-gray-600" />
                  <span className="text-sm">Sepet</span>
                </button>
              </div>

              {isAuthenticated ? (
                <>
                  <a href="/profile" className="block px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                    Profil ({user?.firstName})
                  </a>
                  <button className="w-full px-4 py-3 bg-red-500 text-white rounded-xl hover:bg-red-600 transition-colors text-left font-medium">
                    Çıkış Yap
                  </button>
                </>
              ) : (
                <>
                  <a href="/login" className="block px-4 py-3 text-gray-700 hover:bg-gray-50 rounded-xl font-medium transition-colors">
                    Giriş Yap
                  </a>
                  <a
                    href="/register"
                    className="block px-4 py-3 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-xl hover:shadow-lg transition-all text-center font-medium"
                  >
                    Kayıt Ol
                  </a>
                </>
              )}
            </div>
          </div>
        )}
      </nav>

      {/* Spacer */}
      <div className="h-20"></div>
    </>
  );
};

export default Navbar;