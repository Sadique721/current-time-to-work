import { Component } from '@angular/core';
import { catchError, debounceTime, finalize, Subject, takeUntil } from 'rxjs';
import {
  IPermission,
  pageSelection,
  tablePageSize,
} from 'src/app/core/models/models';
import { CommonService } from 'src/app/core/service/common.service';
import { PaginationService } from 'src/app/core/service/pagination.service';
import { SidebarService } from 'src/app/core/service/sidebar.service';
import { StaffManagementService } from './staff-management.service';
import { FormControl } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';
import { routes, sharedModule } from 'src/app/core.index';
import { AddReceiptComponent } from './add-receipt/add-receipt.component';
import { SelectModule } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { Router, RouterModule } from '@angular/router';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
declare var bootstrap: any;

@Component({
  selector: 'app-staff-management',
  imports: [
    CommonModule,
    CustomPaginationModule,
    sharedModule,
    SelectModule,
    AddReceiptComponent,
    ChangePasswordComponent,
    FloatLabel,
    RouterModule,
  ],
  templateUrl: './staff-management.component.html',
  styleUrl: './staff-management.component.scss',
})
export class StaffManagementComponent {
  routes = routes;
  permission: IPermission;
  staffChangePassword: boolean = false;
  createReceiptAccess: boolean = false;
  tableData: any[] = [];
  pageSize = 10;
  totalData = 0;
  serialNumberArray: number[] = [];
  paginationSkip = 0;
  searchDataValue = new FormControl('');
  isCollapsed = false;
  destroy$ = new Subject<void>();
  lastPayload: string = '';

  searchOptionSelect = [
    { label: 'First Name', value: 'firstname' },
    { label: 'Last Name', value: 'lastname' },
    { label: 'Username', value: 'username' },
    { label: 'Email', value: 'email' },
  ];

  searchDetailControl = new FormControl('');
  prefixControl = new FormControl('');
  searchOptionControl = new FormControl(null);
  searchData: any = [];
  selectedStaff: any = null;
  
