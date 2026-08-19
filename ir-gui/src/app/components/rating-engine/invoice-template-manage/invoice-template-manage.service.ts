import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class InvoiceTemplateManageService {
  baseURL: Readonly<string> = OCS_RATING + "/templates";

  constructor(private http: HttpClient) {}

  postPaginated(payload: any) {
    return this.http.post(`${this.baseURL}/paginated`, payload);
  }

  getById(id: any) {
    return this.http.get(`${this.baseURL}/${id}`);
  }

  postMethod(data: FormData) {
    return this.http.post(`${this.baseURL}`, data);
  }

  putMethod(id: any, data: FormData) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }

  deleteMethod(id: any) {
    return this.http.delete(`${this.baseURL}/${id}`);
  }
}
