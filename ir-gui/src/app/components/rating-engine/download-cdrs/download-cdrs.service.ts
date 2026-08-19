import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

export type CdrType = "VOICE" | "SMS" | "USAGE";
export type RatingStatus = "PENDING" | "RATED" | "UNRATED" | "FAILED";
export type ServiceType = "VOICE" | "SMS" | "USAGE";
export type CallType = "GPRS" | "MO_VOICE" | "MT_VOICE" | "MO_SMS" | "MT_SMS";
export type LineOfBusiness = "INTERCONNECT" | "ROAMING";

export interface CdrFilterRequest {
  serviceType: ServiceType;
  callingOrSubscriber?: string | null;
  calledOrApn?: string | null;
  incomingAccountId?: string | null;
  outgoingAccountId?: string | null;
  incomingRatingStatus?: RatingStatus | null;
  outgoingRatingStatus?: RatingStatus | null;
  homePlmn?: string | null;
  visitedPlmn?: string | null;
  zoneName?: string | null;
  lineOfBusiness?: LineOfBusiness | null;
  callType?: CallType | null;
  fromTime?: string | null;
  toTime?: string | null;
}

@Injectable({
  providedIn: "root",
})
export class DownloadCdrsService {
  private readonly baseURL = `${OCS_RATING}/cdr`;

  constructor(private http: HttpClient) {}

  /** Export CDR as Excel */
  exportCdr(filter: CdrFilterRequest): Observable<Blob> {
    return this.http.post(`${this.baseURL}/export`, filter, {
      responseType: "blob",
    });
  }

  /** Dropdown APIs */
  getZoneNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/zone-names`);
  }

  getHomePlmn(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/home-plmn`);
  }

  getVisitedPlmn(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/visited-plmn`);
  }

  getIncomingAccountIds(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/incoming-account-ids`);
  }

  getOutgoingAccountIds(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/outgoing-account-ids`);
  }

  getRatingStatuses(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/rating-statuses`);
  }

  getServiceTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/service-types`);
  }

  getCallTypes(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/call-types`);
  }

  getLineOfBusiness(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseURL}/dropdown/line-of-business`);
  }
}
