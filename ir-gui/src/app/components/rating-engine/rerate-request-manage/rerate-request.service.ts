import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';

export interface CdrQueryConfigDTO {
  id: number;
  queryName: string;
  serviceType: string;
  fetchQuery: string;
  isActive?: boolean;
  isDelete?: boolean;
  deletedAt?: string | null;
  createdAt?: string;
  modifiedAt?: string;
  createdBy?: string | null;
  modifiedBy?: string | null;
}

export interface ReRateRequestDTO {
  id?: number;
  requestId?: string;
  auditName: string;
  requestParameters: string;
  voiceQueryConfig?: CdrQueryConfigDTO | null;
  smsQueryConfig?: CdrQueryConfigDTO | null;
  usageQueryConfig?: CdrQueryConfigDTO | null;
  status?: string;
  enable: boolean;
  remark?: string;
  startDate?: string;
  endDate?: string;
  requestedAt?: string;
  isActive?: boolean;
  isDelete?: boolean;
  deletedAt?: string;
  createdAt?: string;
  modifiedAt?: string;
  createdBy?: string | null;
  modifiedBy?: string | null;
  version?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ReRateRequestService {
  private apiUrl = OCS_RATING +"/rerate-requests";

  constructor(private http: HttpClient) {}

  fetchPage(page: number, size: number): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('_t', new Date().getTime().toString()); // Cache buster
    return this.http.get<any>(`${this.apiUrl}/page`, { params });
  }

  getById(id: number): Observable<ReRateRequestDTO> {
    return this.http.get<ReRateRequestDTO>(`${this.apiUrl}/${id}`);
  }

  fetchQueryConfigs(): Observable<CdrQueryConfigDTO[]> {
    return this.http.get<CdrQueryConfigDTO[]>(`${this.apiUrl}/query-configs`);
  }

  create(dto: ReRateRequestDTO): Observable<ReRateRequestDTO> {
    return this.http.post<ReRateRequestDTO>(this.apiUrl, dto);
  }

  update(id: number, dto: ReRateRequestDTO): Observable<ReRateRequestDTO> {
    return this.http.put<ReRateRequestDTO>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  editReRateStatus(requestId: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/editStatusAs/PENDING/for/${requestId}`, {});
  }
}
