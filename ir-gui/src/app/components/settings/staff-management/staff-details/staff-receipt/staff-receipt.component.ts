import { Component, Input } from '@angular/core';
import { StaffManagementService } from '../../staff-management.service';
import { catchError, debounceTime, Subject, takeUntil } from 'rxjs';
import {
  IPermission,
  pageSelection,
  tablePageSize,
} from 'src/app/core/models/models';
import { PaginationService } from 'src/app/core/service/pagination.service';
import { CommonService } from 'src/app/core/service/common.service';
import { ChildMenuEnum, MenuEnum } from 'src/app/core/enums/sidebar-menu.enum';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';
import { AddReceiptComponent } from '../../add-receipt/add-receipt.component';
import { CommonModule } from '@angular/common';

declare var bootstrap: any;

@Component({
  selector: 'app-staff-receipt',
  imports: [CommonModule, CustomPaginationModule, AddReceiptComponent],
  templateUrl: './staff-receipt.component.html',
  styleUrl: './staff-receipt.component.scss',
})
export class StaffReceiptComponent {
  @Input() selectedStaff: any = {};
  createReceiptAccess: boolean = false;
  staffreciptMappingList: any[] = [];
  pageSize = 10;
  totalData = 0;
  selectedCountry: any = null;
  serialNumberArray: number[] = [];
  isCollapsed = false;
  private destroy$ = new Subject<void>();
  private lastPayload: string = '';

  constructor(
    private pagination: PaginationService,
    private staffManagementService: StaffManagementService,
    private commonService: CommonService,
  ) {}

  ngOnInit(): void {
    this.getPermission();
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.pageSize,
        });

        
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getstaffreciptMappingList({
            skip: res.skip,
            limit: res.pageSize,
          });
          this.pageSize = res.pageSize;
        }
      });
  }

  getPermission(): void {
    const loggedInUserId = this.commonService.userId;
    if (loggedInUserId == this.selectedStaff.id) {
      this.createReceiptAccess = this.commonService.hasPermission(
        [
          MenuEnum.SETTING,
          ChildMenuEnum.MY_PROFILE,
          'staff_receipt',
          'staff_receipt_mgmt',
        ],
        true,
      ).view;
    } else {
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
  }
  getstaffreciptMappingList(pageOption: pageSelection): void {
    this.staffManagementService
      .getStaffReceiptDataByStaffId(this.selectedStaff.id)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.staffreciptMappingList = [];
          this.totalData = 0;
          this.pagination.calculatePageSize.next({
            totalData: 0,
            pageSize: this.pageSize,
            tableData: [],
            serialNumberArray: [],
          });
          return error;
        }),
      )
      .subscribe((res: any) => {
        this.commonService.spinnerHide();
        const responseList: any[] = res.dataList || [];
        this.totalData = res.dataList?.length || 0;
        this.staffreciptMappingList = responseList;
        this.serialNumberArray = this.staffreciptMappingList.map(
          (_, i) => i + 1,
        );
        this.pagination.calculatePageSize.next({
          totalData: this.totalData,
          pageSize: this.pageSize,
          tableData: this.staffreciptMappingList,
          serialNumberArray: this.serialNumberArray,
        });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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
}
