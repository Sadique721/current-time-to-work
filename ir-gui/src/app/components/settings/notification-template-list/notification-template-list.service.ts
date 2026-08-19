import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  API_GATEWAY_COMMON_MANAGEMENT,
  NOTIFICATION_URL,
} from "src/app/core/RadiusUtils/RadiusConstants";
import { INotificationTemplate } from "./notification-template-list.interface";
@Injectable({
  providedIn: "root",
})
export class NotificationTemplateListService {
  baseURL: Readonly<string> = NOTIFICATION_URL + "/Template";
  API_GATEWAY_COMMON_MANAGEMENT: Readonly<string> =
    API_GATEWAY_COMMON_MANAGEMENT;
  constructor(private http: HttpClient) {}
  searchTemplate(params: HttpParams): Observable<any> {
    return this.http.get(`${this.baseURL}/search`, { params });
  }
  updateTemplate(id: number, data: any): Observable<any> {
    return this.http.put(`${this.baseURL}/update/${id}`, data);
  }
  searchMethod(url: string, body: any): Observable<any> {
    return this.http.post(
      `${this.API_GATEWAY_COMMON_MANAGEMENT}/notification/${url}`,
      body
    );
  }
}
