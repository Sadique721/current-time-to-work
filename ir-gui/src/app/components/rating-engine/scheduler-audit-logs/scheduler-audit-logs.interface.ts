export interface ISchedulerAuditLog {
  auditId: number;
  configId: number;
  schedulerStatusId: number;
  eventType: string;
  eventMessage: string;
  cdrStartTime: string | null;
  cdrEndTime: string | null;
  recordsProcessed: number | null;
  executionTimeMs: number | null;
  errorDetails: string | null;
  createdAt: string;
}

/** Pagination metadata returned inside the `pageDetails` key of the API response. */
export interface ISchedulerAuditPageDetails {
  totalPages: number;
  totalRecords: number;
  totalRecordsPerPage: number;
  currentPageNumber: number;
}

/** Paged response wrapper returned by POST /api/scheduler-audit-logs/paginated. */
export interface ISchedulerAuditPagedResponse {
  pageDetails: ISchedulerAuditPageDetails;
  content: ISchedulerAuditLog[];
}
