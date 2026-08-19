import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class SchedulerConfigurationService {
  private readonly baseURL = `${OCS_RATING}/scheduler`;

  constructor(private http: HttpClient) {}

  /** GET /api/scheduler — fetch current configuration */
  getConfig() {
    return this.http.get<any>(this.baseURL);
  }

  /** PATCH /api/scheduler?isActive=true|false — toggle active state */
  patchActive(isActive: boolean) {
    return this.http.patch(
      `${this.baseURL}?isActive=${isActive}`,
      {},
      { responseType: 'text' }
    );
  }

  /** PUT /api/scheduler — save full configuration */
  putConfig(data: any) {
    return this.http.post(this.baseURL, data, { responseType: 'text' });
  }

  /** GET /api/schedulerStatus/NORMAL — fetch latest scheduler status */
  getLatestStatus() {
    return this.http.get<any>(`${OCS_RATING}/schedulerStatus`);
  }
}