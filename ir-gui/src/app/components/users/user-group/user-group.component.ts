import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import { routes, SidebarService, CommonService, PaginationService } from 'src/app/core.index';
import { UserGroupService, UserGroup } from './user-group.service';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';
import { MatDialog } from '@angular/material/dialog';

interface IPermission {
  create: boolean;
  edit: boolean;
  delete: boolean;
}

@Component({
  selector: 'app-user-group',
  templateUrl: './user-group.component.html',
  styleUrl: './user-group.component.scss',
  standalone: false,
})
export class UserGroupComponent implements OnInit, OnDestroy {
  public routes = routes;

  tableData: UserGroup[] = [];
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

  private isUpdatingPagination = false;
  private isReloadingAfterDelete = false;
  private destroy$ = new Subject<void>();

  permission: IPermission = {
    create: true,
    edit: true,
    delete: true,
  };

  constructor(
    private router: Router,
    private fb: FormBuilder,
    private userGroupService: UserGroupService,
    private dialog: MatDialog,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private paginationService: PaginationService
  ) {
    this.filtersForm = this.fb.group({
      name: [''],
      nameCondition: ['beginWith'],
      outboundRule: [''],
      status: [''],
    });
  }

  ngOnInit(): void {
    this.paginationService.tablePageSize
      .pipe(takeUntil(this.destroy$), debounceTime(50))
      .subscribe((res: any) => {
        if (this.isUpdatingPagination || this.isLoading) return;

        const newSkip = res.skip;
        const newPageSize = res.pageSize;

        if (newSkip === this.paginationSkip && newPageSize === this.pageSize) return;

        this.paginationSkip = newSkip;
        this.pageSize = newPageSize;
        this.currentPage = Math.floor(newSkip / newPageSize) + 1;

        if (this.isSearchMode) {
          const keyword = (this.searchDataValue.value || '').toString().trim();
          this.performSearch(keyword);
        } else {
          this.loadUserGroups();
        }
      });

    this.loadUserGroups();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  

  private loadUserGroups(): void {
    this.isLoading = true;

    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    this.userGroupService
      .getAll(this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.tableData = res.data;
          this.totalData = res.totalCount;
          this.updateSerialNumbers();
          this.updatePagination();
          this.finishLoading();
        },
        error: (err) => {
          console.error('Failed to load user groups', err);
          this.commonService.toastError('Failed to load user groups');
          this.finishLoading();
        },
      });
  }

  

  searchData(): void {
    const keyword = (this.searchDataValue.value || '').toString().trim();

    if (!keyword) {
      this.clearSearch();
      return;
    }

    this.isSearchMode = true;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.performSearch(keyword);
  }

  private performSearch(keyword: string): void {
    this.isLoading = true;

    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerShow();
    }

    this.userGroupService
      .search(keyword, this.currentPage, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.tableData = res.data;
          this.totalData = res.totalCount;
          this.updateSerialNumbers();
          this.updatePagination();
          this.finishLoading();
        },
        error: (err) => {
          console.error('Search failed', err);
          this.commonService.toastError('Search failed');
          this.finishLoading();
        },
      });
  }

  clearSearch(): void {
    this.searchDataValue.setValue('');
    this.isSearchMode = false;
    this.currentPage = 1;
    this.paginationSkip = 0;
    this.loadUserGroups();
  }

  

  toggleAllSelection(event: any): void {
    const checked = event.target.checked;
    this.tableData.forEach(item => (item.selected = checked));
  }

  isAllSelected(): boolean {
    return this.tableData.length > 0 && this.tableData.every(item => item.selected);
  }

  onRowSelectionChange(): void {}

  

  toggleStatus(item: UserGroup): void {
    const updatedStatus = !item.status;
    const payload: Partial<UserGroup> = {
      name: item.name,
      outgoingRule: item.outgoingRule,
      status: updatedStatus,
      staffId: item.staffId,
    };

    this.userGroupService
      .update(item.id, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          item.status = updatedStatus;
          this.commonService.toastSuccess(
            `User Group ${updatedStatus ? 'enabled' : 'disabled'} successfully`
          );
        },
        error: (err) => {
          console.error('Status toggle failed', err);
          this.commonService.toastError('Failed to update status');
        },
      });
  }

  

  openUserGroupAddEdit(item: UserGroup | null = null): void {
    const state: any = {};
    if (item?.id) state.id = item.id;
    this.router.navigate([this.routes.usergroup + '/add-edit'], { state });
  }

  

  confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      disableClose: true,
      data: {
        title: 'Delete User Group',
        message: 'Are you sure you want to delete this user group?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger',
      },
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) this.deleteUserGroup(id);
      });
  }

  private deleteUserGroup(id: number): void {
    this.commonService.spinnerShow();

    this.userGroupService
      .delete(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.commonService.toastSuccess('User group deleted successfully');
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
              pageSize: this.pageSize,
            });
            setTimeout(() => (this.isUpdatingPagination = false), 100);
          } else {
            if (this.isSearchMode) {
              const keyword = (this.searchDataValue.value || '').toString().trim();
              this.performSearch(keyword);
            } else {
              this.loadUserGroups();
            }
          }

          this.commonService.spinnerHide();
        },
        error: (err) => {
          console.error('Delete failed', err);
          this.commonService.toastError('Failed to delete user group');
          this.commonService.spinnerHide();
        },
      });
  }

  

  getStatusLabel(status: boolean | string): string {
    return status === false || status === '0' ? 'Inactive' : 'Active';
  }

  getStatusClass(status: boolean | string): string {
    return status === false || status === '0'
      ? 'badge badge-danger'
      : 'badge badge-success';
  }

  private updateSerialNumbers(): void {
    this.serialNumberArray = Array.from(
      { length: this.tableData.length },
      (_, i) => this.paginationSkip + i + 1
    );
  }

  private updatePagination(): void {
    this.isUpdatingPagination = true;
    this.paginationService.calculatePageSize.next({
      totalData: this.totalData,
      pageSize: this.pageSize,
      tableData: this.tableData,
      serialNumberArray: this.serialNumberArray,
    });
    setTimeout(() => (this.isUpdatingPagination = false), 100);
  }

  private finishLoading(): void {
    this.isLoading = false;
    if (!this.isReloadingAfterDelete) {
      this.commonService.spinnerHide();
    } else {
      this.isReloadingAfterDelete = false;
    }
  }
}