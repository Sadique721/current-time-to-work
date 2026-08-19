import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';
import { ISchedulerAuditPagedResponse } from './scheduler-audit-logs.interface';

@Injectable({
  providedIn: 'root',
})
export class SchedulerAuditLogsService {
  private readonly baseURL = `${OCS_RATING}/scheduler-audit-logs`;

  constructor(private http: HttpClient) {}

  /**
   * POST /api/scheduler-audit-logs
   * Fetches a single page of scheduler audit logs.
   * The API uses 0-indexed pages (Spring Data Pageable convention).
   *
   * @param page     0-based page index
   * @param pageSize number of records per page
   */
  getAuditLogs(page: number = 0, pageSize: number = 10) {
    const body = {
      page: String(page),
      pageSize: String(pageSize),
    };
    return this.http.post<ISchedulerAuditPagedResponse>(`${this.baseURL}/paginated`, body);
  }
}
