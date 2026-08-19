import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, takeUntil, finalize, debounceTime } from 'rxjs';
import {
  CommonService,
  PaginationService,
  sharedModule,
  tablePageSize,
} from 'src/app/core.index';
import { MvnoManagementService } from '../../mvno-management.service';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { MvnoPaymentAddComponent } from './mvno-payment-add/mvno-payment-add.component';

@Component({
  selector: 'app-mvno-payment',
  imports: [
    CommonModule,
    CustomElementModule,
    sharedModule,
    CustomPaginationModule,
    MvnoPaymentAddComponent,
  ],
  templateUrl: './mvno-payment.component.html',
  styleUrl: './mvno-payment.component.scss',
})
export class MvnoPaymentComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  @Input() selectedMvno: any;
  @Input() mvnoId: string = '';
  @Input() mvnoData: any;

  viewcustomerPaymentData: any[] = [];
  customerLedgerDetailData: any = {};
  recordPaymentAccess: boolean = true;
  isDisable: boolean = false;
  showRecordPaymentModal: boolean = false;
  selectedPaymentData: any = {};
  chequeDetail: any = {};
  showChequeDetails: boolean = false;
  displayInvoiceDetails: boolean = false;
  paymentId = new Subject<any>();
  mvnoInvoiceList: any[] = [];
  mvnoCurrentPageInvoiceListdata: number = 1;
  mvnoInvoiceListdataitemsPerPage: number = 10;
  mvnoInvoiceListdatatotalRecords: number = 0;
  chargeId: any;
  currentPagecustomerPaymentdata: number = 1;
  customerPaymentdataitemsPerPage: number = 10;
  customerPaymentdatatotalRecords: number = 0;
  paymentShowItemPerPage: number = 10;
  pageITEM: number = 10;
  newFirst: number = 0;
  paginationSkip = 0;

  searchData: any = {
    filters: [{ filterValue: '', filterColumn: '' }],
    page: '',
    pageSize: '',
  };

  constructor(
    private pagination: PaginationService,
    private commonService: CommonService,
    private mvnoManagementService: MvnoManagementService,
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
      });
    if (this.mvnoId || this.mvnoData?.custInvoiceRefId) {
      this.openCustomersPaymentData(null);
      this.getCustomersDetail();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  openCustomersPaymentData(size: any): void {
    if (
      this.customerLedgerDetailData?.parentCustomerId === 'null' ||
      this.customerLedgerDetailData?.invoiceType === 'Group'
    ) {
      this.isDisable = true;
    }
    if (size) {
      this.customerPaymentdataitemsPerPage = size;
    } else {
      if (this.paymentShowItemPerPage === 1) {
        this.customerPaymentdataitemsPerPage = this.pageITEM;
      } else {
        this.customerPaymentdataitemsPerPage = this.paymentShowItemPerPage;
      }
    }
    const custId = this.mvnoData?.custInvoiceRefId || this.mvnoId;
    const url = `/paymentHistory/${custId}`;

    this.commonService.spinnerShow();
    this.mvnoManagementService
      .paymenthistoryMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.viewcustomerPaymentData = response.dataList || [];
          this.customerPaymentdatatotalRecords = response.totalRecords || 0;
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching payment data',
          );
        },
      });
  }

  getCustomersDetail(): void {
    const custId = this.mvnoData?.custInvoiceRefId || this.mvnoId;
    const url = `/customers/${custId}`;

    this.mvnoManagementService
      .getCustomerspaymentDetails(url)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.customerLedgerDetailData = response.customers || {};
        },
        error: (error: any) => {
          this.commonService.toastError('Error fetching customer details');
        },
      });
  }

  recordPaymentClicked(): void {
    this.showRecordPaymentModal = true;
  }

  closeRecordPaymentModal(): void {
    this.showRecordPaymentModal = false;
  }

  onPaymentAdded(): void {
    this.showRecordPaymentModal = false;
    this.refreshPaymentData();
  }

  openPaymentModal(id: any): void {
    if (this.searchData.filters) {
      this.searchData.filters[0].filterValue = '';
      this.searchData.filters[0].filterColumn = '';
      this.searchData.page = '';
      this.searchData.pageSize = '';
    }

    const url = `/getChequeDetail/${id}`;
    this.mvnoManagementService
      .postMethod(url, this.searchData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.chequeDetail = response.dataList || {};
          this.showChequeDetails = true;
        },
        error: (error: any) => {
          this.commonService.toastError('Error fetching cheque details');
        },
      });
  }

  pageChangedcustomerPaymentList(pageNumber: number): void {
    this.currentPagecustomerPaymentdata = pageNumber;
    this.openCustomersPaymentData('');
  }

  TotalPaymentItemPerPage(event: any): void {
    this.paymentShowItemPerPage = Number(event.value || event);
    if (this.currentPagecustomerPaymentdata > 1) {
      this.currentPagecustomerPaymentdata = 1;
    }
    this.openCustomersPaymentData(this.paymentShowItemPerPage);
  }

  openPaymentInvoiceModal(id: any): void {
    this.displayInvoiceDetails = true;
    this.paymentId.next({ paymentId: id });
  }

  getMvnoInvoiceList(): void {
    const custId = this.mvnoData?.custInvoiceRefId;
    const url = `/invoice/search?billrunid=&docnumber=&customerid=${custId}&billfromdate=&billtodate=&custmobile=&isInvoiceVoid=true`;

    const request = {
      page: this.mvnoCurrentPageInvoiceListdata,
      pageSize: this.mvnoInvoiceListdataitemsPerPage,
    };

    this.commonService.spinnerShow();
    this.mvnoManagementService
      .postMethod(url, request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.mvnoInvoiceList = response.invoicesearchlist || [];
          this.mvnoInvoiceListdatatotalRecords =
            response.pageDetails?.totalRecords || 0;
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching invoice list',
          );
        },
      });
  }

  getChargeList(): void {
    const url = '/charge/0/equal?isDeleted=true';

    this.mvnoManagementService
      .getMethod(url)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          if (response.charges?.length > 0) {
            this.chargeId = response.charges[0].id;
          }
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching charge list',
          );
        },
      });
  }

  clearSearchForm(): void {
    this.searchData = {
      filters: [{ filterValue: '', filterColumn: '' }],
      page: '',
      pageSize: '',
    };
  }

  downloadInvoice(id: any, custId: any, filename: string): void {
  }

  statusApproved(data: any): void {
    if (data.status !== 'pending') {
      this.commonService.toastWarn('Only pending payments can be approved');
      return;
    }

    const url = `/payment/approve/${data.id}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .postMethod(url, {})
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.commonService.toastSuccess(
            response.message || 'Payment approved successfully',
          );
          this.refreshPaymentData();
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error approving payment',
          );
        },
      });
  }

  refreshPaymentData(): void {
    this.openCustomersPaymentData(null);
  }
}