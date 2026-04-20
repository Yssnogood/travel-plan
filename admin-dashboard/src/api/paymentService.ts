import { apiClient, ApiResponse, PagedResponse } from './client';

export interface PaymentMethod {
  id: number;
  userId: number;
  type: 'CREDIT_CARD' | 'DEBIT_CARD' | 'STRIPE' | 'PAYPAL';
  provider: string;
  lastFourDigits?: string;
  expiryMonth?: number;
  expiryYear?: number;
  isDefault: boolean;
  active: boolean;
  createdAt: string;
}

export interface Payment {
  id: number;
  userId: number;
  travelId?: number;
  paymentMethodId: number;
  amount: number;
  currency: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'REFUNDED' | 'CANCELLED';
  transactionId?: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentMethodRequest {
  userId: number;
  type: string;
  provider: string;
  cardNumber?: string;
  expiryMonth?: number;
  expiryYear?: number;
  isDefault?: boolean;
}

export interface CreatePaymentRequest {
  userId: number;
  travelId?: number;
  paymentMethodId: number;
  amount: number;
  currency?: string;
  description?: string;
}

export interface PaymentSearchParams {
  page?: number;
  size?: number;
  userId?: number;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export const paymentService = {
  // Payment Methods
  getAllMethods: async (userId?: number): Promise<PagedResponse<PaymentMethod>> => {
    const response = await apiClient.get('/payment-methods', { params: { userId } });
    return response.data;
  },

  getMethodById: async (id: number): Promise<ApiResponse<PaymentMethod>> => {
    const response = await apiClient.get(`/payment-methods/${id}`);
    return response.data;
  },

  createMethod: async (data: CreatePaymentMethodRequest): Promise<ApiResponse<PaymentMethod>> => {
    const response = await apiClient.post('/payment-methods', data);
    return response.data;
  },

  deleteMethod: async (id: number): Promise<ApiResponse<void>> => {
    const response = await apiClient.delete(`/payment-methods/${id}`);
    return response.data;
  },

  setDefaultMethod: async (id: number): Promise<ApiResponse<PaymentMethod>> => {
    const response = await apiClient.patch(`/payment-methods/${id}/default`);
    return response.data;
  },

  // Payments
  getAllPayments: async (params: PaymentSearchParams = {}): Promise<PagedResponse<Payment>> => {
    const response = await apiClient.get('/payments', { params });
    return response.data;
  },

  getPaymentById: async (id: number): Promise<ApiResponse<Payment>> => {
    const response = await apiClient.get(`/payments/${id}`);
    return response.data;
  },

  createPayment: async (data: CreatePaymentRequest): Promise<ApiResponse<Payment>> => {
    const response = await apiClient.post('/payments', data);
    return response.data;
  },

  processPayment: async (id: number): Promise<ApiResponse<Payment>> => {
    const response = await apiClient.post(`/payments/${id}/process`);
    return response.data;
  },

  refundPayment: async (id: number, amount?: number): Promise<ApiResponse<Payment>> => {
    const response = await apiClient.post(`/payments/${id}/refund`, { amount });
    return response.data;
  },

  cancelPayment: async (id: number): Promise<ApiResponse<Payment>> => {
    const response = await apiClient.post(`/payments/${id}/cancel`);
    return response.data;
  },
};
