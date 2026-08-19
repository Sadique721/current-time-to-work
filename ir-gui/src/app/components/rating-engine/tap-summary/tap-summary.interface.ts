export interface ITapSummary {
  summaryId: number;
  summaryDate: string;
  tapFileName: string;
  tapFileId: number;
  tapDirection: string;
  tapVersion: string;
  partnerName: string;
  serviceType: string;
  totalTapCharge: number;
  ourCharge: number;
}

export interface ITapSummaryPageDetails {
  totalPages: number;
  totalRecords: number;
  totalRecordsPerPage: number;
  currentPageNumber: number;
}

export interface ITapSummaryPagedResponse {
  pageDetails: ITapSummaryPageDetails;
  content: ITapSummary[];
}

export interface ITapSummarySearchCriteria {
  summaryDate?: string;
  fileName?: string;
  tapDirection?: string;
  partnerName?: string;
}
