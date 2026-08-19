import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  OCS_RATING,
} from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class AccountManageService {
  baseURL: Readonly<string> = OCS_RATING + "/accounts";

  constructor(private http: HttpClient) {}

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  getpartnerMethod(url: string) {
    return this.http.get(OCS_RATING + url);
  }

  getproductPlansListMethod(url: string) {
    return this.http.get(OCS_RATING + url);
  }

  putMethod(accountId: number, data: any) {
    return this.http.put(`${this.baseURL}/${accountId}`, data);
  }

  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  deleteMethod(accountId: number) {
    return this.http.delete(`${this.baseURL}/${accountId}`);
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

  getProductPlansByType(partnerType: string) {
    return this.http.get(`${OCS_RATING}/product-plans/names`, {
      params: { partnerType },
    });
  }

  getPartnersByType(partnerType: string) {
    return this.http.get(`${OCS_RATING}/partners/names`, {
      params: { partnerType },
    });
  }

  getAllPartners() {
    return this.http.get(`${OCS_RATING}/partners/list`);
  }
}
