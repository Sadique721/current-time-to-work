import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  COMMON_BASE_URL,
  PAYMENT_RECEIPT_BASE_URL,
  REVENUE_MANAGEMENT_BASE_URL,
} from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class MvnoManagementService {
  constructor(private http: HttpClient) {}

  getAllMVNO(body: any, params: HttpParams): any {
    return this.http.post(
      `${API_GATEWAY_COMMON_MANAGEMENT}/mvno/search`,
      body,
      { params },
    );
  }

  getAutType(): any {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/OtpAuthType`,
    );
  }

  getMvnoById(id: number): any {
    return this.http.get(`${API_GATEWAY_COMMON_MANAGEMENT}/mvno/${id}`);
  }

  createMvno(body: any): any {
    return this.http.post(`${API_GATEWAY_COMMON_MANAGEMENT}/mvno/save`, body);
  }

  updateMvno(body: any): any {
    return this.http.post(`${API_GATEWAY_COMMON_MANAGEMENT}/mvno/update`, body);
  }

  genarateIspInvoice(data: any): any {
    return this.http.post(
      `${COMMON_BASE_URL}/invoiceV2/genarateIspInvoice`,
      data,
    );
  }

  getdocumentList(id: number): any {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/mvno/mvnoDoc/getDocsByMvno/${id}`,
    );
  }

  getDocumentListStatus(): any {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/generic/docStatus`,
    );
  }

  getDocumentVerificationModeList(): any {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/generic/mvnodocverificationmode`,
    );
  }

  getDocumentTypeList(): any {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/generic/mvnodocverificationmode_offline`,
    );
  }

  getDocumentSubTypeList(type: string, mode: string): any {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/mvnodocsubtype?mvnodocsubtype=${type}&mode=${mode}`,
    );
  }

  getDocStatusList(): any {
    return this.http.get<any>(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/generic/docStatus`,
    );
  }

  saveMvnoDocument(mvnoId: number, data: any): any {
    return this.http.post(
      `${API_GATEWAY_COMMON_MANAGEMENT}/mvno/mvnoDoc/uploadDoc?mvnoId=${mvnoId}`,
      data,
    );
  }

  downloadFile(docId: number, mvnoId: number): Observable<any> {
    const get_url =
      API_GATEWAY_COMMON_MANAGEMENT +
      '/mvno/mvnoDoc/document/download/' +
      docId +
      '/' +
      mvnoId;
    return this.http.get(get_url, {
      responseType: 'blob',
    });
  }

  updateDocumentStatus(url: string, payload: any): any {
    return this.http.put(`${API_GATEWAY_COMMON_MANAGEMENT}${url}`, payload);
  }
  getMethod(url: string): any {
    return this.http.get(`${API_GATEWAY_COMMON_MANAGEMENT}${url}`);
  }

  PostMvnoId(oldMvnoid: number, newMvnoid: any): Observable<any> {
    const url = `${API_GATEWAY_COMMON_MANAGEMENT}/mvno/mvnoIspToIsp?oldMvnoid=${oldMvnoid}&newMvnoid=${newMvnoid}`;
    return this.http.post<any>(url, null);
  }

  postMethod(url: string, data: any) {
    return this.http.post(`${REVENUE_MANAGEMENT_BASE_URL}/${url}`, data);
  }
  dunningHistoryMethod(url: string, data: any) {
    return this.http.post(`${COMMON_BASE_URL}/${url}`, data);
  }
  posttMethod(url: string, data: any) {
    return this.http.post(`${COMMON_BASE_URL}/${url}`, data);
  }
  downloadPDFInvoice(invoiceId: any) {
    return this.http.get(
      `${PAYMENT_RECEIPT_BASE_URL}/invoicePdf/download/${invoiceId}`,
    );
  }
  generateMethodInvoice(url: string) {
    return this.http.get(`${PAYMENT_RECEIPT_BASE_URL}/${url}`);
  }
  postMethodWithFile(url: string, data: any) {
    return this.http.post(`${COMMON_BASE_URL}/${url}`, data);
  }
  paymenthistoryMethod(url: string): any {
    return this.http.get(`${REVENUE_MANAGEMENT_BASE_URL}${url}`);
  }
  getCustomerspaymentDetails(url: string): any {
    return this.http.get(`${COMMON_BASE_URL}${url}`);
  }
  getinvoicelist(url: string): any {
    return this.http.get(`${REVENUE_MANAGEMENT_BASE_URL}${url}`);
  }
  ledgerMethod(url: string, data: any) {
    return this.http.post(`${COMMON_BASE_URL}/${url}`, data);
  }
}
