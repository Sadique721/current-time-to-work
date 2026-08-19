import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CALL_CENTER_URL } from 'src/app/core/RadiusUtils/RadiusConstants';

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

@Injectable({
  providedIn: 'root'
})
export class BreakCodeService {
  private apiUrl = CALL_CENTER_URL+'/BreakCode';

  constructor(private http: HttpClient) {}

  getAll(page: number, pageSize: number): Observable<BreakCodeListResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<BreakCodeListResponse>(this.apiUrl, { params });
  }

  search(searchTerm: string, page: number, pageSize: number): Observable<BreakCodeListResponse> {
    const params = new HttpParams()
      .set('keyword', searchTerm)
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<BreakCodeListResponse>(`${this.apiUrl}/Search`, { params });
  }

  getById(id: number): Observable<BreakCode> {
    return this.http.get<BreakCodeResponse>(`${this.apiUrl}/${id}`).pipe(
      map(response => response.data)
    );
  }

  create(breakCode: Partial<BreakCode>): Observable<BreakCode> {
    const payload = {
      breakCode: breakCode.breakCode,
      name: breakCode.name,
      duration: breakCode.duration,
      description: breakCode.description || '',
      status: breakCode.status
    };

    return this.http.post<BreakCodeResponse>(`${this.apiUrl}/Create`, payload).pipe(
      map(response => response.data)
    );
  }

  update(id: number, breakCode: Partial<BreakCode>): Observable<BreakCode> {
    const payload = {
      breakCode: breakCode.breakCode,
      name: breakCode.name,
      duration: breakCode.duration,
      description: breakCode.description || '',
      status: breakCode.status
    };

    return this.http.put<BreakCodeResponse>(`${this.apiUrl}/${id}`, payload).pipe(
      map(response => response.data)
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}