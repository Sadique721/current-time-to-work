import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import {
  FormBuilder,
  ReactiveFormsModule,
  UntypedFormGroup,
  Validators,
} from '@angular/forms';
import { Subject, takeUntil, finalize } from 'rxjs';
import { sharedModule } from 'src/app/core.index';
import { CommonService } from 'src/app/core/service/common.service';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { MvnoManagementService } from '../../../mvno-management.service';

@Component({
  selector: 'app-mvno-payment-add',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CustomElementModule,
    sharedModule,
  ],
  templateUrl: './mvno-payment-add.component.html',
  styleUrl: './mvno-payment-add.component.scss',
})
export class MvnoPaymentAddComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  @Input() mvnoId: string = '';
  @Input() mvnoData: any;
  @Input() showModal: boolean = false;
  @Output() close = new EventEmitter<void>();
  @Output() paymentAdded = new EventEmitter<void>();
  viewcustomerPaymentData: any[] = [];
  customerLedgerDetailData: any = {};
  newFirst: number = 0;

  Amount: number = 0;
  tdsPercent: number = 10; 
  abbsPercent: number = 5; 
  isTdsFlag: boolean = false;
  isAbbsFlag: boolean = false;
  
  paymentFormGroup!: UntypedFormGroup;
  submitted: boolean = false;
  chequeDateName: string = 'Cheque Date';
  chequeDetail: any = {};
  showChequeDetails: boolean = false;
  
  invoiceList: any[] = [];
  paymentMode: any[] = [];
  onlineSourceData: any[] = [];
  bankDataList: any[] = [];
  bankDestination: any[] = [];
  selectedInvoice: any[] = [];
  searchData: any = {
    filters: [{ filterValue: '', filterColumn: '' }],
    page: '',
    pageSize: '',
  };
  
  file: File | null = null;
  fileName: string | null = null;
  createPaymentData: any = {};

  
  displaySelectInvoiceDialog: boolean = false;
  isShowInvoiceList: boolean = false;
  masterSelected: boolean = false;
  destinationbank: boolean = false;

  constructor(
    private commonService: CommonService,
    private mvnoManagementService: MvnoManagementService,
    private fb: FormBuilder,
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.InvoiceListByCustomer();
    this.getPaymentMode();

    
    this.paymentFormGroup
      .get('paymode')
      ?.valueChanges.pipe(takeUntil(this.destroy$))
      .subscribe((value) => {
        
        if (value) {
          this.selPayModeRecord({ value });
        }
      });
  }

  async selPayModeRecord(event: any): Promise<void> {
    const payMode = event.value?.toLowerCase();
    const controls = this.paymentFormGroup.controls;

    if (!payMode) return;
    this.resetPayMode();

    if (payMode === 'pos') {
      controls['chequedate'].enable();
      controls['chequedate'].setValidators([Validators.required]);
      controls['referenceno'].clearValidators();
      controls['reciptNo'].enable();
      this.chequeDateName = 'Transaction date';
    } else if (payMode === 'online') {
      controls['chequedate'].enable();
      controls['chequedate'].setValidators([Validators.required]);
      controls['referenceno'].setValidators([Validators.required]);
      controls['reciptNo'].enable();
      this.chequeDateName = 'Transaction date';
    } else if (payMode === 'direct deposit') {
      controls['branch'].enable();
      controls['chequedate'].enable();
      controls['chequedate'].setValidators([Validators.required]);
      controls['destinationBank'].enable();
      controls['destinationBank'].setValidators([Validators.required]);
      controls['referenceno'].clearValidators();
      controls['reciptNo'].disable();
      this.chequeDateName = 'Transaction date';
    } else if (payMode === 'neft_rtgs') {
      controls['bankManagement'].enable();
      controls['bankManagement'].setValidators([Validators.required]);
      controls['destinationBank'].enable();
      controls['destinationBank'].setValidators([Validators.required]);
      controls['referenceno'].clearValidators();
      controls['reciptNo'].enable();
    } else if (payMode === 'cheque') {
      controls['chequedate'].enable();
      controls['chequedate'].setValidators([Validators.required]);
      controls['bankManagement'].enable();
      controls['bankManagement'].setValidators([Validators.required]);
      controls['chequeno'].enable();
      controls['chequeno'].setValidators([Validators.required]);
      controls['referenceno'].clearValidators();
      controls['reciptNo'].enable();
      controls['branch'].enable();
    } else if (payMode === 'vatreceiveable') {
      controls['invoiceId'].disable();
    } else if (payMode === 'efts') {
      controls['invoiceId'].disable();
      controls['onlinesource'].disable();
      controls['chequeno'].disable();
    }

    
    controls['chequedate'].updateValueAndValidity();
    controls['referenceno'].updateValueAndValidity();
    controls['reciptNo'].updateValueAndValidity();
    controls['branch'].updateValueAndValidity();
    controls['destinationBank'].updateValueAndValidity();
    controls['bankManagement'].updateValueAndValidity();
    controls['chequeno'].updateValueAndValidity();

    
    this.paymentFormGroup.patchValue(
      { onlinesource: '' },
      { emitEvent: false },
    );

    const onlineSourcePayModes = [
      'online',
      'cheque',
      'cash',
      'pos',
      'direct deposit',
      'neft_rtgs',
      'tds',
      'abbs',
      'otheradjustment',
      'barter',
      'vatreceiveable',
    ];
    if (onlineSourcePayModes.includes(payMode)) {
      const url = `/commonList/generic/${payMode}?from_cache=true`;
      this.mvnoManagementService.getMethod(url).subscribe({
        next: (response: any) => {
          this.onlineSourceData = response.dataList || [];
          if (this.onlineSourceData.length > 0) {
            controls['onlinesource'].setValidators([Validators.required]);
          } else {
            controls['onlinesource'].clearValidators();
          }
          controls['onlinesource'].updateValueAndValidity();
        },
        error: (error: any) => {
          this.onlineSourceData = [];
          this.commonService.toastError('Failed to load online source data');
        },
      });
    } else {
      this.onlineSourceData = [];
      controls['onlinesource'].clearValidators();
      controls['onlinesource'].updateValueAndValidity();
    }

    
    this.getBankDetail();

    
    const isAbbsTdsMode = this.checkPaymentMode(payMode);
    if (isAbbsTdsMode) {
      this.paymentFormGroup.patchValue(
        { tdsAmount: 0, abbsAmount: 0 },
        { emitEvent: false },
      );
      if (this.selectedInvoice.length > 0) {
        this.selectedInvoice.forEach((element) => {
          element.tds = 0;
          element.abbs = 0;
        });
      }
    }
  }

  getBankDetail(): void {
    const url = '/bankManagement/searchByStatus?banktype=other';
    this.commonService.spinnerShow();
    this.mvnoManagementService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          if (Array.isArray(response.dataList)) {
            this.bankDataList = response.dataList.map((item: any) => ({
              id: item.id,
              bankname: item.bankname,
              accountnum: item.accountnum,
              bankDisplayName: `${item.bankname} - ${item.accountnum}`,
            }));
          } else {
            this.bankDataList = [];
          }
        },
        error: (error: any) => {
          this.commonService.toastError('Failed to load bank details');
          this.bankDataList = [];
        },
      });
  }
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForm(): void {
    this.paymentFormGroup = this.fb.group({
      invoiceId: [0, Validators.required],
      paymode: ['', Validators.required],
      onlinesource: [''],
      amount: [0, [Validators.required, Validators.min(1)]],
      file: [''],
      chequeno: [''],
      chequedate: [null, [Validators.required]],
      bankManagement: [''],
      destinationBank: [''],
      branch: [''],
      referenceno: [''],
      reciptNo: [''],
      tdsAmount: [0],
      abbsAmount: [0],
      remark: [''],
    });
  }

  closeModal(): void {
    this.resetForm();
    this.close.emit();
  }

  private resetForm(): void {
    this.submitted = false;
    this.paymentFormGroup.reset();
    this.selectedInvoice = [];
    this.isShowInvoiceList = false;
    this.file = null;
    this.fileName = null;
    this.destinationbank = false;
  }
  InvoiceListByCustomer(): void {
    const custId = this.mvnoData?.custInvoiceRefId || this.mvnoId;
    const url = `/invoiceList/byCustomer/${custId}`;

    this.invoiceList = [];
    this.masterSelected = false;

    this.mvnoManagementService
      .getinvoicelist(url)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          if (response.invoiceList?.length > 0) {
            this.invoiceList = response.invoiceList;
          }

          this.invoiceList.forEach((item) => {
            item.tdsCheck = 0;
            item.abbsCheck = 0;
            item.tds = 0;
            item.abbs = 0;
            item.includeTds = false;
            item.includeAbbs = false;
            item.testamount = this.getPendingAmount(item);
            item.formGroup = this.fb.group({
              testamount: [item.testamount],
              tdsCheck: [item.tdsCheck],
              abbsCheck: [item.abbsCheck],
            });
            if (!item.testamount || item.testamount <= 0) {
              item.formGroup.get('tdsCheck')?.disable();
              item.formGroup.get('abbsCheck')?.disable();
            } else {
              item.formGroup.get('tdsCheck')?.enable();
              item.formGroup.get('abbsCheck')?.enable();
            }
            if (this.selectedInvoice.length > 0) {
              const isItemExist = this.selectedInvoice.some(
                (invoice) => invoice.id === item.id,
              );
              if (isItemExist) {
                item.isSelected = true;
              }
            }
          });
        },
        error: (error: any) => {
          this.commonService.toastError('Error fetching invoices');
        },
      });
  }
  onSelectedInvoice(
    value: any,
    invoice: any,
    includeTds: boolean,
    includeAbbs: boolean,
  ): void {
    invoice.testamount = value;
    invoice.formGroup.get('testamount')?.setValue(value);

    if (!value || value <= 0) {
      invoice.formGroup.get('tdsCheck')?.disable();
      invoice.formGroup.get('abbsCheck')?.disable();
    } else {
      invoice.formGroup.get('tdsCheck')?.enable();
      invoice.formGroup.get('abbsCheck')?.enable();
    }
    
    if (includeTds) {
      const tdsValue = ((value * this.tdsPercent) / 100).toFixed(2);
      invoice.tdsCheck = tdsValue;
      invoice.formGroup.get('tdsCheck')?.setValue(tdsValue);
    }
    if (includeAbbs) {
      const abbsValue = ((value * this.abbsPercent) / 100).toFixed(2);
      invoice.abbsCheck = abbsValue;
      invoice.formGroup.get('abbsCheck')?.setValue(abbsValue);
    }
  }
  getPendingAmount(item: any): number {
    let amount = 0;
    if (item.adjustedAmount) {
      amount = item.totalamount - item.adjustedAmount;
    } else if (item.pendingAmt) {
      amount = item.totalamount - item.pendingAmt;
    } else {
      amount = item.totalamount;
    }
    return amount ? parseFloat(amount.toFixed(2)) : 0;
  }
  getPaymentMode(): void {
    const url = '/commonList/paymentMode';
    this.mvnoManagementService
      .getMethod(url)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.paymentMode = response.dataList || [];
        },
        error: (error: any) => {
          this.commonService.toastError('Failed to load payment modes');
        },
      });
  }

  addPayment(): void {
    if (!this.paymentFormGroup.valid) {
      this.paymentFormGroup.markAllAsTouched();
      this.submitted = true;
      return;
    }

    const formValue = this.paymentFormGroup.value;
    formValue.paytype = formValue.invoiceId === 0 ? 'advance' : 'invoice';
    formValue.type = 'Payment';
    formValue.filename = this.fileName;
    const invoiceIds = this.selectedInvoice.map((element) => element.id);
    const paymentListPojos = this.selectedInvoice.map((element) => ({
      tdsAmountAgainstInvoice: element.tds,
      abbsAmountAgainstInvoice: element.abbs,
      amountAgainstInvoice: element.testamount,
      invoiceId: element.id,
    }));

    this.createPaymentData = {
      ...formValue,
      invoiceId: invoiceIds,
      paymentListPojos,
      onlinesource: this.paymentFormGroup.controls['onlinesource'].value,
    };

    delete this.createPaymentData.file;

    
    const formData = new FormData();
    if (this.file) {
      formData.append('file', this.file);
    }
    formData.append('spojo', JSON.stringify(this.createPaymentData));

    const url = '/record/payment';
    this.commonService.spinnerShow();

    this.mvnoManagementService
      .postMethodWithFile(url, formData)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.commonService.toastSuccess(
            response.message || 'Payment created successfully',
          );
          this.resetPaymentForm();
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error creating payment',
          );
        },
      });
  }
  private resetPaymentForm(): void {
    this.submitted = false;
    this.destinationbank = false;
    this.paymentFormGroup.reset();
    this.paymentAdded.emit();
    this.closeModal();
  }

  resetPayMode(): void {
    const controls = this.paymentFormGroup.controls;
    controls['chequeno'].disable();
    controls['chequedate'].disable();
    controls['bankManagement'].disable();
    controls['branch'].disable();
    controls['destinationBank'].disable();
    controls['reciptNo'].enable();
    this.chequeDateName = 'Cheque Date';

    controls['referenceno'].clearValidators();
    controls['referenceno'].updateValueAndValidity();
    controls['chequedate'].setValidators([]);
    controls['destinationBank'].setValidators([]);
    controls['bankManagement'].setValidators([]);
    controls['chequeno'].setValidators([]);
    controls['onlinesource'].setValidators([]);
    this.paymentFormGroup.updateValueAndValidity();
  }

  selPaySourceRecord(event: any): void {
    const paySource = event.value?.toLowerCase();
    const controls = this.paymentFormGroup.controls;

    switch (paySource) {
      case 'cash_via_bank':
        controls['destinationBank'].enable();
        controls['destinationBank'].setValidators([Validators.required]);
        controls['destinationBank'].updateValueAndValidity();
        controls['branch'].enable();
        break;

      case 'cash_in_hand':
        controls['destinationBank'].disable();
        controls['destinationBank'].clearValidators();
        controls['destinationBank'].updateValueAndValidity();
        controls['branch'].disable();
        break;
    }
  }
  checkPaymentMode(formPayModeValue: string): boolean {
    if (
      formPayModeValue &&
      (formPayModeValue === 'vatreceiveable' ||
        formPayModeValue === 'tds' ||
        formPayModeValue === 'abbs')
    ) {
      return true;
    }
    return false;
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
  keypressId(event: KeyboardEvent): void {
    const pattern = /[0-9\.]/;
    const inputChar = String.fromCharCode(event.charCode);

    if (
      event.keyCode !== 8 &&
      event.keyCode !== 9 &&
      !pattern.test(inputChar)
    ) {
      event.preventDefault();
    }
  }

  onFileChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.file = target.files[0];
      this.fileName = target.files[0].name;
    }
  }
  checkUncheckAllInvoice(): void {
    this.invoiceList.forEach((invoice) => {
      invoice.isSelected = this.masterSelected;
    });
    this.getCheckedItemListInvoice();
  }
  getCheckedItemListInvoice(): void {
    this.selectedInvoice = this.invoiceList.filter(
      (invoice) => invoice.isSelected,
    );
  }
  isAllSelectedInvoice(): void {
    this.masterSelected = this.invoiceList.every(
      (item) => item.isSelected === true,
    );
    this.getCheckedItemListInvoice();
  }
  bindInvoice(): void {
    this.getCustomersDetail();

    if (this.selectedInvoice.length >= 1) {
      this.isShowInvoiceList = true;
      this.Amount = 0;

      this.selectedInvoice.forEach((element) => {
        if (element.testamount !== null) {
          this.Amount += parseFloat(element.testamount);
        }
      });

      this.paymentFormGroup.patchValue({
        invoiceId: this.selectedInvoice.map((item) => item.id),
        amount: this.Amount.toFixed(2),
      });

      this.onChangeOFAmountTest(this.selectedInvoice);
      this.destinationbank = true;
    } else {
      this.commonService.toastError(
        'Please select at least one invoice or advance mode.',
      );
    }

    
    if (this.selectedInvoice.length === 2) {
      const hasAdvance = this.selectedInvoice.some(
        (element) => element.docnumber === 'Advance',
      );
      if (hasAdvance) {
        this.selectedInvoice = [];
        this.invoiceList.forEach((element) => {
          element.isSelected = false;
        });
        this.masterSelected = false;
        this.commonService.toastError('Please select advance mode value only.');
      }
    }

    this.displaySelectInvoiceDialog = false;
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
          this.paymentFormGroup.patchValue({
            customerid: this.customerLedgerDetailData.id || custId,
          });
        },
        error: (error: any) => {
          this.paymentFormGroup.patchValue({
            customerid: custId,
          });
          this.commonService.toastError('Error fetching customer details');
        },
      });
  }
  onChangeOFAmountTest(event: any): void {
    if (this.selectedInvoice.length >= 1) {
      const isAbbsTdsMode = this.checkPaymentMode(
        this.paymentFormGroup.controls['paymode'].value?.toLowerCase(),
      );

      let totaltdsAmount = 0;
      let totalabbsAmount = 0;

      this.selectedInvoice.forEach((element) => {
        let tds = 0;
        let abbs = 0;

        if (element.includeTds === true) {
          tds = Number(element.tdsCheck);
          totaltdsAmount += Number(element.tdsCheck);
          this.isTdsFlag = true;
        }

        if (element.includeAbbs === true) {
          abbs = Number(element.abbsCheck);
          totalabbsAmount += Number(element.abbsCheck);
          this.isAbbsFlag = true;
        }

        if (isAbbsTdsMode) {
          element.tds = 0;
          element.abbs = 0;
        } else {
          element.tds = tds;
          element.abbs = abbs;
        }
      });

      if (isAbbsTdsMode) {
        this.paymentFormGroup.controls['abbsAmount'].setValue(0);
        this.paymentFormGroup.controls['tdsAmount'].setValue(0);
      } else {
        this.paymentFormGroup.controls['abbsAmount'].setValue(totalabbsAmount);
        this.paymentFormGroup.controls['tdsAmount'].setValue(totaltdsAmount);
      }
    }
  }
  modalCloseInvoiceList(): void {
    this.getCustomersDetail();
    this.paymentFormGroup.patchValue({
      invoiceId: this.selectedInvoice.map((item) => item.id),
      amount: this.selectedInvoice[0]?.refundAbleAmount || 0,
    });
    this.isShowInvoiceList = true;
    this.displaySelectInvoiceDialog = false;
    this.newFirst = 0;
  }
  onChangeOFTDSTest(event: any, data: any): void {
    if (event.checked && data.totalamount) {
      data.includeTds = true;
      data.tdsCheck = ((data.testamount * this.tdsPercent) / 100).toFixed(2);
      data.tds = ((data.testamount * this.tdsPercent) / 100).toFixed(2);
    } else {
      data.includeTds = false;
      data.tdsCheck = 0;
      data.tds = 0;
    }
  }
  onChangeOFABBSTest(event: any, data: any): void {
    if (event.checked && data.totalamount) {
      data.includeAbbs = true;
      data.abbsCheck = ((data.testamount * this.abbsPercent) / 100).toFixed(2);
      data.abbs = ((data.testamount * this.abbsPercent) / 100).toFixed(2);
    } else {
      data.includeAbbs = false;
      data.abbsCheck = 0;
      data.abbs = 0;
    }
  }
  modalOpenInvoice(): void {
    this.displaySelectInvoiceDialog = true;
  }
}
