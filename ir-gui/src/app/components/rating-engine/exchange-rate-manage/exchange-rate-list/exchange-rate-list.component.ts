import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, finalize, debounceTime } from 'rxjs/operators';
import {
  PaginationService,
  CommonService,
  pageSelection,
  tablePageSize,
  routes
} from 'src/app/core.index';
import { ExchangeRateService } from '../exchange-rate.service';

@Component({
  selector: 'app-exchange-rate-list',
  templateUrl: './exchange-rate-list.component.html',
  styleUrls: ['./exchange-rate-list.component.scss'],
  standalone: false
})
export class ExchangeRateListComponent implements OnInit, OnDestroy {
  public routes = routes;
  private destroy$ = new Subject<void>();

  // Table Data
  ratesList: any[] = [];
  totalRecords = 0;
  pageSize = 10;
  paginationSkip = 0;
  serialNumberArray: number[] = [];

  searchControl = new FormControl("");
  dateControl = new FormControl(null);
  private lastPayload = "";

  constructor(
    private exchangeRateService: ExchangeRateService,
    private pagination: PaginationService,
    private commonService: CommonService
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        this.pageSize = res.pageSize;
        
        const page = Math.floor(res.skip / res.pageSize) + 1;
        const dateValue = this.dateControl.value ? this.formatDate(this.dateControl.value) : "";
        const currentPayload = JSON.stringify({
          page: page,
          pageSize: res.pageSize,
          search: this.searchControl.value?.trim(),
          validFrom: dateValue,
        });

        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.loadData(page, res.pageSize);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadData(page: number, limit: number): void {
    const searchValue = this.searchControl.value?.trim() || '';
    this.commonService.spinnerShow();
    const dateValue = this.dateControl.value ? this.formatDate(this.dateControl.value) : "";
    this.exchangeRateService
      .getExchangeRates(page, limit, searchValue, dateValue)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (res: any) => {
          this.totalRecords = res.pageDetails.totalRecords;
          this.ratesList = res.content || [];
          this.serialNumberArray = this.ratesList.map((_, i) => i + 1);
          
          this.pagination.calculatePageSize.next({
            totalData: this.totalRecords,
            pageSize: res.pageDetails.totalRecordsPerPage,
            tableData: this.ratesList,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.msg || 'Failed to fetch exchange rates'
          );
        }
      });
  }

  private formatDate(date: any): string {
    if (!date) return "";
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  fetchData(): void {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  clearSearch(): void {
    this.searchControl.setValue("");
    this.dateControl.setValue(null);
    this.fetchData();
  }

  refreshList(): void {
    this.fetchData();
  }
}
