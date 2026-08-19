import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CALL_CENTER_URL } from 'src/app/core/RadiusUtils/RadiusConstants';

export interface CallScript {
  id: number;
  callScriptName: string;
  description: string;
  script: string;
  status: boolean;
  mvnoId?: number;
  createdAt?: string;
  updatedAt?: string;
}

interface ApiCallScript {
  id: number;
  name: string;
  description: string;
  script: string;
  status: string;
  mvnoId?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CallScriptListResponse {
  data: CallScript[];
  totalCount: number;
}

interface ApiResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: ApiCallScript[];
  pagination: {
    limit: number;
    totalRecords: number;
    page: number;
    totalPages: number;
  };
}

interface ApiResponseSingle {
  statusCode: number;
  success: boolean;
  message: string;
  data: ApiCallScript; 
  pagination: null;
}

interface ApiResponseList {
  statusCode: number;
  success: boolean;
  message: string;
  data: ApiCallScript[]; 
  pagination: {
    limit: number;
    totalRecords: number;
    page: number;
    totalPages: number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class CallScriptService {
  private apiUrl = CALL_CENTER_URL+'/CallScript';

  constructor(private http: HttpClient) {}

  private transformApiToLocal(apiData: ApiCallScript): CallScript {
    return {
      id: apiData.id,
      callScriptName: apiData.name,
      description: apiData.description,
      script: apiData.script,
      status: apiData.status === '1',
      mvnoId: apiData.mvnoId,
      createdAt: apiData.createdAt,
      updatedAt: apiData.updatedAt
    };
  }

  private transformLocalToApi(localData: Partial<CallScript>): any {
    return {
      name: localData.callScriptName,
      description: localData.description,
      script: localData.script,
      status: localData.status ? '1' : '0'
    };
  }

  search(searchTerm: string, page: number, pageSize: number): Observable<CallScriptListResponse> {
    const params = new HttpParams()
      .set('keyword', searchTerm)
      .set('page', page.toString())
      .set('limit', pageSize.toString());

    return this.http.get<ApiResponse>(`${this.apiUrl}/Search`, { params }).pipe(
      map(response => ({
        data: response.data.map(item => this.transformApiToLocal(item)),
        totalCount: response.pagination.totalRecords
      }))
    );
  }


  getAll(page: number, pageSize: number): Observable<CallScriptListResponse> {
  const params = new HttpParams()
    .set('page', page.toString())
    .set('limit', pageSize.toString());

  return this.http.get<ApiResponseList>(this.apiUrl, { params }).pipe(
    map(response => ({
      data: response.data.map(item => this.transformApiToLocal(item)),
      totalCount: response.pagination.totalRecords
    }))
  );
}

getById(id: number): Observable<CallScript> {
  return this.http.get<ApiResponseSingle>(`${this.apiUrl}/${id}`).pipe(
    map(response => this.transformApiToLocal(response.data))
  );
}

create(callScript: Partial<CallScript>): Observable<CallScript> {
  const payload = this.transformLocalToApi(callScript);
  return this.http.post<ApiResponseSingle>(this.apiUrl, payload).pipe(
    map(response => this.transformApiToLocal(response.data))
  );
}

update(id: number, callScript: Partial<CallScript>): Observable<CallScript> {
  const payload = this.transformLocalToApi(callScript);
  return this.http.put<ApiResponseSingle>(`${this.apiUrl}/${id}`, payload).pipe(
    map(response => this.transformApiToLocal(response.data))
  );
}




  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}