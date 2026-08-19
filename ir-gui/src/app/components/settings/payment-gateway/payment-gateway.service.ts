import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_GATEWAY_COMMON_MANAGEMENT } from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class PaymentGatewayService {
  constructor(private http: HttpClient) {}

  getAlllPaymentConfig(body: any): Observable<any> {
    return this.http.post(
      `${API_GATEWAY_COMMON_MANAGEMENT}/paymentconfig/findByAllPaymentConfig`,
      body,
    );
  }

  getListOfCommonPaymentGatewaty(): Observable<any> {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/commonList/paymentGateway`,
    );
  }

  createPaymentGateway(data: any): Observable<any> {
    return this.http.post(
      `${API_GATEWAY_COMMON_MANAGEMENT}/paymentconfig/create`,
      data,
    );
  }

  updatePaymentGateway(data: any): Observable<any> {
    return this.http.put(
      `${API_GATEWAY_COMMON_MANAGEMENT}/paymentconfig/update`,
      data,
    );
  }

  deletePaymentGateway(paymentConfigId: number): Observable<any> {
    return this.http.delete(
      `${API_GATEWAY_COMMON_MANAGEMENT}/paymentconfig/delete?paymentConfigId=${paymentConfigId}`,
    );
  }

  updatePaymentGatewayStatus(data: any): Observable<any> {
    return this.http.put(
      `${API_GATEWAY_COMMON_MANAGEMENT}/paymentconfig/changeStatus`,
      data,
    );
  }

  getPaymentGatewayParameterById(name: string): Observable<any> {
    return this.http.get(
      `${API_GATEWAY_COMMON_MANAGEMENT}/paymentconfig/getParameterByName?name=${name}`,
    );
  }
}
