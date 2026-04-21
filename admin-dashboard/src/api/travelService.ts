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

interface BackendTravel {
  id: number;
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  totalBudget?: number;
  currency: string;
  status: 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  createdBy: number;
  destinations?: Destination[];
  createdAt: string;
  updatedAt: string;
}

interface BackendTravelsPage {
  content?: BackendTravel[];
}

interface BackendPagedTravelResponse extends Omit<PagedResponse<BackendTravel>, 'data'> {
  data: BackendTravelsPage | BackendTravel[];
}

export interface CreateTravelRequest {
  title: string;
  description?: string;
  startDate: string;
  endDate: string;
  budget?: number;
  currency?: string;
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
    const response = await apiClient.get<BackendPagedTravelResponse>('/travels', { params });
    const payload = response.data;
    const backendRows = Array.isArray(payload.data)
      ? payload.data
      : payload.data?.content || [];

    const data: Travel[] = backendRows.map((travel) => ({
      id: travel.id,
      title: travel.title,
      description: travel.description,
      startDate: travel.startDate,
      endDate: travel.endDate,
      budget: travel.totalBudget,
      currency: travel.currency || 'EUR',
      status: travel.status,
      userId: travel.createdBy || 0,
      destinations: travel.destinations || [],
      activities: [],
      createdAt: travel.createdAt,
      updatedAt: travel.updatedAt,
    }));

    return {
      ...payload,
      data,
    };
  },

  getById: async (id: number): Promise<ApiResponse<Travel>> => {
    const response = await apiClient.get(`/travels/${id}`);
    return response.data;
  },

  create: async (data: CreateTravelRequest): Promise<ApiResponse<Travel>> => {
    const payload = {
      title: data.title,
      description: data.description,
      startDate: data.startDate,
      endDate: data.endDate,
      totalBudget: data.budget,
      currency: data.currency,
      destinations: data.destinationIds?.map((id) => ({ destinationId: id })),
    };
    const response = await apiClient.post('/travels', payload);
    return response.data;
  },

  update: async (id: number, data: UpdateTravelRequest): Promise<ApiResponse<Travel>> => {
    const payload = {
      ...data,
      totalBudget: data.budget,
      budget: undefined,
    };
    const response = await apiClient.put(`/travels/${id}`, payload);
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
