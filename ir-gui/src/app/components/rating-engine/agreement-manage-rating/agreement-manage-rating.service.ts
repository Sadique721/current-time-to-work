import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { OCS_RATING } from "src/app/core/RadiusUtils/RadiusConstants";

@Injectable({
  providedIn: "root",
})
export class AgreementManageService {
  baseURL: Readonly<string> = OCS_RATING + "/agreements";

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get(`${this.baseURL}`);
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

  /** Step 1 – Fetch all partners for the partner dropdown */
  getPartners() {
    return this.http.get(`${OCS_RATING}/partners/partnerIds`);
  }

  /** Step 2 – Fetch account codes for a specific partner */
  getAccountsByPartnerId(partnerId: number) {
    return this.http.get(`${OCS_RATING}/accounts/${partnerId}/accountCodes`);
  }

  /** Step 4 – Fetch all invoice templates */
  getTemplateIds(lineOfBusiness?: string) {
    const url = lineOfBusiness
      ? `${OCS_RATING}/templates/templateIds?lineOfBusiness=${lineOfBusiness}`
      : `${OCS_RATING}/templates/templateIds`;
    return this.http.get(url);
  }

  getTaxConfigs() {
    return this.http.get(`${OCS_RATING}/tax-configs/names`);
  }

  getTaxConfigsByCountry(countryIso: string) {
    return this.http.get(`${OCS_RATING}/tax-configs/country/${countryIso}`);
  }

  getCountries() {
    return this.http.get(`${OCS_RATING}/countries/iso-codes`);
  }

  getStates(countryIso: string) {
    return this.http.get(`${OCS_RATING}/states?countryIso=${countryIso}`);
  }
}
