import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import {
  catchError,
  debounceTime,
  EMPTY,
  finalize,
  Subject,
  takeUntil,
} from 'rxjs';
import {
  CommonService,
  pageSelection,
  PaginationService,
  routes,
  sharedModule,
  SidebarService,
  tablePageSize,
} from 'src/app/core.index';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { RoleManagementService } from './role-management.service';
import { CommonModule } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { UntypedFormControl } from '@angular/forms';
import { ConfirmDialogComponent } from 'src/app/core/shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-role-management',
  imports: [sharedModule, CustomElementModule, CommonModule, RouterModule],
  templateUrl: './role-management.component.html',
  styleUrl: './role-management.component.scss',
})
export class RoleManagementComponent implements OnInit, OnDestroy {
  tableData: any[] = [];
  pageSize = 10;
  totalData = 0;
  isCollapsed = false;

  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue: UntypedFormControl;
  private destroy$;
  private lastPayload: string = '';

  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private roleManagementService: RoleManagementService,
    private commonService: CommonService,
    private dialog: MatDialog,
    private router: Router,
  ) {
    this.destroy$ = new Subject<void>();
    this.searchDataValue = new UntypedFormControl('');

    
    

    
    
    
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: this.searchDataValue.value.trim(),
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;
    const url = this.searchDataValue ? 'searchRoleByProduct' : 'permissions';

    const payload: any = this.searchDataValue
      ? {
          filters: [
            {
              filterDataType: '',
              filterValue: this.searchDataValue.value.trim(),
              filterColumn: 'any',
              filterOperator: 'equalto',
              filterCondition: 'and',
            },
          ],
          page,
          pageSize: pageOption.limit,
        }
      : {
          page,
          pageSize: pageOption.limit,
        };
    this.commonService.spinnerShow();
    this.roleManagementService
      .postMethod(url, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          this.commonService.spinnerHide();
          const responseList: any[] = res.dataList || [];
          this.totalData = res?.totalRecords || 0;
          this.tableData = responseList;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);

          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (error: any) => {
          this.commonService.spinnerHide();
          if (error.status === 404 && this.searchDataValue) {
            this.tableData = [];
            this.totalData = 0;

            this.pagination.calculatePageSize.next({
              totalData: 0,
              pageSize: this.pageSize,
              tableData: [],
              serialNumberArray: [],
            });
            this.commonService.toastInfo(error?.error?.msg);
          } else {
            this.commonService.toastError(
              error?.error?.msg || 'Something went wrong while fetching data',
            );
          }
        },
      });
  }

  public searchData(): void {
    
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  public clearSearch(): void {
    this.searchDataValue.setValue('');
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  public confirmDelete(id: number): void {
    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '420px',
      disableClose: true,
      data: {
        title: 'Delete Role',
        message: 'Are you sure you want to delete this role?',
        confirmButtonText: 'Yes Delete',
        cancelButtonText: 'Cancel',
        iconClass: 'ti ti-trash fs-24 text-danger',
      },
    });

    dialogRef
      .afterClosed()
      .pipe(takeUntil(this.destroy$))
      .subscribe((confirmed: boolean) => {
        if (confirmed) {
          this.delete(id);
        }
      });
  }

  private delete(id: number): void {
    this.commonService.spinnerShow();
    this.roleManagementService
      .deleteMethod(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((err) => {
          this.commonService.toastError(err?.error.error || '');
          return EMPTY;
        }),
      )
      .subscribe((response: any) => {
        if (response?.responseCode == 200) {
          if (
            this.totalData % this.pageSize === 1 &&
            this.totalData > this.pageSize
          ) {
            const previousPageSkip =
              (Math.ceil(this.totalData / this.pageSize) - 2) * this.pageSize;

            this.lastPayload = '';
            this.pagination.tablePageSize.next({
              skip: previousPageSkip,
              limit: this.pageSize,
              pageSize: this.pageSize,
            });
          } else {
            this.searchData();
          }
          this.commonService.toastSuccess(response.responseMessage);
        } else {
          this.commonService.toastError(response.responseMessage);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openAddEdit(item: any = null): void {
    this.router.navigate([routes.roleManagement + '/add-edit'], {
      state: {
        ...(item ? item : {}),
        isAddEdit: true,
      },
    });
  }
}
