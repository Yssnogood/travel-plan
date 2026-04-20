import { apiClient, ApiResponse, PagedResponse } from './client';

export interface Destination {
  id: number;
  name: string;
  country: string;
  description?: string;
}

export interface Activity {
  id: number;
  name: string;
  description?: string;
  estimatedDuration?: string;
}

export interface Travel {
  id: number;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  budget?: number;
  currency: string;
  status: 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  userId: number;
  destinations: Destination[];
  activities: Activity[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateTravelRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  budget?: number;
  currency?: string;
  userId: number;
  destinationIds?: number[];
  activityIds?: number[];
}

export interface UpdateTravelRequest {
  title?: string;
  description?: string;
  startDate?: string;
  endDate?: string;
  budget?: number;
  currency?: string;
  status?: string;
}

export interface TravelSearchParams {
  page?: number;
  size?: number;
  userId?: number;
  status?: string;
  startDateFrom?: string;
  startDateTo?: string;
  sortBy?: string;
  sortDirection?: 'ASC' | 'DESC';
}

export const travelService = {
  getAll: async (params: TravelSearchParams = {}): Promise<PagedResponse<Travel>> => {
    const response = await apiClient.get('/travels', { params });
    return response.data;
  },

  getById: async (id: number): Promise<ApiResponse<Travel>> => {
    const response = await apiClient.get(`/travels/${id}`);
    return response.data;
  },

  create: async (data: CreateTravelRequest): Promise<ApiResponse<Travel>> => {
    const response = await apiClient.post('/travels', data);
    return response.data;
  },

  update: async (id: number, data: UpdateTravelRequest): Promise<ApiResponse<Travel>> => {
    const response = await apiClient.put(`/travels/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<ApiResponse<void>> => {
    const response = await apiClient.delete(`/travels/${id}`);
    return response.data;
  },

  updateStatus: async (id: number, status: string): Promise<ApiResponse<Travel>> => {
    const response = await apiClient.patch(`/travels/${id}/status`, { status });
    return response.data;
  },

  addDestination: async (travelId: number, destinationId: number): Promise<ApiResponse<Travel>> => {
    const response = await apiClient.post(`/travels/${travelId}/destinations/${destinationId}`);
    return response.data;
  },

  removeDestination: async (travelId: number, destinationId: number): Promise<ApiResponse<void>> => {
    const response = await apiClient.delete(`/travels/${travelId}/destinations/${destinationId}`);
    return response.data;
  },

  // Destination CRUD
  getAllDestinations: async (): Promise<ApiResponse<Destination[]>> => {
    const response = await apiClient.get('/destinations');
    return response.data;
  },

  // Activity CRUD
  getAllActivities: async (): Promise<ApiResponse<Activity[]>> => {
    const response = await apiClient.get('/activities');
    return response.data;
  },
};
