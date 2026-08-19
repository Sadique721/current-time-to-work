import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';

@Injectable({
  providedIn: 'root',
})
export class ExchangeRateService {
  private readonly baseURL = `${OCS_RATING}/exchange-rate-scheduler`;

  constructor(private http: HttpClient) {}

  /**
   * POST /api/exchange-rate-scheduler/rates
   * Fetches paginated exchange rates.
   * @param page     1-based page index
   * @param pageSize number of records per page
   */
  getExchangeRates(page: number = 1, pageSize: number = 10, search: string = '', validFrom: string = '') {
    const body: any = {
      page: page,
      pageSize: pageSize,
      searchCriteria: {
        searchTerm: search || null,
        validFrom: validFrom || null
      }
    };
    return this.http.post<any>(`${this.baseURL}/rates`, body);
  }
}
