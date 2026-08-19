import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { PRODUCT_MANAGEMENT_BASE_URL } from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class ReportedProblemService {
  constructor(private http: HttpClient) {}

  getMethod(url: string) {
    return this.http.get(PRODUCT_MANAGEMENT_BASE_URL + url);
  }

  postMethod(url: string, data: any) {
    return this.http.post(PRODUCT_MANAGEMENT_BASE_URL + url, data);
  }

  deleteMethod(url: string) {
    return this.http.delete(PRODUCT_MANAGEMENT_BASE_URL + url);
  }

  updateMethod(url: string, data: any) {
    return this.http.put(PRODUCT_MANAGEMENT_BASE_URL + url, data);
  }
}
