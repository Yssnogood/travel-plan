import { apiClient, ApiResponse, PagedResponse } from './client';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  dateOfBirth?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  dateOfBirth?: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  dateOfBirth?: string;
}

export interface UserSearchParams {
  page?: number;
  size?: number;
  search?: string;
  active?: boolean;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export const userService = {
  getAll: async (params: UserSearchParams = {}): Promise<PagedResponse<User>> => {
    const response = await apiClient.get('/users', { params });
    return response.data;
  },

  getById: async (id: number): Promise<ApiResponse<User>> => {
    const response = await apiClient.get(`/users/${id}`);
    return response.data;
  },

  create: async (data: CreateUserRequest): Promise<ApiResponse<User>> => {
    const response = await apiClient.post('/users', data);
    return response.data;
  },

  update: async (id: number, data: UpdateUserRequest): Promise<ApiResponse<User>> => {
    const response = await apiClient.put(`/users/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await apiClient.delete(`/users/${id}`);
    return response.data;
  },

  deactivate: async (id: number): Promise<ApiResponse<User>> => {
    const response = await apiClient.patch(`/users/${id}/deactivate`);
    return response.data;
  },

  activate: async (id: number): Promise<ApiResponse<User>> => {
    const response = await apiClient.patch(`/users/${id}/activate`);
    return response.data;
  },
};
