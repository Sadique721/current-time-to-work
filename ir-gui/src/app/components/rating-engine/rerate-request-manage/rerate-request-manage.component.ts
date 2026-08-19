import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime, Subject, takeUntil } from 'rxjs';
import { CommonService, PaginationService, pageSelection, tablePageSize, SidebarService } from 'src/app/core.index';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { ReRateRequestDTO, ReRateRequestService } from './rerate-request.service';
import { Router } from '@angular/router';
import { routes } from 'src/app/core/helpers/routes';

@Component({
  selector: 'app-rerate-request-manage',
  templateUrl: './rerate-request-manage.component.html',
  standalone: false,
  styleUrls: []
})
export class ReRateRequestManageComponent implements OnInit, OnDestroy {
  tableData: ReRateRequestDTO[] = [];
  selectedRequest: ReRateRequestDTO | null = null;
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  searchControl = new FormControl('');
  private destroy$ = new Subject<void>();
  private lastPayload = '';

  isCollapsed = false;

  constructor(
    private pagination: PaginationService,
    private svc: ReRateRequestService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router,
    private sidebar: SidebarService
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const payload = JSON.stringify({ skip: res.skip, limit: res.pageSize, search: this.searchControl.value?.trim() });
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
    this.svc.fetchPage(page, limit).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.tableData = res.content || [];
        this.totalData = res.totalElements || 0;
        this.pagination.calculatePageSize.next({
          totalData: this.totalData,
          pageSize: this.pageSize,
          tableData: this.tableData,
          serialNumberArray: this.tableData.map((_, i) => i + 1)
        });
      },
      error: () => {
        this.commonService.spinnerHide();
        this.commonService.toastError('Failed to load re-rate requests');
      }
    });
  }

  searchData(): void {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({ skip: 0, limit: this.pageSize, pageSize: this.pageSize });
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.searchData();
  }

  openAddEdit(item: ReRateRequestDTO | null = null): void {
    if (item?.id) {
      this.router.navigate(['/rating-engine/rerate-requests/add-edit', item.id]);
    } else {
      this.router.navigate(['/rating-engine/rerate-requests/add-edit']);
    }
  }

  reRun(item: ReRateRequestDTO): void {
    if (!item.requestId) return;
    this.dialog.open(ConfirmDialogComponent, {
      width: '420px', disableClose: true,
      data: { title: 'Re-Run Request', message: 'Are you sure you want to re-run this request?', confirmButtonText: 'Yes, Re-Run', cancelButtonText: 'Cancel', iconClass: 'ti ti-refresh fs-24 text-info' }
    }).afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.svc.editReRateStatus(item.requestId!).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => { this.commonService.toastSuccess('Re-Run initiated'); this.searchData(); },
        error: () => this.commonService.toastError('Failed to initiate Re-Run')
      });
    });
  }

  confirmDelete(item: ReRateRequestDTO): void {
    if (!item.id) return;
    this.dialog.open(ConfirmDialogComponent, {
      width: '420px', disableClose: true,
      data: { title: 'Delete ReRate Request', message: 'Are you sure you want to delete this request?', confirmButtonText: 'Yes Delete', cancelButtonText: 'Cancel', iconClass: 'ti ti-trash fs-24 text-danger' }
    }).afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.svc.delete(item.id!).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => { this.commonService.toastSuccess('Deleted successfully'); this.searchData(); },
        error: () => this.commonService.toastError('Delete failed')
      });
    });
  }

  getStatusClass(status: string | undefined): string {
    const map: any = { NEW: 'badge badge-primary', COMPLETED: 'badge badge-success', FAILED: 'badge badge-danger', IN_PROGRESS: 'badge badge-warning', PENDING: 'badge badge-info' };
    return map[status?.toUpperCase() ?? ''] || 'badge badge-secondary';
  }

  isLocked(status: string | undefined): boolean {
    return ['COMPLETED', 'IN_PROGRESS'].includes(status?.toUpperCase() ?? '');
  }

  canReRun(status: string | undefined): boolean {
    return ['COMPLETED', 'FAILED'].includes(status?.toUpperCase() ?? '');
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
