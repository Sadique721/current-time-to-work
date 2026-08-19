import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { OCS_RATING } from 'src/app/core/RadiusUtils/RadiusConstants';
import { IInvoicePagedResponse, IInvoiceSearchCriteria } from './invoices.interface';

@Injectable({
  providedIn: 'root',
})
export class InvoicesService {
  private readonly baseURL = `${OCS_RATING}/invoices`;

  constructor(private http: HttpClient) {}

  /**
   * POST /api/invoices/paginated
   * Fetches a single page of invoices.
   * Attaches the optional searchCriteria block only when the caller provides it.
   */
  getPaginatedInvoices(
    page: number = 1,
    pageSize: number = 10,
    searchCriteria?: IInvoiceSearchCriteria
  ) {
    const body: any = { page, pageSize };
    if (searchCriteria && Object.keys(searchCriteria).length > 0) {
      body.searchCriteria = searchCriteria;
    }
    return this.http.post<IInvoicePagedResponse>(`${this.baseURL}/paginated`, body);
  }

  /**
   * GET /api/invoices/{id}/pdf
   * Downloads the PDF file for the given invoice ID.
   * responseType must be 'blob' so the HTTP client returns raw binary data
   * instead of attempting to parse the multipart/PDF response as JSON.
   */
  downloadInvoice(invoiceId: string | number) {
    return this.http.get(`${OCS_RATING}/invoices/${invoiceId}/pdf`, {
      responseType: 'blob',
    });
  }
}
