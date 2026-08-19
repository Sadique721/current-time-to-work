import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class TaxManageService {
  private baseUrl = `${OCS_RATING}/tax-configs`;

  constructor(private http: HttpClient) {}

  postPaginated(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/paginated`, payload);
  }

  postMethod(payload: any): Observable<any> {
    return this.http.post(`${this.baseUrl}`, payload);
  }

  putMethod(id: number, payload: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}`, payload);
  }

  deleteMethod(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`);
  }

  getById(id: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${id}`);
  }

  getCountries(): Observable<any> {
    return this.http.get(`${OCS_RATING}/countries/iso-codes`);
  }
}
