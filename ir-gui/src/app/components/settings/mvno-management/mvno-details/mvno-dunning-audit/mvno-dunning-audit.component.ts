import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Subject, debounceTime, finalize, takeUntil } from 'rxjs';
import {
  CommonService,
  pageSelection,
  PaginationService,
  tablePageSize,
} from 'src/app/core.index';
import { MvnoManagementService } from '../../mvno-management.service';
import { CustomPaginationModule } from 'src/app/core/shared/custom-pagination/custom-pagination.module';

@Component({
  selector: 'app-mvno-dunning-audit',
  templateUrl: './mvno-dunning-audit.component.html',
  styleUrl: './mvno-dunning-audit.component.scss',
  imports: [CommonModule, CustomPaginationModule],
})
export class MvnoDunningAuditComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  @Input() mvnoData: any;
  @Input() mvnoId: string = '';
  paginationSkip = 0;
  dunningAuditData: any[] = [];
  DunningitemsPerPage: number = 5;
  currentPageDunning: number = 0;
  totalDunningRecords: number = 0;
  private lastPayload: string = '';
  pageSize = 10;

  constructor(
    private commonService: CommonService,
    private mvnoManagementService: MvnoManagementService,
    private pagination: PaginationService,
  ) {}

  ngOnInit(): void {
    
    const custId = this.mvnoData?.custInvoiceRefId || this.mvnoId;
    if (custId) {
      this.getDunningData(custId);
    }
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const currentPayload = JSON.stringify({
          skip: res.skip,
          limit: res.limit,
        });
        if (this.lastPayload !== currentPayload) {
          this.lastPayload = currentPayload;
          this.getDunningDaata({ skip: res.skip, limit: res.pageSize });
          this.pageSize = res.pageSize;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  getDunningData(custId: string, size?: number): void {
    const page = this.currentPageDunning;
    const pageSize = size || this.DunningitemsPerPage;

    const data = {
      filters: [
        {
          filterColumn: 'customer',
          filterValue: custId,
        },
      ],
      page,
      pageSize,
    };

    const url = '/dunnninghistory/findByAgentOrCustomerId';
    this.commonService.spinnerShow();
    this.mvnoManagementService
      .dunningHistoryMethod(url, data)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (res: any) => {
          const history = res.customerDunningHistory || {};
          this.dunningAuditData = history.content || [];
          this.totalDunningRecords = history.totalElements || 0;
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR || 'Error fetching dunning audit data',
          );
        },
      });
  }

  getDunningDaata(pageOption: pageSelection): void {
    const page = pageOption.skip / pageOption.limit + 1;

    const data = {
      filters: [
        {
          filterColumn: 'customer',
          filterValue: this.mvnoData.custInvoiceRefId,
        },
      ],
      page,
      pageSize: pageOption.limit,
    };

    const url = 'dunnninghistory/findByAgentOrCustomerId';
    this.commonService.spinnerShow();
    this.mvnoManagementService
      .dunningHistoryMethod(url, data)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
      )
      .subscribe({
        next: (res: any) => {
          const history = res.customerDunningHistory || {};
          this.dunningAuditData = history.content || [];
          this.totalDunningRecords = history.totalElements || 0;
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.errorMessage || 'Error fetching dunning audit data',
          );
        },
      });
  }
}
