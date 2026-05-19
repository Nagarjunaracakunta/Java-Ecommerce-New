export interface LoginRequest { username: string; password: string; }
export interface RegisterRequest { username: string; password: string; role?: string; }
export interface AuthResponse { token: string; tokenType: string; expiresIn: number; }

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  category: string;
}

export interface CartItem { productId: number; quantity: number; }
export interface CartItemResponse {
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  subtotal: number;
}
export interface CartResponse { username: string; items: CartItemResponse[]; total: number; }

export interface OrderItem { productId: number; quantity: number; }
export interface OrderItemResponse {
  productId: number;
  productName: string;
  price: number;
  quantity: number;
  subtotal: number;
}
export interface Order {
  id: number;
  username: string;
  status: string;
  totalAmount: number;
  shippingAddress: string;
  items: OrderItemResponse[];
  createdAt: string;
  updatedAt: string;
}
export interface PlaceOrderRequest { shippingAddress: string; }

export interface Payment {
  id: number;
  orderId: number;
  username: string;
  amount: number;
  status: string;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Notification {
  id: string;
  username: string;
  type: string;
  message: string;
  orderId: number;
  paymentId: number;
  amount: number;
  read: boolean;
  createdAt: string;
}