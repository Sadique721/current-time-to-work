import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { UserRoleService, UserRole } from './user-role.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

@Component({
  selector: 'app-user-role',
  templateUrl: './user-role.component.html',
  styleUrl: './user-role.component.scss',
  standalone: false,
})
export class UserRoleComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: UserRole[] = [];
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

  roleNameConditionOptions = [
    { label: 'Begin with', value: 'beginWith' },
    { label: 'Contains', value: 'contains' },
    { label: 'Ends with', value: 'endsWith' },
    { label: 'Equals', value: 'equals' },
  ];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private userRoleService: UserRoleService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      roleName: [''],
      roleNameCondition: ['beginWith'],
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
          const searchTerm = (this.searchDataValue.value || '').toString().trim();
          this.performSearch(searchTerm);
        } else {
          this.loadUserRoles();
        }
      });
    this.loadUserRoles();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  private loadUserRoles(): void {
    this.isLoading = true;
    
    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

        setTimeout(() => {
      const mockData: UserRole[] = [
        { 
          id: 1, 
          name: 'test role for agent',
          status: true,
          pbxMode: false,
          cdrReport: false,
          loginLogoutReport: false,
          callCenterMode: false,
          recording: false,
          followUp: false,
          stickyAgent: false,
          numberMasking: false,
          setting: false,
          breakcode: false,
          allowBlackList: false,
          whatsapp: false,
          selected: false
        },
        { 
          id: 2, 
          name: 'Default',
          status: true,
          pbxMode: true,
          cdrReport: true,
          loginLogoutReport: true,
          callCenterMode: true,
          recording: true,
          followUp: true,
          stickyAgent: false,
          numberMasking: true,
          setting: true,
          breakcode: true,
          allowBlackList: false,
          whatsapp: true,
          selected: false
        },
      ];

      this.tableData = mockData;
      this.totalData = mockData.length;
      
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
      
          }, 500);
  }

  searchData(): void {
    const searchTerm = (this.searchDataValue.value || '').toString().trim();

    
    if (!searchTerm) {
      this.isSearchMode = false;
      this.currentPage = 1;
      this.paginationSkip = 0;
      this.loadUserRoles();
      return;
    }

    this.isSearchMode = true;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.performSearch(searchTerm);
  }

  private performSearch(searchTerm: string): void {
    this.isLoading = true;
    
    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    
    setTimeout(() => {
      const mockData: UserRole[] = [
        { 
          id: 1, 
          name: 'test role for agent',
          status: true,
          pbxMode: false,
          cdrReport: false,
          loginLogoutReport: false,
          callCenterMode: false,
          recording: false,
          followUp: false,
          stickyAgent: false,
          numberMasking: false,
          setting: false,
          breakcode: false,
          allowBlackList: false,
          whatsapp: false,
          selected: false
        },
      ];

      this.tableData = mockData.filter(item => 
        item.name.toLowerCase().includes(searchTerm.toLowerCase())
      );
      this.totalData = this.tableData.length;
      
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
      
          }, 500);
  }

  clearSearch(): void {
        this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadUserRoles();
  }


  toggleStatus(item: UserRole): void {
    item.status = !item.status;
        this.commonService.toastSuccess(`User Role ${item.status ? 'enabled' : 'disabled'} successfully`);
  }

  openUserRoleAddEdit(item: UserRole | null = null): void {
    const state: any = {};
    if (item?.id) {
      state.id = item.id;
    }
    this.router.navigate([this.routes.userrole + '/add-edit'], {
      state,
    });
  }

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: "Delete User Role",
        message: "Are you sure you want to delete this user role?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.deleteUserRole(id);
        }
      });
  }

  private deleteUserRole(id: number): void {
    this.commonService.spinnerShow();

    setTimeout(() => {
      this.commonService.toastSuccess('User role deleted successfully');
      
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
      } else {
        if (this.isSearchMode) {
          const searchTerm = (this.searchDataValue.value || '').toString().trim();
          this.performSearch(searchTerm);
        } else {
          this.loadUserRoles();
        }
      }
      
      this.commonService.spinnerHide();
    }, 500);
  }
}