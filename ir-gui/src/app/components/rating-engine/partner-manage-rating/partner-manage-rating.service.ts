import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  OCS_RATING,
} from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class PartnerManageService {
  baseURL: Readonly<string> = OCS_RATING + "/partners";

  constructor(private http: HttpClient) {}

  getMethod() {
    return this.http.get(`${this.baseURL}`);
  }

  getById(partnerId: number) {
    return this.http.get(`${this.baseURL}/${partnerId}`);
  }

  putMethod(partnerId: number, data: any) {
    return this.http.put(`${this.baseURL}/${partnerId}`, data);
  }
  postMethod(data: any) {
    return this.http.post(`${this.baseURL}`, data);
  }

  deleteMethod(partnerId: number) {
    return this.http.delete(`${this.baseURL}/${partnerId}`);
  }

  updateMethod(id: number, data: any) {
    return this.http.put(`${this.baseURL}/${id}`, data);
  }

  
  postPaginated(data: any) {
    return this.http.post(`${this.baseURL}/paginated`, data);
  }

  getTapProfilesDropdown() {
    return this.http.get(`${OCS_RATING}/v1/roaming/tap-profiles/dropdown`);
  }

  getTapProfileGroupsDropdown() {
    return this.http.get(`${OCS_RATING}/v1/roaming/tap-profile-groups/dropdown`);
  }

  getOrganizations() {
    return this.http.get(`${OCS_RATING}/organizations/names`);
  }

  getClearingHouses() {
    return this.http.get(`${OCS_RATING}/clearing-houses/names`);
  }

  getContries() {
    return this.http.get(`${OCS_RATING}/countries/names`);
  }

  getCurrencyCodes() {
    return this.http.get(`${OCS_RATING}/countries/currencyCodes`);
  }

  patchSftpConfig(partnerId: number, data: any) {
    return this.http.patch(`${this.baseURL}/${partnerId}/sftp-config`, data);
  }

  getSftpConfig(partnerId: number) {
    return this.http.get(`${this.baseURL}/${partnerId}/sftp-config`);
  }
}
