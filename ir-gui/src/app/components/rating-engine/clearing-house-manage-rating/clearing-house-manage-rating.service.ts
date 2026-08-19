import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class ClearingHouseManageService {
  baseURL: Readonly<string> = OCS_RATING + "/clearing-houses";

  constructor(private http: HttpClient) {}

  /** GET /search?name=&status=&page=1&pageSize=10 */
  search(params: any) {
    return this.http.get(`${this.baseURL}/search`, { params });
  }

  /** POST /paginated  (for full paginated list without filters) */
  getPaginated(page: number, pageSize: number) {
    return this.http.post(`${this.baseURL}/paginated`, { page, pageSize });
  }

  getById(id: any) {
    return this.http.get(`${this.baseURL}/${id}`);
  }

  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  putMethod(id: any, data: any) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }

  deleteMethod(id: any) {
    return this.http.delete(`${this.baseURL}/${id}`);
  }

  getNames() {
    return this.http.get(`${this.baseURL}/names`);
  }

  getCurrencyCodes() {
    return this.http.get(`${OCS_RATING}/countries/currencyCodes`);
  }

  getSftpConfig(id: any) {
    return this.http.get(`${this.baseURL}/${id}/sftp-config`);
  }

  patchSftpConfig(id: any, data: any) {
    return this.http.patch(`${this.baseURL}/${id}/sftp-config`, data);
  }
}
