import React, { useState } from 'react';
import { ArrowLeft, ShoppingCart, Heart, Share2, Star, Check, Truck, Shield, RotateCcw, Minus, Plus, Zap } from 'lucide-react';

const ProductDetail = () => {
  const [quantity, setQuantity] = useState(1);
  const [selectedImage, setSelectedImage] = useState(0);
  const [isFavorite, setIsFavorite] = useState(false);
  const [activeTab, setActiveTab] = useState('description');
  const [showNotification, setShowNotification] = useState(false);

  // Mock data
  const product = {
    id: '1',
    name: 'Premium Wireless Headphones',
    description: 'Yüksek kaliteli ses deneyimi sunan, aktif gürültü önleme özellikli kablosuz kulaklık. 30 saat pil ömrü ve hızlı şarj desteği ile kesintisiz müzik keyfi.',
    price: 1299,
    originalPrice: 1599,
    category: 'Elektronik',
    rating: 4.8,
    reviews: 2847,
    inStock: true,
    images: ['📦', '🎧', '📱', '🔊'],
    features: [
      'Aktif Gürültü Önleme (ANC)',
      '30 Saat Pil Ömrü',
      'Hızlı Şarj (10dk = 3 saat)',
      'Bluetooth 5.2',
      'Premium Ses Kalitesi',
      'Hafif ve Rahat Tasarım'
    ],
    specs: {
      'Marka': 'Premium Audio',
      'Model': 'PA-WH1000',
      'Bağlantı': 'Bluetooth 5.2',
      'Pil Ömrü': '30 Saat',
      'Şarj Süresi': '2 Saat',
      'Ağırlık': '250g'
    }
  };

  const handleAddToCart = () => {
    setShowNotification(true);
    setTimeout(() => setShowNotification(false), 3000);
  };

  const discount = Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100);

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-blue-50">
      {/* Notification */}
      {showNotification && (
        <div className="fixed top-24 right-4 z-50 animate-slide-in-right">
          <div className="bg-green-500 text-white px-6 py-4 rounded-xl shadow-2xl flex items-center gap-3">
            <div className="w-8 h-8 bg-white rounded-full flex items-center justify-center">
              <Check className="w-5 h-5 text-green-500" />
            </div>
            <div>
              <p className="font-semibold">Sepete Eklendi!</p>
              <p className="text-sm text-green-100">{quantity} adet ürün</p>
            </div>
          </div>
        </div>
      )}

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Back Button */}
        <button className="group mb-8 flex items-center gap-2 text-gray-600 hover:text-blue-600 transition-colors">
          <ArrowLeft className="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
          <span className="font-medium">Ürünlere Dön</span>
        </button>

        <div className="grid lg:grid-cols-2 gap-12">
          {/* Left Column - Images */}
          <div className="space-y-4">
            {/* Main Image */}
            <div className="relative aspect-square bg-gradient-to-br from-blue-400 to-purple-500 rounded-3xl overflow-hidden shadow-2xl">
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-9xl animate-float">{product.images[selectedImage]}</span>
              </div>
              
              {/* Badges */}
              <div className="absolute top-6 left-6 flex flex-col gap-2">
                {discount > 0 && (
                  <div className="px-4 py-2 bg-red-500 text-white rounded-full font-bold text-sm shadow-lg">
                    %{discount} İndirim
                  </div>
                )}
                {product.inStock && (
                  <div className="px-4 py-2 bg-green-500 text-white rounded-full font-semibold text-sm shadow-lg">
                    Stokta
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div className="absolute top-6 right-6 flex flex-col gap-2">
                <button
                  onClick={() => setIsFavorite(!isFavorite)}
                  className="p-3 bg-white/90 backdrop-blur-sm rounded-full shadow-lg hover:scale-110 transition-transform"
                >
                  <Heart
                    className={`w-6 h-6 ${
                      isFavorite ? 'fill-red-500 text-red-500' : 'text-gray-600'
                    }`}
                  />
                </button>
                <button className="p-3 bg-white/90 backdrop-blur-sm rounded-full shadow-lg hover:scale-110 transition-transform">
                  <Share2 className="w-6 h-6 text-gray-600" />
                </button>
              </div>
            </div>

            {/* Thumbnail Images */}
            <div className="grid grid-cols-4 gap-4">
              {product.images.map((img, idx) => (
                <button
                  key={idx}
                  onClick={() => setSelectedImage(idx)}
                  className={`aspect-square bg-gradient-to-br from-gray-100 to-gray-200 rounded-2xl flex items-center justify-center text-4xl hover:scale-105 transition-all ${
                    selectedImage === idx
                      ? 'ring-4 ring-blue-500 shadow-lg'
                      : 'hover:shadow-md'
                  }`}
                >
                  {img}
                </button>
              ))}
            </div>
          </div>

          {/* Right Column - Details */}
          <div className="space-y-6">
            {/* Category */}
            <div className="inline-flex items-center gap-2 px-4 py-2 bg-blue-100 text-blue-700 rounded-full font-semibold">
              <Zap className="w-4 h-4" />
              {product.category}
            </div>

            {/* Title */}
            <h1 className="text-4xl lg:text-5xl font-bold text-gray-900 leading-tight">
              {product.name}
            </h1>

            {/* Rating */}
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                {[...Array(5)].map((_, i) => (
                  <Star
                    key={i}
                    className={`w-5 h-5 ${
                      i < Math.floor(product.rating)
                        ? 'fill-yellow-400 text-yellow-400'
                        : 'text-gray-300'
                    }`}
                  />
                ))}
              </div>
              <span className="text-lg font-semibold text-gray-900">{product.rating}</span>
              <span className="text-gray-500">({product.reviews.toLocaleString()} değerlendirme)</span>
            </div>

            {/* Price */}
            <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-2xl p-6">
              <div className="flex items-end gap-4 mb-2">
                <span className="text-5xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                  ₺{product.price.toLocaleString()}
                </span>
                {discount > 0 && (
                  <span className="text-2xl text-gray-400 line-through mb-2">
                    ₺{product.originalPrice.toLocaleString()}
                  </span>
                )}
              </div>
              {discount > 0 && (
                <p className="text-green-600 font-semibold">
                  ₺{(product.originalPrice - product.price).toLocaleString()} tasarruf ediyorsunuz!
                </p>
              )}
            </div>

            {/* Quantity Selector */}
            <div className="bg-white rounded-2xl p-6 shadow-lg">
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                Adet Seçin
              </label>
              <div className="flex items-center gap-4">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="w-12 h-12 bg-gray-100 rounded-xl hover:bg-gray-200 transition-colors flex items-center justify-center font-bold text-gray-700"
                >
                  <Minus className="w-5 h-5" />
                </button>
                <span className="text-2xl font-bold text-gray-900 w-16 text-center">
                  {quantity}
                </span>
                <button
                  onClick={() => setQuantity(quantity + 1)}
                  className="w-12 h-12 bg-gray-100 rounded-xl hover:bg-gray-200 transition-colors flex items-center justify-center font-bold text-gray-700"
                >
                  <Plus className="w-5 h-5" />
                </button>
                <div className="ml-auto text-right">
                  <p className="text-sm text-gray-500">Toplam</p>
                  <p className="text-2xl font-bold text-gray-900">
                    ₺{(product.price * quantity).toLocaleString()}
                  </p>
                </div>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="space-y-3">
              <button
                onClick={handleAddToCart}
                className="group w-full bg-gradient-to-r from-blue-600 to-purple-600 text-white py-5 rounded-2xl font-bold text-lg shadow-xl hover:shadow-2xl transform hover:-translate-y-1 transition-all flex items-center justify-center gap-3"
              >
                <ShoppingCart className="w-6 h-6 group-hover:rotate-12 transition-transform" />
                Sepete Ekle
              </button>
              <button className="w-full bg-green-600 text-white py-5 rounded-2xl font-bold text-lg shadow-lg hover:shadow-xl hover:bg-green-700 transition-all">
                Hemen Satın Al
              </button>
            </div>

            {/* Features */}
            <div className="bg-white rounded-2xl p-6 shadow-lg">
              <h3 className="font-bold text-gray-900 text-lg mb-4">Öne Çıkan Özellikler</h3>
              <div className="grid grid-cols-2 gap-3">
                {product.features.map((feature, idx) => (
                  <div key={idx} className="flex items-start gap-2">
                    <div className="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                      <Check className="w-3 h-3 text-green-600" />
                    </div>
                    <span className="text-sm text-gray-700">{feature}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Trust Badges */}
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-white rounded-xl p-4 text-center shadow-md hover:shadow-lg transition-shadow">
                <Truck className="w-8 h-8 text-blue-600 mx-auto mb-2" />
                <p className="text-xs font-semibold text-gray-900">Ücretsiz Kargo</p>
              </div>
              <div className="bg-white rounded-xl p-4 text-center shadow-md hover:shadow-lg transition-shadow">
                <Shield className="w-8 h-8 text-green-600 mx-auto mb-2" />
                <p className="text-xs font-semibold text-gray-900">2 Yıl Garanti</p>
              </div>
              <div className="bg-white rounded-xl p-4 text-center shadow-md hover:shadow-lg transition-shadow">
                <RotateCcw className="w-8 h-8 text-purple-600 mx-auto mb-2" />
                <p className="text-xs font-semibold text-gray-900">14 Gün İade</p>
              </div>
            </div>
          </div>
        </div>

        {/* Tabs Section */}
        <div className="mt-16 bg-white rounded-3xl shadow-xl p-8">
          {/* Tab Headers */}
          <div className="flex gap-4 border-b border-gray-200 mb-8">
            {['description', 'specs', 'reviews'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`px-6 py-3 font-semibold transition-all ${
                  activeTab === tab
                    ? 'text-blue-600 border-b-2 border-blue-600'
                    : 'text-gray-500 hover:text-gray-700'
                }`}
              >
                {tab === 'description' && 'Açıklama'}
                {tab === 'specs' && 'Teknik Özellikler'}
                {tab === 'reviews' && 'Değerlendirmeler'}
              </button>
            ))}
          </div>

          {/* Tab Content */}
          <div className="prose max-w-none">
            {activeTab === 'description' && (
              <div className="space-y-4">
                <p className="text-gray-700 text-lg leading-relaxed">
                  {product.description}
                </p>
                <p className="text-gray-600 leading-relaxed">
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.
                </p>
              </div>
            )}

            {activeTab === 'specs' && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {Object.entries(product.specs).map(([key, value]) => (
                  <div key={key} className="flex justify-between p-4 bg-gray-50 rounded-xl">
                    <span className="font-semibold text-gray-700">{key}</span>
                    <span className="text-gray-900">{value}</span>
                  </div>
                ))}
              </div>
            )}

            {activeTab === 'reviews' && (
              <div className="text-center py-12">
                <Star className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                <p className="text-gray-500 text-lg">Henüz değerlendirme yapılmamış</p>
              </div>
            )}
          </div>
        </div>
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-20px); }
        }

        @keyframes slide-in-right {
          from {
            transform: translateX(100%);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }

        .animate-float {
          animation: float 3s ease-in-out infinite;
        }

        .animate-slide-in-right {
          animation: slide-in-right 0.3s ease-out;
        }
      `}</style>
    </div>
  );
};

export default ProductDetail;