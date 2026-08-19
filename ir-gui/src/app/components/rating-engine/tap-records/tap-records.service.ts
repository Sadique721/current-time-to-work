import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';
import { ITapRecordPagedResponse, ITapRecordSearchCriteria } from './tap-records.interface';

@Injectable({ providedIn: 'root' })
export class TapRecordsService {
  private readonly baseURL = `${OCS_RATING}/roaming/tap`;

  constructor(private http: HttpClient) {}

  getPaginatedTapRecords(
    page: number = 1,
    pageSize: number = 10,
    searchCriteria?: ITapRecordSearchCriteria
  ) {
    const body: any = { page, pageSize };
    if (searchCriteria && Object.keys(searchCriteria).length > 0) {
      body.searchCriteria = searchCriteria;
    }
    return this.http.post<ITapRecordPagedResponse>(`${this.baseURL}/files/paginated`, body);
  }

  downloadTapFile(tapFileId: number) {
    return this.http.get(`${this.baseURL}/files/${tapFileId}/download`, {
      responseType: 'blob',
    });
  }
}
