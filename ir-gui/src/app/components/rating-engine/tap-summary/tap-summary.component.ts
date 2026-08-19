import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize, debounceTime } from 'rxjs/operators';
import {
  PaginationService,
  CommonService,
  tablePageSize,
  routes
} from 'src/app/core.index';
import { TapSummaryService } from './tap-summary.service';
import { ITapSummary, ITapSummaryPagedResponse, ITapSummarySearchCriteria } from './tap-summary.interface';

declare var bootstrap: any;

@Component({
  selector: 'app-tap-summary',
  templateUrl: './tap-summary.component.html',
  standalone: false 
})
export class TapSummaryComponent implements OnInit, OnDestroy {
  public routes = routes;
  private destroy$ = new Subject<void>();
  selectedCdrDetails: any = null;

  // Table Data
  tableData: ITapSummary[] = [];
  totalRecords = 0;
  pageSize = 10;
  paginationSkip = 0;
  serialNumberArray: number[] = [];

  // Filter Controls
  filterSummaryDate = new FormControl(null);
  filterFileName = new FormControl('');
  filterTapDirection = new FormControl('');
  filterPartnerName = new FormControl('');

  directionOptions = [
    { label: 'TAP IN', value: 'TAP_IN' },
    { label: 'TAP OUT', value: 'TAP_OUT' }
  ];

  private lastPayload = '';

  constructor(
    private tapSummaryService: TapSummaryService,
    private pagination: PaginationService,
    public commonService: CommonService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        this.pageSize = res.pageSize;
        
        const page = Math.floor(res.skip / res.pageSize) + 1;
        const criteria = this.buildSearchCriteria();
        const currentPayload = JSON.stringify({
          page: page,
          pageSize: res.pageSize,
          criteria: criteria
        });

        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.loadData(page, res.pageSize, criteria);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildSearchCriteria(): ITapSummarySearchCriteria {
    const criteria: ITapSummarySearchCriteria = {};
    
    if (this.filterSummaryDate.value) {
      criteria.summaryDate = this.formatDate(this.filterSummaryDate.value);
    }
    if (this.filterFileName.value?.trim()) {
      criteria.fileName = this.filterFileName.value.trim();
    }
    if (this.filterTapDirection.value) {
      criteria.tapDirection = this.filterTapDirection.value;
    }
    if (this.filterPartnerName.value?.trim()) {
      criteria.partnerName = this.filterPartnerName.value.trim();
    }
    
    return criteria;
  }

  loadData(page: number, limit: number, criteria: ITapSummarySearchCriteria): void {
    this.commonService.spinnerShow();
    this.tapSummaryService
      .getTapSummaries(page, limit, criteria)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (res: ITapSummaryPagedResponse) => {
          this.totalRecords = res.pageDetails.totalRecords;
          this.tableData = res.content || [];
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          
          this.pagination.calculatePageSize.next({
            totalData: this.totalRecords,
            pageSize: res.pageDetails.totalRecordsPerPage,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.msg || 'Failed to fetch TAP summaries'
          );
        }
      });
  }

  searchData(): void {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  clearSearch(): void {
    this.filterSummaryDate.setValue(null);
    this.filterFileName.setValue('');
    this.filterTapDirection.setValue('');
    this.filterPartnerName.setValue('');
    this.searchData();
  }

  downloadFile(row: ITapSummary): void {
    if (!row.tapFileId) {
      this.commonService.toastError('File ID not available for download');
      return;
    }

    this.commonService.spinnerShow();
    this.tapSummaryService
      .downloadTapFile(row.tapFileId)
      .pipe(takeUntil(this.destroy$), finalize(() => this.commonService.spinnerHide()))
      .subscribe({
        next: (blob: Blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = row.tapFileName || `TAP_File_${row.tapFileId}.bin`;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.msg || 'Failed to download TAP file'
          );
        }
      });
  }

  viewCdrs(row: ITapSummary): void {
    if (!row.tapFileId) {
      this.commonService.toastError('File ID not available to fetch CDR details');
      return;
    }
    const serviceType = row.serviceType || 'SMS';
    this.router.navigate(['/rating-engine/tap-summary/details', row.tapFileId], { queryParams: { serviceType } });
  }

  private formatDate(date: any): string {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  toLabel(value: string): string {
    if (!value) return '-';
    return value
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
      .join(' ');
  }
}
