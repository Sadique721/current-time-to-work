import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { CallScriptService } from './call-script.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

interface CallScriptRow {
  id: number;
  callScriptName: string;
  script: string;
  description?: string;
  status: boolean;
  selected?: boolean;
}

@Component({
  selector: 'app-call-script',
  templateUrl: './call-script.component.html',
  styleUrl: './call-script.component.scss',
  standalone: false,
})
export class CallScriptComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: CallScriptRow[] = [];
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
    private callScriptService: CallScriptService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      callScriptName: [''],
      callScriptNameCondition: ['beginWith'],
      description: [''],
      descriptionCondition: ['beginWith'],
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
          this.loadCallScripts();
        }
      });

    this.loadCallScripts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private loadCallScripts(): void {
    this.isLoading = true;
    
    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    this.callScriptService.getAll(this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data.map(item => ({
            id: item.id,
            callScriptName: item.callScriptName,
            script: item.script,
            description: item.description,
            status: item.status,
            selected: false
          }));
          
          this.totalData = response.totalCount;
          
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
          this.commonService.toastError('Failed to load call scripts');
          this.isLoading = false;
          
          if (!this.isReloadingAfterDelete) {
            this.commonService.spinnerHide();
          } else {
            this.isReloadingAfterDelete = false;
          }
        }
      });
  }

  searchData(): void {
    const searchTerm = (this.searchDataValue.value || '').toString().trim();

    if (!searchTerm) {
      this.isSearchMode = false;
      this.currentPage = 0;
      this.paginationSkip = 0;
      this.loadCallScripts();
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

    this.callScriptService.search(searchTerm, this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tableData = response.data.map(item => ({
            id: item.id,
            callScriptName: item.callScriptName,
            script: item.script,
            description: item.description,
            status: item.status,
            selected: false
          }));
          
          this.totalData = response.totalCount;
          
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
          this.commonService.toastError('Failed to search call scripts');
          this.isLoading = false;
          
          if (!this.isReloadingAfterDelete) {
            this.commonService.spinnerHide();
          } else {
            this.isReloadingAfterDelete = false;
          }
        }
      });
  }

  toggleStatus(item: CallScriptRow): void {
    const newStatus = !item.status;
    
    this.callScriptService.update(item.id, {
      callScriptName: item.callScriptName,
      description: item.description,
      script: item.script,
      status: newStatus
    }).pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          item.status = newStatus;
          this.commonService.toastSuccess(`Call script ${newStatus ? 'enabled' : 'disabled'} successfully`);
        },
        error: (error) => {
          this.commonService.toastError('Failed to update status');
        }
      });
  }

  openCallScriptAddEdit(item: CallScriptRow | null = null): void {
    const state: any = {};
    if (item?.id) {
      state.id = item.id;
    }
    this.router.navigate([this.routes.callscript + '/add-edit'], {
      state,
    });
  }

  clearSearch(): void {
    this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 0;
    this.paginationSkip = 0;
    this.loadCallScripts();
  }

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: "Delete Call Script",
        message: "Are you sure you want to delete this call script?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.deleteCallScript(id);
        }
      });
  }

  private deleteCallScript(id: number): void {
    this.commonService.spinnerShow();

    this.callScriptService.delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess('Call script deleted successfully');
          
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
              this.loadCallScripts();
            }
          }
          
          this.commonService.spinnerHide();
        },
        error: (error) => {
          this.commonService.toastError('Failed to delete call script');
          this.commonService.spinnerHide();
        }
      });
  }
}