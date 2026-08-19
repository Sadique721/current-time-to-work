import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';
import { ITapSummaryPagedResponse, ITapSummarySearchCriteria } from './tap-summary.interface';
import { TapRecordsService } from '../tap-records/tap-records.service';

@Injectable({
  providedIn: 'root',
})
export class TapSummaryService {
  // OCS_RATING usually includes /api
  private readonly baseURL = OCS_RATING;

  constructor(
    private http: HttpClient,
    private tapRecordsService: TapRecordsService
  ) {}

  /**
   * POST /roaming/summary/paginated
   * Note: Based on provided cURL, this endpoint might not have /api prefix.
   * We adjust by going one level up from OCS_RATING if necessary.
   */
  getTapSummaries(
    page: number = 1,
    pageSize: number = 10,
    searchCriteria: ITapSummarySearchCriteria = {}
  ): Observable<ITapSummaryPagedResponse> {
    const url = this.baseURL.replace('/api', '') + '/roaming/summary/paginated';
    const body = {
      page: page,
      pageSize: pageSize,
      searchCriteria: searchCriteria,
    };
    return this.http.post<ITapSummaryPagedResponse>(url, body);
  }

  /**
   * Delegates download to TapRecordsService to use the same API endpoint.
   */
  downloadTapFile(fileId: number): Observable<Blob> {
    return this.tapRecordsService.downloadTapFile(fileId) as Observable<Blob>;
  }

  /**
   * GET /roaming/tap/files/{id}/cdrs?serviceType={serviceType}
   */
  getTapSummaryCdrs(fileId: number, serviceType: string): Observable<any> {
    const url = `${this.baseURL}/roaming/tap/files/${fileId}/cdrs`;
    return this.http.get(url, { params: { serviceType } });
  }
}
