import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { CommonService, PaginationService, tablePageSize, SidebarService } from 'src/app/core.index';
import { ErrorAnalysisService, ErrorConfigCheckStatusDTO } from './error-analysis.service';

@Component({
  selector: 'app-error-analysis',
  templateUrl: './error-analysis.component.html',
  styleUrls: [],
  standalone: false
})
export class ErrorAnalysisComponent implements OnInit, OnDestroy {
  tableData: ErrorConfigCheckStatusDTO[] = [];

  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  private destroy$ = new Subject<void>();
  private lastPayload = '';
  public isCollapsed = false;

  constructor(
    private pagination: PaginationService,
    private errorAnalysisService: ErrorAnalysisService,
    private commonService: CommonService,
    private sidebar: SidebarService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const payload = JSON.stringify({ skip: res.skip, limit: res.pageSize });
        if (this.lastPayload !== payload) {
          this.lastPayload = payload;
          this.pageSize = res.pageSize;
          this.loadData(res.skip, res.pageSize);
        }
      });
  }

  private loadData(skip: number, limit: number): void {
    const page = skip / limit;
    this.commonService.spinnerShow();
    this.errorAnalysisService.fetchErrorConfigCheckResults(page, limit).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.tableData = res.content || res || [];
        this.totalData = res.totalElements || res.length || 0;
        this.pagination.calculatePageSize.next({
          totalData: this.totalData,
          pageSize: this.pageSize,
          tableData: this.tableData,
          serialNumberArray: this.tableData.map((_, i) => i + 1)
        });
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(err?.error?.message || 'Failed to load error records');
      }
    });
  }

  refreshData(): void {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({ skip: 0, limit: this.pageSize, pageSize: this.pageSize });
  }

  viewDetails(item: ErrorConfigCheckStatusDTO): void {
    this.router.navigate(['/rating-engine/error-analysis/details', item.id], { state: { data: item } });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
