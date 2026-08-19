import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class OrganizationManageService {
  baseURL: Readonly<string> = OCS_RATING + "/organizations";

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get(`${this.baseURL}`);
  }

  postPaginated(payload: any) {
    return this.http.post(`${this.baseURL}/paginated`, payload);
  }

  getById(id: number) {
    return this.http.get(`${this.baseURL}/${id}`);
  }

  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  putMethod(id: number, data: any) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }

  deleteMethod(id: number) {
    return this.http.delete(`${this.baseURL}/${id}`);
  }
}
