import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  CUSTOMER_URL,
  INVENTORY_MANAGEMENT_BASE_URL,
  NOTIFICATION_URL,
  PAYMENT_RECEIPT_BASE_URL,
  PRODUCT_MANAGEMENT_BASE_URL,
  REVENUE_MANAGEMENT_BASE_URL,
  TICKET_MANAGEMENT,
} from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class AuditLogService {
  constructor(private http: HttpClient) {}

  getCommonManagementLog(url: string) {
    return this.http.get(API_GATEWAY_COMMON_MANAGEMENT + url);
  }

  getProductManagementLog(url: string) {
    return this.http.get(PRODUCT_MANAGEMENT_BASE_URL + url);
  }

  getTicketManagementLog(url: string) {
    return this.http.get(TICKET_MANAGEMENT + url);
  }

  getInventoryLog(url: string) {
    return this.http.get(INVENTORY_MANAGEMENT_BASE_URL + url);
  }

  generatePaymentReceiptLog(url: string) {
    return this.http.get(PAYMENT_RECEIPT_BASE_URL + url);
  }

  getNotificationLog(url: string) {
    return this.http.get(NOTIFICATION_URL + url);
  }

  postCommonManagementLog(path: string, data: any) {
    return this.http.post(API_GATEWAY_COMMON_MANAGEMENT + path, data);
  }

  postProductManagementLog(url: string, data: any) {
    return this.http.post(PRODUCT_MANAGEMENT_BASE_URL + url, data);
  }

  postTicketManagementLog(url: string, data: any) {
    return this.http.post(TICKET_MANAGEMENT + url, data);
  }

  postInventoryLog(url: string, data: any) {
    return this.http.post(INVENTORY_MANAGEMENT_BASE_URL + url, data);
  }

  postRevenueLog(url: string, data: any) {
    return this.http.post(REVENUE_MANAGEMENT_BASE_URL + url, data);
  }

  postNotificationLog(path: string, data: any) {
    return this.http.post(NOTIFICATION_URL + path, data);
  }

  getCustomerLog(url: string) {
return this.http.get(CUSTOMER_URL+ url);
  }
  
  postCustomerLog(path: string, data: any) {
return this.http.post(CUSTOMER_URL + path, data);
}
}
