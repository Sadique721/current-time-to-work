import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  OCS_RATING,
} from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class CountryManageService {
  baseURL: Readonly<string> = OCS_RATING + "/countries";

  constructor(private http: HttpClient) {}

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  putMethod(countryId: number, data: any) {
    return this.http.put(`${this.baseURL}/${countryId}`, data);
  }
  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  deleteMethod(countryId: number) {
    return this.http.delete(`${this.baseURL}/${countryId}`);
  }

  updateMethod(id: number, data: any) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }
  postPaginated(data: any) {
    return this.http.post(`${this.baseURL}/paginated`, data);
  }
}
