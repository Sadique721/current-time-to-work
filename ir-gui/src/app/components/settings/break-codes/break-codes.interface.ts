export interface BreakCode {
  id: number;
  breakCode: string;
  name: string;
  duration: string;
  description?: string;
  status: string;
  mvnoId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface BreakCodeListResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: BreakCode[];
  pagination: {
    page: number;
    totalRecords: number;
    limit: number;
    totalPages: number;
  };
}

export interface BreakCodeResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: BreakCode;
  pagination: null;
}