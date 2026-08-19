import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import {
  FormsModule,
  ReactiveFormsModule,
  UntypedFormControl,
  UntypedFormArray,
  UntypedFormGroup,
  FormBuilder,
  Validators,
} from '@angular/forms';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { Subject, takeUntil, finalize, debounceTime } from 'rxjs';
import { Router } from '@angular/router';
import {
  CommonService,
  PaginationService,
  tablePageSize,
} from 'src/app/core.index';
import { MvnoManagementService } from '../../mvno-management.service';
import dayjs from 'src/app/core/helpers/dayjs.config';
import saveAs from 'file-saver';
@Component({
  selector: 'app-mvno-invoice',
  imports: [
    CommonModule,
    FormsModule,
    CustomElementModule,
    ReactiveFormsModule,
  ],
  templateUrl: './mvno-invoice.component.html',
  styleUrl: './mvno-invoice.component.scss',
})
export class MvnoInvoiceComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  @Input() selectedMvno: any;
  @Input() mvnoId: string = '';
  fromDate = new UntypedFormControl(null);
  toDate = new UntypedFormControl(null);
  invoiceList: any[] = [];
  mvnoData: any = {};
  viewcustomerInvoiceData: any[] = [];
  displayInvoiceListDialog: boolean = false;
  customerInvoiceId: any;
  customerName: string = '';
  currentPageinvoiceListdata: number = 1;
  invoiceListdataitemsPerPage: number = 5;
  invoiceListDatatotalRecords: number = 0;
  showItemPerPage: number = 5;
  isInvoiceDetailsModel: boolean = false;
  viewInvoiceData: any = {};
  selectedChargeType = new UntypedFormControl('');
  customerInvoiceTotalAmount = new UntypedFormControl(0);
  commission = new UntypedFormControl(0);
  commissionAmount = new UntypedFormControl(0);
  paginationSkip = 0;
  mvnoChagesListFormmArray = new UntypedFormArray([]);
  searchInvoiceChargeForm!: UntypedFormGroup;
  totalAmount = 0;
  chargeId: any;
  debitDocDetailIdList: any[] = [];
  isChargeDropdownDisable: boolean = false;
  currency = 'USD';
  chargeTypeList: any[] = [];
  masterChargeTypeList: any[] = [];
  chargeTypes: any[] = [];
  masterChargeTypes: any[] = [];
  showRemarkModal: boolean = false;
  invoiceID: any = null;
  invoiceCancelRemarksType: string = '';
  invoiceCancelRemarks: string = '';
  selectedInvoiceForAction: any = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private commonService: CommonService,
    private mvnoManagementService: MvnoManagementService,
    private pagination: PaginationService,
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
      });
    this.initializeForms();
    this.setupFormSubscriptions();

    if (this.mvnoId) {
      this.getMvnoById(this.mvnoId);
    }

    this.getChargeType();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    this.searchInvoiceChargeForm = this.fb.group({
      isInvoiceVoid: [false],
      fromDate: [''],
      toDate: [''],
    });
  }

  private setupFormSubscriptions(): void {
    this.commission.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.calculateCommission();
      });

    this.customerInvoiceTotalAmount.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.calculateCommission();
      });
  }

  getMvnoById(id: string): void {
    if (!id) return;

    const url = `/mvno/${id}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.mvnoData = response.data;
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching MVNO details',
          );
        },
      });
  }

  getChargeType(): void {
    const url = '/commonList/generic/chargetype';
    this.mvnoManagementService
      .getMethod(url)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.chargeTypeList = response.dataList || [];
          this.masterChargeTypeList = [...this.chargeTypeList];
        },
        error: (error: any) => {
          this.commonService.toastError('Failed to load charge types');
        },
      });
  }

  getCustomerInvoices(): void {
    this.chargeTypes = [];
    this.masterChargeTypes = [];
    this.customerInvoiceTotalAmount.setValue(0);

    const request = { ...this.searchInvoiceChargeForm.value };
    request.fromDate = dayjs(request.fromDate).utc().startOf('day').toDate();
    request.toDate = dayjs(request.toDate).utc().endOf('day').toDate();

    const url = `invoiceByMvnoId/${this.mvnoId}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .postMethod(url, request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          if (response.debitDocDetails?.length > 0) {
            this.chargeTypes = response.debitDocDetails.filter(
              (charge: any) =>
                charge.chargeType != null && charge.totalAmount > 0,
            );
            this.selectedChargeType.enable();

            const filterchargeTypes = this.chargeTypes.map(
              (charge) => charge.chargeType,
            );
            this.chargeTypeList = this.masterChargeTypeList.filter(
              (chargeType) =>
                filterchargeTypes.includes(chargeType.text) ||
                filterchargeTypes.includes(chargeType.value),
            );

            this.masterChargeTypes = [...this.chargeTypes];
          } else {
            this.commonService.toastInfo('No invoice found for given date');
            this.selectedChargeType.disable();
            this.chargeTypeList = [...this.masterChargeTypeList];
          }
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching invoices',
          );
        },
      });
  }

  onDateChange(): void {
    if (this.fromDate.value && this.toDate.value) {
      this.searchInvoiceChargeForm.patchValue({
        fromDate: this.fromDate.value,
        toDate: this.toDate.value,
      });
    }
  }

  searchInvoices(): void {
    if (!this.fromDate.value || !this.toDate.value) {
      this.commonService.toastWarn('Please select both from and to dates');
      return;
    }

    this.searchInvoiceChargeForm.patchValue({
      fromDate: this.fromDate.value,
      toDate: this.toDate.value,
    });

    this.getCustomerInvoices();
  }

  clearSearch(): void {
    this.fromDate.setValue(null);
    this.toDate.setValue(null);
    this.invoiceList = [];
    this.searchInvoiceChargeForm.reset({
      isInvoiceVoid: false,
      fromDate: '',
      toDate: '',
    });
    this.chargeTypeList = [...this.masterChargeTypeList];
    this.selectedChargeType.enable();
  }

  calculateCommission(): void {
    const invoiceAmount = this.customerInvoiceTotalAmount.value || 0;
    const commissionPercent = this.commission.value || 0;
    const calculatedAmount = Number(
      ((invoiceAmount * commissionPercent) / 100).toFixed(2),
    );

    this.commissionAmount.setValue(calculatedAmount);
  }

  onChargeTypeChange(): void {
    const selectedCharge = this.chargeTypes.find(
      (charge) => charge.chargeType === this.selectedChargeType.value,
    );

    if (selectedCharge) {
      this.customerInvoiceTotalAmount.setValue(selectedCharge.totalAmount);
      this.chargeId = selectedCharge.id;
    }
  }
  onAddCharge(): void {
    if (
      !this.selectedChargeType.value ||
      !this.customerInvoiceTotalAmount.value
    ) {
      this.commonService.toastWarn('Please fill all required fields');
      return;
    }

    if (this.commissionAmount.value <= 0) {
      this.commonService.toastWarn('Commission amount must be greater than 0');
      return;
    }

    const chargeGroup = this.fb.group({
      chargeTypeText: [
        {
          value: this.getSelectedChargeTypeText(),
          disabled: true,
        },
      ],
      chargeTypeValue: [this.selectedChargeType.value],
      invoiceAmount: [
        {
          value: this.customerInvoiceTotalAmount.value,
          disabled: true,
        },
      ],
      commission: [
        {
          value: this.commission.value,
          disabled: true,
        },
      ],
      commissionAmount: [
        {
          value: this.commissionAmount.value,
          disabled: true,
        },
      ],
    });

    this.mvnoChagesListFormmArray.push(chargeGroup);
    this.calculateTotalAmount();
    this.clearChargeInputs();
    const selectedCharge = this.chargeTypes.find(
      (charge) => charge.chargeType === this.selectedChargeType.value,
    );
    if (selectedCharge) {
      this.debitDocDetailIdList.push(selectedCharge.id);
    }

    this.commonService.toastSuccess('Charge added successfully');
  }

  deleteAddedCharge(index: number, row: any): void {
    const chargeValue = row.get('chargeTypeValue')?.value;
    const selectedCharge = this.chargeTypes.find(
      (charge) => charge.chargeType === chargeValue,
    );
    if (selectedCharge) {
      const docIndex = this.debitDocDetailIdList.indexOf(selectedCharge.id);
      if (docIndex > -1) {
        this.debitDocDetailIdList.splice(docIndex, 1);
      }
    }

    this.mvnoChagesListFormmArray.removeAt(index);
    this.calculateTotalAmount();
  }
  generateInvoice(): void {
    if (this.mvnoChagesListFormmArray.length === 0) {
      this.commonService.toastWarn('No charges added to generate invoice');
      return;
    }

    const dateWithTime = dayjs().format('DD-MM-YYYY HH:mm');
    const date = dayjs().format('YYYY-MM-DD');
    const formReq = { ...this.searchInvoiceChargeForm.value };

    const request = {
      custid: this.mvnoData.custInvoiceRefId,
      billableCustomerId: null,
      paymentOwnerId: 1,
      isMvnoCharge: true,
      debitDocDetailIds: this.debitDocDetailIdList,
      ispFromDate: dayjs(formReq.fromDate).utc().startOf('day').toDate(),
      ispToDate: dayjs(formReq.toDate).utc().endOf('day').toDate(),
      custChargeDetailsPojoList: [
        {
          type: 'one-time',
          chargeid: this.chargeId,
          validity: 30,
          price: this.totalAmount,
          actualprice: this.customerInvoiceTotalAmount.value,
          charge_date: date,
          unitsOfValidity: 'Days',
          billingCycle: 1,
          paymentOwnerId: 1,
          discount: null,
          staticIPAdrress: null,
          expiry: date,
          expiryDate: dateWithTime,
        },
      ],
    };

    const url = 'createCustChargeOverride';
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .posttMethod(url, request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.commonService.toastSuccess(
            response.message || 'Invoice generated successfully',
          );
          this.resetAfterGeneration();
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error generating invoice',
          );
        },
      });
  }

  private getSelectedChargeTypeText(): string {
    const selected = this.chargeTypeList.find(
      (item) => item.value === this.selectedChargeType.value,
    );
    return selected ? selected.text : '';
  }

  private clearChargeInputs(): void {
    this.selectedChargeType.setValue('');
    this.customerInvoiceTotalAmount.setValue(0);
    this.commission.setValue(0);
    this.commissionAmount.setValue(0);
  }

  private resetAfterGeneration(): void {
    this.mvnoChagesListFormmArray = this.fb.array([]);
    this.debitDocDetailIdList = [];
    this.totalAmount = 0;
    this.clearChargeInputs();
    this.clearSearch();
  }

  private calculateTotalAmount(): void {
    this.totalAmount = this.mvnoChagesListFormmArray.controls.reduce(
      (sum, control) => {
        const amount = control.get('commissionAmount')?.value || 0;
        return sum + parseFloat(amount);
      },
      0,
    );
  }
  openInvoiceDetail(invoice: any): void {
    this.isInvoiceDetailsModel = true;
    this.getInvoiceDetails(invoice.id, invoice.custid);
  }

  getInvoiceDetails(invoiceId: any, custId: any): void {
    if (!invoiceId || !custId) {
      this.commonService.toastWarn('Invoice ID and Customer ID are required');
      return;
    }

    const url = `/invoiceDetails/${invoiceId}/${custId}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.viewInvoiceData = response.invoiceDetails || {};
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching invoice details',
          );
          this.invoiceModelClose(); 
        },
      });
  }

  invoiceModelClose(): void {
    this.isInvoiceDetailsModel = false;
    this.viewInvoiceData = {};
  }
  openPaymentDetail(invoiceId: any): void {}

  downloadPDF(invoiceId: any, customerName: string): void {
    if (!invoiceId) {
      this.commonService.toastWarn('Invoice ID is required');
      return;
    }

    const downloadUrl = `/invoicePdf/download/${invoiceId}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .downloadPDFInvoice(downloadUrl)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          const file = new Blob([response], { type: 'application/pdf' });
          const fileName = `${customerName}_${invoiceId}.pdf`;
          saveAs(file, fileName);
          this.commonService.toastSuccess('PDF downloaded successfully');
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error downloading PDF',
          );
        },
      });
  }

  generatePDF(invoiceId: any): void {
    if (!invoiceId) {
      this.commonService.toastWarn('Invoice ID is required');
      return;
    }

    const url = `/generatePdfByInvoiceId/${invoiceId}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .generateMethodInvoice(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          if (response.responseCode === 200) {
            this.commonService.toastSuccess(
              response.responseMessage || 'PDF generated successfully',
            );
          } else if (response.responseCode === 417) {
            this.getCustomerInvoices(); 
            this.commonService.toastInfo(
              response.responseMessage || 'PDF generation in progress',
            );
          } else {
            this.commonService.toastInfo(
              response.responseMessage || 'PDF generation status updated',
            );
          }
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error generating PDF',
          );
        },
      });
  }

  canCancelRegenerate(invoice: any): boolean {
    return (
      invoice.billrunstatus !== 'Cancelled' &&
      (!invoice.paymentStatus ||
        invoice.paymentStatus.toLowerCase() === 'unpaid' ||
        invoice.paymentStatus.toLowerCase() === 'partial pending')
    );
  }

  invoiceRemarks(invoice: any, type: string): void {
    this.invoiceID = invoice.id;
    this.invoiceCancelRemarksType = type;
    this.selectedInvoiceForAction = invoice;
    this.showRemarkModal = true;
    this.invoiceCancelRemarks = '';
  }

  cancelRegenerate(invoice: any): void {
    this.invoiceRemarks(invoice, 'cancel_regenerate');
  }

  cancelRegenerateInvoice(): void {
    if (!this.invoiceCancelRemarks.trim()) {
      this.commonService.toastWarn('Please enter remarks');
      return;
    }

    if (!this.invoiceID) {
      this.commonService.toastWarn('Invoice ID is required');
      return;
    }

    const data = {};
    const url = `/cancelAndRegenerate/${this.invoiceID}?isCaf=false&invoiceCancelRemarks=${encodeURIComponent(this.invoiceCancelRemarks)}`;

    this.commonService.spinnerShow();

    this.mvnoManagementService
      .postMethod(url, data)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.closeInvoiceCancelRemark();
          this.getCustomerInvoices(); 

          if (response.responseCode === 417) {
            this.commonService.toastInfo(
              response.responseMessage || 'Invoice cancellation in progress',
            );
          } else {
            this.commonService.toastSuccess(
              response.message ||
                response.responseMessage ||
                'Invoice cancelled and regenerated successfully',
            );
          }
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error cancelling and regenerating invoice',
          );
        },
      });
  }

  closeInvoiceCancelRemark(): void {
    this.showRemarkModal = false;
    this.invoiceID = null;
    this.invoiceCancelRemarksType = '';
    this.invoiceCancelRemarks = '';
    this.selectedInvoiceForAction = null;
  }

  resendPayload(invoice: any): void {
    if (!invoice?.id) {
      this.commonService.toastWarn('Invoice ID is required');
      return;
    }

    const url = `/invoice/reSendPayload/${invoice.id}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.commonService.toastSuccess(
            response.msg || response.message || 'Payload resent successfully',
          );
          this.getCustomerInvoices(); 
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error resending payload',
          );
        },
      });
  }
  get isSearchDisabled(): boolean {
    return !this.fromDate.value || !this.toDate.value;
  }

  get isAddChargeDisabled(): boolean {
    return (
      !this.selectedChargeType.value ||
      !this.customerInvoiceTotalAmount.value ||
      this.commissionAmount.value <= 0
    );
  }

  get hasCharges(): boolean {
    return this.mvnoChagesListFormmArray.length > 0;
  }
  getCustomerInvoiceList(invoiceId: any, customerName: string): void {
    this.customerInvoiceId = invoiceId;
    this.customerName = customerName;

    const request = {
      page: this.currentPageinvoiceListdata,
      pageSize: this.invoiceListdataitemsPerPage,
    };

        
    const url = `/mvnoInvoice/list/${this.customerInvoiceId}`;
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .postMethod(url, request)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.viewcustomerInvoiceData =
            response.mvnoDebitDocDetailsPojos || [];

          if (this.viewcustomerInvoiceData.length !== 0) {
            this.displayInvoiceListDialog = true;
            this.invoiceListDatatotalRecords =
              response.pageDetails?.totalRecords || 0;
          } else {
            this.commonService.toastInfo('No Customers Found!');
          }

                  },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching customer invoice list',
          );
        },
      });
  }
  closeCustomerInvoiceList(): void {
    this.displayInvoiceListDialog = false;
    this.currentPageinvoiceListdata = 1;
    this.invoiceListdataitemsPerPage = 5;
    this.viewcustomerInvoiceData = [];
    this.customerInvoiceId = null;
    this.customerName = '';
  }

  TotalItemPerPageInvoice(event: any): void {
    this.showItemPerPage = Number(event.value || event);
    this.currentPageinvoiceListdata = 1;
    this.invoiceListdataitemsPerPage = this.showItemPerPage;

    if (this.customerInvoiceId && this.customerName) {
      this.getCustomerInvoiceList(this.customerInvoiceId, this.customerName);
    }
  }

  onInvoiceListPageChange(pageNumber: number): void {
    this.currentPageinvoiceListdata = pageNumber;

    if (this.customerInvoiceId && this.customerName) {
      this.getCustomerInvoiceList(this.customerInvoiceId, this.customerName);
    }
  }
}
