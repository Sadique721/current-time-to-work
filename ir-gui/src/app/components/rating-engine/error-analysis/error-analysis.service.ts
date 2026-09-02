import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';

export interface ErrorConfigCheckStatusDTO {
  id: number;
  errorRatedRecordId: string;
  isConfigReady: boolean;
  errorMessage: string;
  incomingFailureData: string[];
  incomingSuccessData: string[];
  outgoingFailureData: string[];
  outgoingSuccessData: string[];
  serviceType: string;
  lineOfBusiness: string;
  callingNumber?: string;
  calledNumber?: string;
  incomingAccountId?: string;
  outgoingAccountId?: string;
}

@Injectable({
  providedIn: "root"
})
export class ErrorAnalysisService {
  private baseUrl = OCS_RATING + "/rerate-requests";

  constructor(private http: HttpClient) {}

  fetchErrorConfigCheckResults(page: number = 0, size: number = 10): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('_t', new Date().getTime().toString());
    return this.http.get<any>(`${this.baseUrl}/fetchErrorConfigCheckResult`, { params });
  }
}
