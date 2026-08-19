export interface ITapRecord {
  tapFileId: number;
  fileName: string;
  senderTadig: string | null;
  recipientTadig: string | null;
  fileSequenceNo: number | null;
  fileType: string;
  tapVersion: string | null;
  status: string;
  totalRecords: number | null;
  totalCharge: number | null;
  tapDecimalPlaces: number | null;
  localCurrency: string | null;
  partnerId: number | null;
  partnerName: string | null;
  errorReason: string | null;
  processedAt: string | null;
}

export interface ITapRecordPageDetails {
  totalPages: number;
  totalRecords: number;
  totalRecordsPerPage: number;
  currentPageNumber: number;
}

export interface ITapRecordPagedResponse {
  pageDetails: ITapRecordPageDetails;
  content: ITapRecord[];
}

export interface ITapRecordSearchCriteria {
  senderTadig?: string;
  recipientTadig?: string;
  status?: string;
  fileType?: string;
  partnerId?: number;
}
