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

interface BackendUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  status?: string;
  createdAt: string;
  updatedAt: string;
}

interface BackendUsersPage {
  content?: BackendUser[];
}

interface BackendPagedResponse extends Omit<PagedResponse<BackendUser>, 'data'> {
  data: BackendUsersPage | BackendUser[];
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
    const response = await apiClient.get<BackendPagedResponse>('/users', { params });
    const payload = response.data;
    const backendRows = Array.isArray(payload.data)
      ? payload.data
      : payload.data?.content || [];

    const data: User[] = backendRows.map((user) => ({
      id: user.id,
      email: user.email,
      firstName: user.firstName,
      lastName: user.lastName,
      phoneNumber: user.phone,
      active: user.status === 'ACTIVE',
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    }));

    return {
      ...payload,
      data,
    };
  },

  getById: async (id: number): Promise<ApiResponse<User>> => {
    const response = await apiClient.get(`/users/${id}`);
    return response.data;
  },

  create: async (data: CreateUserRequest): Promise<ApiResponse<User>> => {
    const payload = {
      ...data,
      phone: data.phoneNumber,
    };
    const response = await apiClient.post('/users', payload);
    return response.data;
  },

  update: async (id: number, data: UpdateUserRequest): Promise<ApiResponse<User>> => {
    const payload = {
      ...data,
      phone: data.phoneNumber,
    };
    const response = await apiClient.put(`/users/${id}`, payload);
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
