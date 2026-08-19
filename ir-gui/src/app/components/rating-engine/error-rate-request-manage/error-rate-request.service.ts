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
}

export interface ErrorRateRequestDTO {
  id?: number;
  requestId?: string;
  requestName: string;
  requestParameters: string;
  voiceQueryConfig?: CdrQueryConfigDTO | null;
  smsQueryConfig?: CdrQueryConfigDTO | null;
  usageQueryConfig?: CdrQueryConfigDTO | null;
  status?: string;
  enable: boolean;
  startDate?: string;
  endDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ErrorRateRequestService {
  private apiUrl = OCS_RATING + "/error-rate-requests";

  constructor(private http: HttpClient) {}

  fetchPage(page: number, size: number): Observable<any> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('_t', new Date().getTime().toString()); // Cache buster
    return this.http.get<any>(`${this.apiUrl}/page`, { params });
  }

  fetchQueryConfigs(): Observable<CdrQueryConfigDTO[]> {
    return this.http.get<CdrQueryConfigDTO[]>(`${this.apiUrl}/query-configs`);
  }

  create(dto: ErrorRateRequestDTO): Observable<ErrorRateRequestDTO> {
    return this.http.post<ErrorRateRequestDTO>(this.apiUrl, dto);
  }

  update(id: number, dto: ErrorRateRequestDTO): Observable<ErrorRateRequestDTO> {
    return this.http.put<ErrorRateRequestDTO>(`${this.apiUrl}/${id}`, dto);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  editErrorRateStatus(requestId: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/editStatusAs/PENDING/for/${requestId}`, {});
  }
}
