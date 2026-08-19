import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface TelecomCircle {
  id: number;
  name: string;
  prefix: string;
  status: string;
  mvnoId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface TelecomCircleListResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: TelecomCircle[];
  pagination: {
    page: number;
    totalRecords: number;
    limit: number;
    totalPages: number;
  };
}

export interface TelecomCircleResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: TelecomCircle;
  pagination: null;
}

@Injectable({
  providedIn: 'root'
})
export class TelecomCircleService {
  private apiUrl = 'https://assistant.unifyxcess.ai:30443/api/v1/call/telecom_circle';

  constructor(private http: HttpClient) {}

  getAll(page: number, pageSize: number): Observable<TelecomCircleListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<TelecomCircleListResponse>(this.apiUrl, { params });
  }

  search(searchTerm: string, page: number, pageSize: number): Observable<TelecomCircleListResponse> {
    const params = new HttpParams()
      .set('keyword', searchTerm)
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<TelecomCircleListResponse>(`${this.apiUrl}/Search`, { params });
  }

  getById(id: number): Observable<TelecomCircle> {
    return this.http.get<TelecomCircleResponse>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.data)
    );
  }

  create(circle: Partial<TelecomCircle>): Observable<TelecomCircle> {
    const payload = {
      name: circle.name,
      prefix: circle.prefix,
      status: circle.status
    };

    return this.http.post<TelecomCircleResponse>(`${this.apiUrl}/create`, payload).pipe(
      map(response => response.data)
    );
  }

  update(id: number, circle: Partial<TelecomCircle>): Observable<TelecomCircle> {
    const payload = {
      name: circle.name,
      prefix: circle.prefix,
      status: circle.status
    };

    return this.http.put<TelecomCircleResponse>(`${this.apiUrl}/${id}`, payload).pipe(
      map(response => response.data)
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}