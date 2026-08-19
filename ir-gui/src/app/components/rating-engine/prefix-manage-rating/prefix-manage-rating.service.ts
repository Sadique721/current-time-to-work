import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  OCS_RATING,
} from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class PrefixManageService {
  baseURL: Readonly<string> = OCS_RATING + "/prefixes";

  constructor(private http: HttpClient) {}

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  putMethod(prefixId: number, data: any) {
    return this.http.put(`${this.baseURL}/${prefixId}`, data);
  }
  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  deleteMethod(prefixId: number) {
    return this.http.delete(`${this.baseURL}/${prefixId}`);
  }

  updateMethod(id: number, data: any) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }

  
  postPaginated(data: any) {
    return this.http.post(`${this.baseURL}/paginated`, data);
  }

  getContries() {
    return this.http.get(`${OCS_RATING}/countries/names`);
  }
}