  constructor(
    private pagination: PaginationService,
    private sidebar: SidebarService,
    private commonService: CommonService,
    private staffManagementService: StaffManagementService,
    private router: Router,
  ) {
    this.permission = this.commonService.hasPermission([
      MenuEnum.SETTING,
      ChildMenuEnum.STAFF,
    ]);

    this.staffChangePassword = this.commonService.hasPermission(
      [
        MenuEnum.SETTING,
        ChildMenuEnum.STAFF,
        'staff_details',
        'staff_change_password',
      ],
      true,
    ).view;

    this.createReceiptAccess = this.commonService.hasPermission(
      [
        MenuEnum.SETTING,
        ChildMenuEnum.STAFF,
        'staff_details',
        'staff_details_receipt',
        'staff_create_receipt',
      ],
      true,
    ).view;
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
          search: this.searchDetailControl.value,
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getTableData({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }

        this.searchData = {
          filters: [
            {
              filterColumn: 'any',
              filterCondition: 'and',
              filterDataType: '',
              filterOperator: 'equalto',
              filterValue: '',
              port: '',
              salesRepresentative: '',
              serviceArea: '',
              serviceNetwork: '',
              slot: '',
            },
          ],
          page: res.skip / res.limit + 1,
          pageSize: res.pageSize,
        };
      });
  }

  private getTableData(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;
    const payload = { page, pageSize: pageOption.limit };
    this.commonService.spinnerShow();
    this.staffManagementService
      .getAllStaffList(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
        catchError((error) => {
          this.commonService.toastError(
            error?.error.errorMessage ||
              'Something went wrong while fetching data',
          );
          return error;
        }),
      )
      .subscribe((res: any) => {
        this.commonService.spinnerHide();
        const responseList: any[] = res.staffUserlist || [];
        this.totalData = res.pageDetails?.totalRecords || 0;
        this.tableData = responseList;
        this.serialNumberArray = this.tableData.map((_, i) => i + 1);
        this.pagination.calculatePageSize.next({
          totalData: this.totalData,
          pageSize: this.pageSize,
          tableData: this.tableData,
          serialNumberArray: this.serialNumberArray,
        });
      });
  }

  
  getSearchPlaceholder(): string {
    if (!this.searchOptionControl.value) {
      return 'Select a search option first';
    }
    
    const selectedOption = this.searchOptionSelect.find(
      option => option.value === this.searchOptionControl.value
    );
    
    return selectedOption ? `Search by ${selectedOption.label}` : 'Search';
  }

  searchStaffData() {
    
    if (!this.searchOptionControl.value) {
      this.commonService.toastInfo('Please select a search option');
      return;
    }

    
    if (!this.searchDetailControl.value?.trim()) {
      this.commonService.toastInfo('Please enter a search value');
      return;
    }

    
    this.searchData.filters[0].filterColumn = this.searchOptionControl.value;
    this.searchData.filters[0].filterValue = this.searchDetailControl.value?.trim();

    this.commonService.spinnerShow();
    this.staffManagementService
      .staffSearch(this.searchData)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Something went wrong while fetching data',
          );
          return error;
        }),
      )
      .subscribe((response: any) => {
        if (response?.responseCode == 200) {
          this.tableData = response.dataList;
          this.totalData = response.totalRecords;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        } else if (response?.responseCode == 404) {
          this.commonService.toastInfo(response?.responseMessage);
          this.tableData = [];
          this.totalData = 0;

          this.pagination.calculatePageSize.next({
            totalData: 0,
            pageSize: this.pageSize,
            tableData: [],
            serialNumberArray: [],
          });
        } else {
          this.commonService.toastInfo(response?.responseMessage);
        }
      });
  }

  searchStaffByName() {
    this.searchData.filters[0].filterValue =
      this.searchDetailControl.value?.trim();
    this.staffManagementService
      .staffSearch(this.searchData)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Something went wrong while fetching data',
          );
          return error;
        }),
      )
      .subscribe((response: any) => {
        if (response?.responseCode == 200) {
          this.tableData = response.dataList;
          this.totalData = response.totalRecords;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: this.pageSize,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        } else if (response?.responseCode == 404) {
          this.commonService.toastInfo(response?.responseMessage);
          this.tableData = [];
          this.totalData = 0;

          this.pagination.calculatePageSize.next({
            totalData: 0,
            pageSize: this.pageSize,
            tableData: [],
            serialNumberArray: [],
          });
        } else {
          this.commonService.toastInfo(response?.responseMessage);
        }
      });
  }

  searchReceiptName() {
    const receNo = this.searchDetailControl.value;
    const prefix = this.prefixControl.value?.trim() || '';
    const data = {};
    this.staffManagementService
      .staffReceiptSearch(receNo, prefix, data)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
        catchError((error) => {
          this.commonService.toastError(error?.error?.ERROR);
          return error;
        }),
      )
      .subscribe({
        next: (response: any) => {
          this.tableData = response.dataList;
          this.totalData = response.totalRecords || 0;
          this.searchData = [];
        },
        error: (error: any) => {
          if (error.status === 404) {
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
              error?.error?.ERROR || 'Something went wrong while fetching data',
            );
          }
        },
      });
  }

  public clearSearch(): void {
    this.searchDetailControl.setValue('');
    this.prefixControl.setValue('');
    this.searchOptionControl.setValue(null);
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

  addReceipt(staff: any = null): void {
    this.selectedStaff = staff ?? {};

    const modalElement = document.getElementById('add-receipt');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onCloseAddReceipt(isReload: boolean): void {
    this.selectedStaff = null;
    isReload ? this.searchStaffData() : null;
  }

  openChangePasswordDialog(staff: any = null): void {
    this.selectedStaff = staff ?? {};

    const modalElement = document.getElementById('change-password');
    if (modalElement) {
      const modalInstance =
        bootstrap.Modal.getInstance(modalElement) ||
        new bootstrap.Modal(modalElement);
      modalInstance.show();
    }
  }

  onCloseChangePasswordDialog(isReload: boolean): void {
    this.selectedStaff = null;
    isReload ? this.searchStaffData() : null;
  }

  openAddEditStaff(item: any = null): void {
    this.router.navigate([routes.staffManagement + '/add-edit'], {
      state: {
        ...(item ? item : {}),
        isAddEdit: true,
      },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}