export interface IInvoice {
  invoiceId: number;
  invoiceNumber: string;
  agreementId: number;
  agreementCode: string;
  billingCycleStart: string;
  billingCycleEnd: string;
  status: string;
  customerTotal: number;
  vendorTotal: number;
  netAmount: number;
  netPayableBy: string;
  generatedDate: string;
  pdfFilePath: string | null;
  pdfChecksum: string | null;
  pdfGeneratedAt: string | null;
  pdfErrorReason: string | null;
}

export interface IInvoicePageDetails {
  totalPages: number;
  totalRecords: number;
  totalRecordsPerPage: number;
  currentPageNumber: number;
}

export interface IInvoicePagedResponse {
  pageDetails: IInvoicePageDetails;
  content: IInvoice[];
}

export interface IInvoiceSearchCriteria {
  searchTerm?: string;
  status?: string;
  billingCycleStartFrom?: string;
  billingCycleStartTo?: string;
  agreementId?: number;
}
