import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { UserService, User } from './user.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

@Component({
  selector: 'app-user',
  templateUrl: './user.component.html',
  styleUrl: './user.component.scss',
  standalone: false,
})
export class UserComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: User[] = [];
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

  usernameConditionOptions = [
    { label: 'Begin with', value: 'beginWith' },
    { label: 'Contains', value: 'contains' },
    { label: 'Ends with', value: 'endsWith' },
    { label: 'Equals', value: 'equals' },
  ];

  emailConditionOptions = [
    { label: 'Begin with', value: 'beginWith' },
    { label: 'Contains', value: 'contains' },
    { label: 'Ends with', value: 'endsWith' },
    { label: 'Equals', value: 'equals' },
  ];

  userGroupOptions = [
    { label: 'auto outbounded', value: 'auto outbounded' },
    { label: 'Default', value: 'Default' },
    { label: 'Sales Team', value: 'Sales Team' },
    { label: 'Support Team', value: 'Support Team' },
  ];

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private userService: UserService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      username: [''],
      usernameCondition: ['beginWith'],
      userGroup: [''],
      email: [''],
      emailCondition: ['beginWith'],
      callRecording: [''],
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
          this.loadUsers();
        }
      });

    this.loadUsers();
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
    this.isLoading = true;
    

    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    

    setTimeout(() => {
      const mockData: User[] = [
        { 
          id: 1, 
          username: 'bharat', 
          email: 'Bharat.singh@unifyxcess.ai',
          defaultTimeout: 30,
          userRole: 'Agent',
          userGroup: 'auto outbounded',
          callRecording: true,
          status: true,
          defaultSipDevice: '1050',
          campaign: 'auto lead test',
          selected: false
        },
        { 
          id: 2, 
          username: 'Dev', 
          email: 'devshah2367@gmail.com',
          defaultTimeout: 30,
          userRole: 'Agent',
          userGroup: 'Default',
          callRecording: true,
          status: true,
          defaultSipDevice: '102',
          campaign: 'auto lead test',
          selected: false
        },
        { 
          id: 3, 
          username: 'agenttest', 
          email: 'agenttest@mail.com',
          defaultTimeout: 30,
          userRole: 'Agent',
          userGroup: 'Default',
          callRecording: true,
          status: true,
          campaign: 'Sale_IC',
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
      this.loadUsers();
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
      const mockData: User[] = [
        { 
          id: 1, 
          username: 'bharat', 
          email: 'Bharat.singh@unifyxcess.ai',
          defaultTimeout: 30,
          userRole: 'Agent',
          userGroup: 'auto outbounded',
          callRecording: true,
          status: true,
          defaultSipDevice: '1050',
          campaign: 'auto lead test',
          selected: false
        },
      ];

      this.tableData = mockData.filter(item => 
        item.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.email.toLowerCase().includes(searchTerm.toLowerCase())
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
    this.loadUsers();
  }

  toggleAllSelection(event: any): void {
    const checked = event.target.checked;
    this.tableData.forEach(item => item.selected = checked);
  }

  isAllSelected(): boolean {
    return this.tableData.length > 0 && this.tableData.every(item => item.selected);
  }


  toggleStatus(item: User): void {
    item.status = !item.status;
        this.commonService.toastSuccess(`User ${item.status ? 'enabled' : 'disabled'} successfully`);
  }

  openUserAddEdit(item: User | null = null): void {
    const state: any = {};
    if (item?.id) {
      state.id = item.id;
    }
    this.router.navigate([this.routes.user + '/add-edit'], {
      state,
    });
  }

  toggleFilters(): void {
    this.isFiltersOpen = !this.isFiltersOpen;
  }

  applyFilters(): void {
    this.isSearchMode = true;
    this.currentPage = 1;
    this.paginationSkip = 0;
    
    const filters = this.filtersForm.value;
        
    const searchTerm = this.searchDataValue.value || '';
    this.performSearch(searchTerm.toString().trim());
    
    this.isFiltersOpen = false;
    this.commonService.toastSuccess('Filters applied');
  }

  clearFilters(): void {
    this.filtersForm.reset({
      username: '',
      usernameCondition: 'beginWith',
      userGroup: '',
      email: '',
      emailCondition: 'beginWith',
      callRecording: '',
      status: ''
    });
    this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.isFiltersOpen = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadUsers();
  }

  hasSelectedItems(): boolean {
    return this.tableData.some(item => item.selected);
  }

  getSelectedIds(): number[] {
    return this.tableData
      .filter(item => item.selected)
      .map(item => item.id);
  }

  confirmBulkDelete(): void {
    const selectedIds = this.getSelectedIds();
    const count = selectedIds.length;

    if (count === 0) {
      this.commonService.toastError('Please select users to delete');
      return;
    }

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: "Delete Users",
        message: `Are you sure you want to delete ${count} selected user(s)?`,
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.bulkDeleteUsers(selectedIds);
        }
      });
  }

  private bulkDeleteUsers(ids: number[]): void {
    this.commonService.spinnerShow();

    setTimeout(() => {
      this.commonService.toastSuccess(`${ids.length} user(s) deleted successfully`);
      
      this.isReloadingAfterDelete = true;
      
      const remainingItems = this.tableData.filter(item => !item.selected).length;
      const willBeEmpty = remainingItems === 0;
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
          this.loadUsers();
        }
      }
      
      this.commonService.spinnerHide();
    }, 500);
  }

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: "Delete User",
        message: "Are you sure you want to delete this user?",
        confirmButtonText: "Yes Delete",
        cancelButtonText: "Cancel",
        iconClass: "ti ti-trash fs-24 text-danger",
      },
    });

    dialogRef.afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.deleteUser(id);
        }
      });
  }

  private deleteUser(id: number): void {
    this.commonService.spinnerShow();

    setTimeout(() => {
      this.commonService.toastSuccess('User deleted successfully');
      
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
          this.loadUsers();
        }
      }
      
      this.commonService.spinnerHide();
    }, 500);
  }
}