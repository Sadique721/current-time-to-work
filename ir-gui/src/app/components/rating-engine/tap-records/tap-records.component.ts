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
import { ITapRecord, ITapRecordPagedResponse, ITapRecordSearchCriteria } from './tap-records.interface';
import { TapRecordsService } from './tap-records.service';

@Component({
  selector: 'app-tap-records',
  templateUrl: './tap-records.component.html',
  styleUrls: ['./tap-records.component.scss'],
  standalone: false,
})
export class TapRecordsComponent implements OnInit, OnDestroy {
  tableData: ITapRecord[] = [];
  totalData = 0;
  pageSize = 10;
  paginationSkip = 0;
  serialNumberArray: number[] = [];

  private activeSearchCriteria: ITapRecordSearchCriteria | undefined = undefined;
  private destroy$ = new Subject<void>();
  private lastPayload = '';

  filterSenderTadig = new FormControl('');
  filterRecipientTadig = new FormControl('');
  filterStatus = new FormControl('');
  filterFileType = new FormControl('');

  statusOptions = [
    { label: 'Received', value: 'RECEIVED' },
    { label: 'Processing', value: 'PROCESSING' },
    { label: 'Processed', value: 'PROCESSED' },
    { label: 'Failed', value: 'FAILED' },
  ];

  fileTypeOptions = [
    { label: 'TAP In', value: 'TAP_IN' },
    { label: 'TAP Out', value: 'TAP_OUT' },
  ];

  constructor(
    private pagination: PaginationService,
    private commonService: CommonService,
    private tapRecordsService: TapRecordsService
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

  private buildSearchCriteria(): ITapRecordSearchCriteria | undefined {
    const criteria: ITapRecordSearchCriteria = {};

    const senderTadig = this.filterSenderTadig.value?.trim();
    if (senderTadig) criteria.senderTadig = senderTadig;

    const recipientTadig = this.filterRecipientTadig.value?.trim();
    if (recipientTadig) criteria.recipientTadig = recipientTadig;

    const status = this.filterStatus.value?.trim();
    if (status) criteria.status = status;

    const fileType = this.filterFileType.value?.trim();
    if (fileType) criteria.fileType = fileType;

    return Object.keys(criteria).length > 0 ? criteria : undefined;
  }

  searchData(): void {
    this.activeSearchCriteria = this.buildSearchCriteria();
    this.lastPayload = '';
    this.loadData({ skip: 0, limit: this.pageSize });
  }

  clearSearch(): void {
    this.filterSenderTadig.setValue('');
    this.filterRecipientTadig.setValue('');
    this.filterStatus.setValue('');
    this.filterFileType.setValue('');
    this.activeSearchCriteria = undefined;
    this.lastPayload = '';
    this.loadData({ skip: 0, limit: this.pageSize });
  }

  private loadData(pageOption: pageSelection): void {
    const page = Math.floor(pageOption.skip / pageOption.limit) + 1;
    this.commonService.spinnerShow();
    this.tapRecordsService
      .getPaginatedTapRecords(page, pageOption.limit, this.activeSearchCriteria)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: ITapRecordPagedResponse) => {
          this.commonService.spinnerHide();
          this.totalData = res.pageDetails.totalRecords;
          this.tableData = res.content;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: res.pageDetails.totalRecordsPerPage,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(err?.error?.msg || 'Failed to load TAP records');
        },
      });
  }

  downloadFile(record: ITapRecord): void {
    this.commonService.spinnerShow();
    this.tapRecordsService
      .downloadTapFile(record.tapFileId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          this.commonService.spinnerHide();
          const url = window.URL.createObjectURL(blob);
          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download = record.fileName;
          anchor.style.display = 'none';
          document.body.appendChild(anchor);
          anchor.click();
          document.body.removeChild(anchor);
          window.URL.revokeObjectURL(url);
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(err?.error?.msg || 'Failed to download TAP file');
        },
      });
  }

  statusBadgeClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'PROCESSED':  return 'bg-success';
      case 'PROCESSING': return 'bg-warning';
      case 'RECEIVED':   return 'bg-info';
      case 'FAILED':     return 'bg-danger';
      default:           return 'bg-secondary';
    }
  }

  toLabel(value: string): string {
    if (!value) return '-';
    return value
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
      .join(' ');
  }
}
