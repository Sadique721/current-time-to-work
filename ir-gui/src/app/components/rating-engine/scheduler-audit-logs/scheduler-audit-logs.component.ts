import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import {
  PaginationService,
  CommonService,
  pageSelection,
  tablePageSize,
} from 'src/app/core.index';
import { ISchedulerAuditLog, ISchedulerAuditPagedResponse } from './scheduler-audit-logs.interface';
import { SchedulerAuditLogsService } from './scheduler-audit-logs.service';

@Component({
  selector: 'app-scheduler-audit-logs',
  templateUrl: './scheduler-audit-logs.component.html',
  styleUrls: ['./scheduler-audit-logs.component.scss'],
  standalone: false,
})
export class SchedulerAuditLogsComponent implements OnInit, OnDestroy {
  /** Slice of live data currently shown on the table page. */
  tableData: ISchedulerAuditLog[] = [];
  totalData = 0;
  pageSize = 10;
  paginationSkip = 0;
  serialNumberArray: number[] = [];

  /** Filter text bound to the search input. */
  searchDataValue = new FormControl('');

  private destroy$ = new Subject<void>();
  private lastPayload = '';

  constructor(
    private pagination: PaginationService,
    private commonService: CommonService,
    private auditService: SchedulerAuditLogsService
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const key = JSON.stringify({ skip: res.skip, limit: res.pageSize });
        if (this.lastPayload !== key) {
          this.lastPayload = key;
          this.pageSize = res.pageSize;
          this.loadData({ skip: res.skip, limit: res.pageSize });
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // DATA LOADING
  // ─────────────────────────────────────────────────────────────────────────────

  private loadData(pageOption: pageSelection): void {
    // The API uses 1-indexed pages; derive it from the skip/limit pair.
    const page = Math.floor(pageOption.skip / pageOption.limit) + 1;

    this.commonService.spinnerShow();
    this.auditService
      .getAuditLogs(page, pageOption.limit)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ISchedulerAuditPagedResponse) => {
          this.commonService.spinnerHide();
          // Extract pagination fields from the nested pageDetails object.
          this.totalData = res.pageDetails?.totalRecords || 0;
          this.tableData = res.content || [];
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: res.pageDetails?.totalRecordsPerPage || pageOption.limit,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(
            err?.error?.msg || 'Failed to load audit logs'
          );
        },
      });
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // SEARCH / FILTER
  // ─────────────────────────────────────────────────────────────────────────────

  searchData(): void {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({ skip: 0, limit: this.pageSize, pageSize: this.pageSize });
  }

  clearSearch(): void {
    this.searchDataValue.setValue('');
    this.lastPayload = '';
    this.pagination.tablePageSize.next({ skip: 0, limit: this.pageSize, pageSize: this.pageSize });
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // UTILITY HELPERS
  // ─────────────────────────────────────────────────────────────────────────────

  /**
   * Returns a short display label for the event type.
   */
  eventLabel(eventType: string): string {
    switch (eventType) {
      case 'SCHEDULER_EXECUTED_SUCCESS': return 'Success';
      case 'SCHEDULER_EXECUTED_FAILED':  return 'Failed';
      default:
        return eventType
          .split('_')
          .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
          .join(' ');
    }
  }

  /**
   * Returns the CSS badge class for the event type.
   */
  eventBadgeClass(eventType: string): string {
    switch (eventType) {
      case 'SCHEDULER_EXECUTED_SUCCESS': return 'badge-success';
      case 'SCHEDULER_EXECUTED_FAILED':  return 'badge-danger';
      default:                           return 'badge-secondary';
    }
  }
}

