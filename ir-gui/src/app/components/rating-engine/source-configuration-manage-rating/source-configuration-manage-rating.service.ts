import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  OCS_RATING,
} from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class SourceConfigurationManageService {
  baseURL: Readonly<string> = OCS_RATING + "/source-config";

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

  
  getById(id: number) {
    return this.http.get(`${this.baseURL}/${id}`);
  }

  
  getSourceCdrBySource(sourceId: number) {
    return this.http.get(`${OCS_RATING}/source-cdr-config/source/${sourceId}`);
  }

  updateSourceCdrSequences(sourceId: number, data: any) {
    return this.http.put(
      `${OCS_RATING}/source-cdr-config/update-sequences/${sourceId}`,
      data
    );
  }
}
