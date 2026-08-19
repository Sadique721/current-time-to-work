import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { API_GATEWAY_COMMON_MANAGEMENT } from 'src/app/core/RadiusUtils/RadiusConstants';

export interface UserGroup {
  id: number;
  name: string;
  outgoingRule: string;   
  outboundRule?: string;  
  status: boolean;
  mvnoId?: number;
  staffId?: number;
  createdAt?: string;
  updatedAt?: string;
  selected?: boolean;
}

export interface UserGroupListResponse {
  data: UserGroup[];
  totalCount: number;
}

interface ApiUserGroup {
  id: number;
  name: string;
  outboundRule: string;
  status: string;         
  mvnoId?: number;
  staffId?: number;
  createdAt?: string;
  updatedAt?: string;
}

interface ApiListResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: ApiUserGroup[];
  pagination: {
    totalRecords: number;
    limit: number;
    totalPages: number;
    page: number;
  };
}

interface ApiSingleResponse {
  statusCode: number;
  success: boolean;
  message: string;
  data: ApiUserGroup;
}

@Injectable({
  providedIn: 'root'
})
export class UserGroupService {
  private readonly apiUrl = API_GATEWAY_COMMON_MANAGEMENT+'/user-group';

  constructor(private http: HttpClient) {}

  

    private mapToFrontend(apiItem: ApiUserGroup): UserGroup {
    return {
      id: apiItem.id,
      name: apiItem.name,
      outgoingRule: apiItem.outboundRule,   
      outboundRule: apiItem.outboundRule,
      status: apiItem.status === '1',       
      mvnoId: apiItem.mvnoId,
      staffId: apiItem.staffId,
      createdAt: apiItem.createdAt,
      updatedAt: apiItem.updatedAt,
      selected: false,
    };
  }

    private mapToApi(userGroup: Partial<UserGroup>): any {
    return {
      name: userGroup.name,
      outboundRule: userGroup.outgoingRule,            
      status: userGroup.status ? '1' : '0',            
      staffId: userGroup.staffId ?? 101,
    };
  }

  

    getAll(page: number, pageSize: number): Observable<UserGroupListResponse> {
    const params = new HttpParams()
      .set('page', (page - 1).toString())   
      .set('limit', pageSize.toString());

    return this.http
      .get<ApiListResponse>(this.apiUrl, { params })
      .pipe(
        map(res => ({
          data: (res.data || []).map(item => this.mapToFrontend(item)),
          totalCount: res.pagination?.totalRecords ?? 0,
        }))
      );
  }

    search(keyword: string, page: number, pageSize: number): Observable<UserGroupListResponse> {
    const params = new HttpParams()
      .set('keyword', keyword)
      .set('page', (page - 1).toString())
      .set('limit', pageSize.toString());

    return this.http
      .get<ApiListResponse>(`${this.apiUrl}/search`, { params })
      .pipe(
        map(res => ({
          data: (res.data || []).map(item => this.mapToFrontend(item)),
          totalCount: res.pagination?.totalRecords ?? 0,
        }))
      );
  }

    getById(id: number): Observable<UserGroup> {
    return this.http
      .get<ApiSingleResponse>(`${this.apiUrl}/${id}`)
      .pipe(map(res => this.mapToFrontend(res.data)));
  }

    create(userGroup: Partial<UserGroup>): Observable<UserGroup> {
    const payload = this.mapToApi(userGroup);
    return this.http
      .post<ApiSingleResponse>(this.apiUrl, payload)
      .pipe(map(res => this.mapToFrontend(res.data)));
  }

    update(id: number, userGroup: Partial<UserGroup>): Observable<UserGroup> {
    const payload = this.mapToApi(userGroup);
    return this.http
      .put<ApiSingleResponse>(`${this.apiUrl}/${id}`, payload)
      .pipe(map(res => this.mapToFrontend(res.data)));
  }

    delete(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.apiUrl}/${id}`);
  }
}