import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { TelecomCircleService } from './telecom-circle.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

interface TelecomCircleRow {
  id: number;
  name: string;
  prefix: string;
  status: string;
  selected?: boolean;
}

@Component({
  selector: 'app-telecom-circle',
  templateUrl: './telecom-circle.component.html',
  styleUrl: './telecom-circle.component.scss',
  standalone: false,
})
export class TelecomCircleComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: TelecomCircleRow[] = [];
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  currentPage = 0;
  serialNumberArray: Array<number> = [];

  searchDataValue = new FormControl('');
  filtersForm: FormGroup;
  isCollapsed = false;
  isLoading = false;
  isSearchMode = false;
  isFiltersOpen = false;

  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  private isUpdatingPagination = false;
  private isReloadingAfterDelete = false;
  private destroy$ = new Subject<void>();

  permission: IPermission = {
    create: true,
    edit: true,
    delete: true,
  };

  nameConditionOptions = [
    { label: 'Begin with', value: 'beginWith' },
    { label: 'Contains', value: 'contains' },
    { label: 'Ends with', value: 'endsWith' },
    { label: 'Equals', value: 'equals' },
  ];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private telecomCircleService: TelecomCircleService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      circleName: [''],
      circleNameCondition: ['beginWith'],
      prefix: [''],
      prefixCondition: ['beginWith'],
      status: ['']
    });
  }

  ngOnInit(): void {
    this.paginationService.tablePageSize
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(50)
      )
      .subscribe((res: any) => {
        if (this.isUpdatingPagination || this.isLoading) {
          return;
        }
        
        const newSkip = res.skip;
        const newPageSize = res.pageSize;
        const newPage = Math.floor(newSkip / newPageSize);
        
        if (newSkip === this.paginationSkip && newPageSize === this.pageSize) {
          return;
        }
        
        this.paginationSkip = newSkip;
        this.pageSize = newPageSize;
        this.currentPage = newPage;
        
        if (this.isSearchMode) {
          const searchTerm = (this.searchDataValue.value || '').toString().trim();
          this.performSearch(searchTerm);
        } else {
          this.loadTelecomCircles();
        }
      });

    this.loadTelecomCircles();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private loadTelecomCircles(): void {
    this.isLoading = true;
    
    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    this.telecomCircleService.getAll(this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data.map(item => ({
            id: item.id,
            name: item.name,
            prefix: item.prefix,
            status: item.status,
            selected: false
          }));
          
          this.totalData = response.pagination.totalRecords;
          
          this.serialNumberArray = Array.from(
            { length: this.tableData.length },
            (_, i) => this.paginationSkip + i + 1
          );

          this.isUpdatingPagination = true;
          
          this.paginationService.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
          
          setTimeout(() => {
            this.isUpdatingPagination = false;
          }, 100);

          this.isLoading = false;
          
          if (!this.isReloadingAfterDelete) {
            this.commonService.spinnerHide();
          } else {
            this.isReloadingAfterDelete = false;
          }
        },
        error: (error) => {
          this.commonService.toastError('Failed to load telecom circles');
          this.isLoading = false;
          this.commonService.spinnerHide();
        }
      });
  }

  searchData(): void {
    const searchTerm = (this.searchDataValue.value || '').toString().trim();

    if (!searchTerm) {
      this.isSearchMode = false;
      this.currentPage = 0;
      this.paginationSkip = 0;
      this.loadTelecomCircles();
      return;
    }

    this.isSearchMode = true;
    this.currentPage = 0;
    this.paginationSkip = 0;
    this.performSearch(searchTerm);
  }

  private performSearch(searchTerm: string): void {
    this.isLoading = true;
    
    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    this.telecomCircleService.search(searchTerm, this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data.map(item => ({
            id: item.id,
            name: item.name,
            prefix: item.prefix,
            status: item.status,
            selected: false
          }));
          
          this.totalData = response.pagination.totalRecords;
          
          this.serialNumberArray = Array.from(
            { length: this.tableData.length },
            (_, i) => this.paginationSkip + i + 1
          );

          this.isUpdatingPagination = true;
          
          this.paginationService.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
          
          setTimeout(() => {
            this.isUpdatingPagination = false;
          }, 100);

          this.isLoading = false;
          
          if (!this.isReloadingAfterDelete) {
            this.commonService.spinnerHide();
          } else {
            this.isReloadingAfterDelete = false;
          }
        },
        error: (error) => {
          this.commonService.toastError('Failed to search telecom circles');
          this.isLoading = false;
          this.commonService.spinnerHide();
        }
      });
  }

  toggleStatus(item: TelecomCircleRow): void {
    const newStatus = item.status === '1' ? '0' : '1';
    
    const payload = {
      name: item.name,
      prefix: item.prefix,
      status: newStatus
    };

    this.telecomCircleService.update(item.id, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          item.status = newStatus;
          this.commonService.toastSuccess(`Telecom circle ${newStatus === '1' ? 'enabled' : 'disabled'} successfully`);
        },
        error: (error) => {
          this.commonService.toastError('Failed to update status');
        }
      });
  }

  openTelecomCircleAddEdit(item: TelecomCircleRow | null = null): void {
    const state: any = {};
    if (item?.id) {
      state.id = item.id;
    }
    this.router.navigate([this.routes.telecomcircle + '/add-edit'], {
      state,
    });
  }

  clearSearch(): void {
    this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 0;
    this.paginationSkip = 0;
    this.loadTelecomCircles();
  }

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: "Delete Telecom Circle",
        message: "Are you sure you want to delete this telecom circle?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.deleteTelecomCircle(id);
        }
      });
  }

  private deleteTelecomCircle(id: number): void {
    this.commonService.spinnerShow();

    this.telecomCircleService.delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess('Telecom circle deleted successfully');
          
          this.isReloadingAfterDelete = true;
          
          const willBeEmpty = this.tableData.length === 1;
          const notFirstPage = this.currentPage > 0;
          
          if (willBeEmpty && notFirstPage) {
            this.currentPage--;
            this.paginationSkip = this.currentPage * this.pageSize;
            
            this.isUpdatingPagination = true;
            
            this.paginationService.tablePageSize.next({
              skip: this.paginationSkip,
              limit: this.paginationSkip + this.pageSize,
              pageSize: this.pageSize
            });
            
            setTimeout(() => {
              this.isUpdatingPagination = false;
            }, 100);
          } else {
            if (this.isSearchMode) {
              const searchTerm = (this.searchDataValue.value || '').toString().trim();
              this.performSearch(searchTerm);
            } else {
              this.loadTelecomCircles();
            }
          }
          
          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.toastError('Failed to delete telecom circle');
          this.commonService.spinnerHide();
        }
      });
  }

   getStatusLabel(status: boolean | string): string {
    return status === false || status === '0' ? 'Inactive' : 'Active';
  }

  getStatusClass(status: boolean | string): string {
    return status === false || status === '0' ? 'badge badge-danger' : 'badge badge-success';
  }
}