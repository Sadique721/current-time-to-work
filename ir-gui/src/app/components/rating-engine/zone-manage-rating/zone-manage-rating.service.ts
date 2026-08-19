import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class ZoneManageService {
  baseURL: Readonly<string> = OCS_RATING + "/zones";

  constructor(private http: HttpClient) { }

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  getById(zoneId: number) {
    return this.http.get(`${this.baseURL}/${zoneId}`);
  }

  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  putMethod(zoneId: number, data: any) {
    return this.http.put(`${this.baseURL}/${zoneId}`, data);
  }

  deleteMethod(zoneId: number) {
    return this.http.delete(`${this.baseURL}/${zoneId}`);
  }

  postPaginated(data: any) {
    return this.http.post(`${this.baseURL}/paginated`, data);
  }

  getPrefixOptions(search: string = "", prefixType: string = "ALL") {
    let params = new HttpParams();
    if (search) {
      params = params.set("search", search);
    }
    if (prefixType && prefixType !== "ALL") {
      params = params.set("prefixType", prefixType);
    }

    return this.http.get(`${this.baseURL}/prefix-options`, { params });
  }
}
