import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { Subject, debounceTime, finalize, takeUntil } from 'rxjs';
import { CommonService, sharedModule, tablePageSize } from 'src/app/core.index';
import { PaginationService } from 'src/app/core/service/pagination.service';
import { CommonModule } from '@angular/common';
import { SelectModule } from 'primeng/select';
import { CustomElementModule } from 'src/app/core/shared/custom-elements/custom-elemets.module';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';
import { MvnoManagementService } from '../../mvno-management.service';
import customDatetime from 'src/app/core/shared/custom-elements/custom-datetime.pipe';
import { SystemConfigService } from '../../../system-config/system-config.service';

@Component({
  selector: 'app-mvno-ledger',
  imports: [
    SelectModule,
    CommonModule,
    CustomPaginationModule,
    sharedModule,
    CustomElementModule,
  ],
  templateUrl: './mvno-ledger.component.html',
  styleUrl: './mvno-ledger.component.scss',
})
export class MvnoLedgerComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  @Input() mvnoId: string = '';
  @Input() mvnoData: any;
  totalData = 0;
  tableData: any[] = [];
  serialNumberArray: number[] = [];
  startDate = new UntypedFormControl(null);
  endDate = new UntypedFormControl(null);
  currency: string = '';

  ledgerData: any[] = [];
  totalRecords = 0;
  pageSize = 10;
  currentPage = 0;
  lastPayload = '';
  submitted = false;
  paginationSkip = 0;

  customerLedgerData: any = {
    title: '',
    firstname: '',
    lastname: '',
    plan: '',
    status: '',
    username: '',
    customerLedgerInfoPojo: {
      openingAmount: '',
      closingBalance: '',
    },
  };
  constructor(
    private revenueManagementService: MvnoManagementService,
    private commonService: CommonService,
    private pagination: PaginationService,
    private systemService: SystemConfigService,
  ) {}

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
      });
    this.setupPaginationListener();
    this.getCurrencyConfig();
  }
  private getCurrencyConfig() {
    this.systemService
      .getConfigurationByName('CURRENCY_FOR_PAYMENT')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: any) => {
          if (res?.data?.value) {
            this.currency = res.data.value;
          }
        },
        error: () => {
          this.commonService.toastError('Error fetching currency config');
        },
      });
  }
  private setupPaginationListener() {
    this.pagination.tablePageSize
      .pipe(takeUntil(this.destroy$))
      .subscribe((res: any) => {
        const payload = {
          custId: this.mvnoData?.custInvoiceRefId || this.mvnoId,

          CREATE_DATE: this.startDate?.value
            ? customDatetime(this.startDate?.value?.toISOString(), {
                format: 'YYYY-MM-DD',
              })
            : null,
          END_DATE: this.endDate?.value
            ? customDatetime(this.endDate?.value?.toISOString(), {
                format: 'YYYY-MM-DD',
              })
            : null,
          page: res.skip / res.pageSize,
          pageSize: res.pageSize,
        };

        const key = JSON.stringify(payload);
        if (this.lastPayload !== key) {
          this.lastPayload = key;
          this.pageSize = res.pageSize;
          this.loadLedger(payload);
        }
      });
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  loadLedger(payload: any) {
    this.commonService.spinnerShow();
    const url = 'customerLedgers';
    this.revenueManagementService
      .ledgerMethod(url, payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (response: any) => {
          this.customerLedgerData = response.customerLedgerDtls || {};
          this.ledgerData =
            response.customerLedgerDtls?.customerLedgerInfoPojo
              ?.debitCreditDetail || [];
          this.totalRecords =
            response.customerLedgerDtls?.customerLedgerInfoPojo?.totalRecords ||
            0;
          this.updatePagination();
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching ledger data',
          );
        },
      });
  }
  private updatePagination() {
    this.pagination.calculatePageSize.next({
      totalData: this.totalData,
      pageSize: this.pageSize,
      tableData: this.tableData,
      serialNumberArray: this.serialNumberArray,
    });
  }

  onSearch() {
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  clearSearch(): void {
    this.startDate.setValue(null);
    this.endDate.setValue(null);
    this.lastPayload = '';
    this.pagination.tablePageSize.next({
      skip: 0,
      limit: this.pageSize,
      pageSize: this.pageSize,
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
