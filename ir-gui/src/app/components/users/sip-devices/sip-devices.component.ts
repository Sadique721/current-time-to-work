import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { SipDevicesService, SipDevice } from './sip-devices.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

@Component({
  selector: 'app-sip-devices',
  templateUrl: './sip-devices.component.html',
  styleUrl: './sip-devices.component.scss',
  standalone: false,
})
export class SipDevicesComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: SipDevice[] = [];
  pageSize = 10;
  totalData = 0;
  paginationSkip = 0;
  currentPage = 1;
  serialNumberArray: Array<number> = [];

  searchDataValue = new FormControl('');
  filtersForm: FormGroup;
  isCollapsed = false;
  isLoading = false;
  isSearchMode = false;
  isFiltersOpen = false;

  
  private isUpdatingPagination = false;
  
  
  private isReloadingAfterDelete = false;

  private destroy$ = new Subject<void>();

  permission: IPermission = {
    create: true,
    edit: true,
    delete: true,
  };

  
  userOptions: Array<{ label: string; value: number }> = [];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private sipDevicesService: SipDevicesService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    
    this.filtersForm = this.fb.group({
      deviceName: [''],
      username: [''],
      userId: [''],
      status: ['']
    });
  }

  ngOnInit(): void {
        
    
    this.loadUsers();
    
    
    this.paginationService.tablePageSize
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(50)
      )
      .subscribe((res: any) => {
                
        if (this.isUpdatingPagination) {
                    return;
        }
        
        if (this.isLoading) {
                    return;
        }
        
        const newSkip = res.skip;
        const newPageSize = res.pageSize;
        const newPage = Math.floor(newSkip / newPageSize) + 1;
        
        if (newSkip === this.paginationSkip && newPageSize === this.pageSize) {
                    return;
        }
        
        this.paginationSkip = newSkip;
        this.pageSize = newPageSize;
        this.currentPage = newPage;
        
                
        
        if (this.isSearchMode) {
          this.performSearch();
        } else {
          this.loadSipDevices();
        }
      });

    
    this.loadSipDevices();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private loadUsers(): void {
    this.sipDevicesService.getUsers()
      .pipe(takeUntil(this.destroy$))
      .subscribe((users) => {
        this.userOptions = users;
      });
  }


  private loadSipDevices(): void {
  this.isLoading = true;
  
  if (!this.isReloadingAfterDelete) {
    this.commonService.spinnerShow();
  }

  
  this.sipDevicesService.getAll(this.currentPage, this.pageSize)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (response) => {
                
        this.tableData = response.data;
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
                this.isLoading = false;
        this.commonService.spinnerHide();
        this.commonService.toastError('Failed to load SIP devices');
      }
    });
}



searchData(): void {
  const searchTerm = (this.searchDataValue.value || '').toString().trim();

  
  if (!searchTerm) {
    this.isSearchMode = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadSipDevices();
    return;
  }

  this.isSearchMode = true;
  this.currentPage = 1;
  this.paginationSkip = 0;
  this.performSearch();
}

private performSearch(): void {
  const searchTerm = (this.searchDataValue.value || '').toString().trim();
  
  if (!searchTerm) {
    this.isSearchMode = false;
    this.loadSipDevices();
    return;
  }

  this.isLoading = true;
  this.commonService.spinnerShow();

  
  this.sipDevicesService.search(searchTerm, this.currentPage, this.pageSize)
    .pipe(takeUntil(this.destroy$))
    .subscribe({
      next: (response) => {
                
        this.tableData = response.data;
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
        this.commonService.spinnerHide();
      },
      error: (error) => {
                this.isLoading = false;
        this.commonService.spinnerHide();
        this.commonService.toastError('Failed to search devices');
      }
    });
}

    toggleStatus(item: SipDevice): void {
    const newStatus = item.status === "1" ? "0" : "1";
    
    this.commonService.spinnerShow();
    
    this.sipDevicesService.update(item.id, { ...item, status: newStatus })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          item.status = newStatus;
          this.commonService.spinnerHide();
          this.commonService.toastSuccess(`Device ${newStatus === "1" ? 'enabled' : 'disabled'} successfully`);
        },
        error: (error) => {
                    this.commonService.spinnerHide();
          this.commonService.toastError('Failed to update device status');
        }
      });
  }

    openSipDeviceAddEdit(item: SipDevice | null = null): void {
    const state: any = {};
    if (item?.id) {
      state.id = item.id;
    }
    this.router.navigate([this.routes.sipdevices + '/add-edit'], {
      state,
    });
  }

    openOtherFeatures(item: SipDevice): void {
    this.router.navigate([this.routes.sipdevices + '/other-features'], {
      state: { id: item.id },
    });
  }

    confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: "Delete SIP Device",
        message: "Are you sure you want to delete this SIP Device?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.deleteSipDevice(id);
        }
      });
  }

    private deleteSipDevice(id: number): void {
    this.commonService.spinnerShow();

    this.sipDevicesService.delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess('SIP device deleted successfully');
          
          this.isReloadingAfterDelete = true;
          
          const willBeEmpty = this.tableData.length === 1;
          const notFirstPage = this.currentPage > 1;
          
          if (willBeEmpty && notFirstPage) {
            this.currentPage--;
            this.paginationSkip = (this.currentPage - 1) * this.pageSize;
            
            this.isUpdatingPagination = true;
            
            this.paginationService.tablePageSize.next({
              skip: this.paginationSkip,
              limit: this.paginationSkip + this.pageSize,
              pageSize: this.pageSize
            });
            
            setTimeout(() => {
              this.isUpdatingPagination = false;
            }, 100);
          }
          
          if (this.isSearchMode) {
            this.performSearch();
          } else {
            this.loadSipDevices();
          }
          
          this.commonService.spinnerHide();
        },
        error: (error) => {
                    this.commonService.spinnerHide();
          this.commonService.toastError('Failed to delete device');
        }
      });
  }

  clearSearch(): void {
        this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadSipDevices();
  }
}