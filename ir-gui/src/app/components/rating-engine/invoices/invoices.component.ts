import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil } from 'rxjs/operators';
import {
  PaginationService,
  CommonService,
  pageSelection,
  tablePageSize,
} from 'src/app/core.index';
import { IInvoice, IInvoicePagedResponse, IInvoiceSearchCriteria } from './invoices.interface';
import { InvoicesService } from './invoices.service';

@Component({
  selector: 'app-invoices',
  templateUrl: './invoices.component.html',
  styleUrls: ['./invoices.component.scss'],
  standalone: false,
})
export class InvoicesComponent implements OnInit, OnDestroy {
  /** Slice of data currently visible on the table. */
  tableData: IInvoice[] = [];
  totalData = 0;
  pageSize = 10;
  paginationSkip = 0;
  serialNumberArray: number[] = [];

  // ── Active search criteria (null when no filters are applied) ─────────────
  private activeSearchCriteria: IInvoiceSearchCriteria | undefined = undefined;

  // ── Filter controls ───────────────────────────────────────────────────────
  /** Maps to searchCriteria.searchTerm (Agreement Code free-text search) */
  filterSearchTerm = new FormControl('');
  /** Maps to searchCriteria.status */
  filterStatus = new FormControl('');
  /** Maps to searchCriteria.billingCycleStartFrom */
  filterBillingCycleStartFrom = new FormControl('');
  /** Maps to searchCriteria.billingCycleStartTo */
  filterBillingCycleStartTo = new FormControl('');
  /** Maps to searchCriteria.agreementId */
  filterAgreementId = new FormControl('');

  private destroy$ = new Subject<void>();
  private lastPayload = '';
  
  statusOptions = [
    { label: 'Generated', value: 'GENERATED' },
    { label: 'Pending', value: 'PENDING' },
    { label: 'Failed', value: 'FAILED' },
    { label: 'Cancelled', value: 'CANCELLED' }
  ];

  schedulingForm: FormGroup;


  constructor(
    private fb: FormBuilder,
    private pagination: PaginationService,
    private commonService: CommonService,
    private invoicesService: InvoicesService
  ) {
    this.schedulingForm = this.fb.group({
      startDate: [null],
      endDate: [null],
    });
  }

  ngOnInit(): void {
    this.pagination.tablePageSize
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe((res: tablePageSize) => {
        this.paginationSkip = res.skip;
        const key = JSON.stringify({ skip: res.skip, limit: res.pageSize });
        if (this.lastPayload !== key) {
          this.lastPayload = key;
          this.pageSize = res.pageSize;
          this.loadData({ skip: res.skip, limit: res.pageSize });
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ───────────────────────────────────────────────────────────────────────────
  // SEARCH / FILTER ACTIONS
  // ───────────────────────────────────────────────────────────────────────────

  private formatDateForAPI(date: any): string | null {
    if (!date) return null;
    const d = new Date(date);
    return d.toISOString();
  }

  /** Build the searchCriteria object from current filter control values. */
  private buildSearchCriteria(): IInvoiceSearchCriteria | undefined {
    const criteria: IInvoiceSearchCriteria = {};

    const searchTerm = this.filterSearchTerm.value?.trim();
    if (searchTerm) criteria.searchTerm = searchTerm;

    const status = this.filterStatus.value?.trim();
    if (status) criteria.status = status;

    const billingFrom = this.formatDateForAPI(this.schedulingForm.get('startDate')?.value);
    if (billingFrom) criteria.billingCycleStartFrom = billingFrom;

    const billingTo = this.formatDateForAPI(this.schedulingForm.get('endDate')?.value);
    if (billingTo) criteria.billingCycleStartTo = billingTo;

    const agreementIdRaw = this.filterAgreementId.value != null
      ? String(this.filterAgreementId.value).trim()
      : '';
    if (agreementIdRaw) {
      const parsed = parseInt(agreementIdRaw, 10);
      if (!isNaN(parsed)) criteria.agreementId = parsed;
    }

    return Object.keys(criteria).length > 0 ? criteria : undefined;
  }

  /** Trigger a fresh page-1 load with the current filter values. */
  searchData(): void {
    this.activeSearchCriteria = this.buildSearchCriteria();
    this.lastPayload = '';
    this.loadData({ skip: 0, limit: this.pageSize });

    console.log("========" + JSON.stringify(this.activeSearchCriteria) + "========")
  }

  /** Reset all filters and reload unfiltered page 1. */
  clearSearch(): void {
    this.filterSearchTerm.setValue('');
    this.filterStatus.setValue('');
    this.filterBillingCycleStartFrom.setValue('');
    this.filterBillingCycleStartTo.setValue('');
    this.filterAgreementId.setValue('');
    this.activeSearchCriteria = undefined;
    this.lastPayload = '';
    this.loadData({ skip: 0, limit: this.pageSize });
  }

  // ───────────────────────────────────────────────────────────────────────────
  // DATA LOADING
  // ───────────────────────────────────────────────────────────────────────────

  private loadData(pageOption: pageSelection): void {
    const page = Math.floor(pageOption.skip / pageOption.limit) + 1;

    this.commonService.spinnerShow();
    this.invoicesService
      .getPaginatedInvoices(page, pageOption.limit, this.activeSearchCriteria)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (res: IInvoicePagedResponse) => {
          this.commonService.spinnerHide();
          // Extract pagination fields from the nested pageDetails object.
          this.totalData = res.pageDetails.totalRecords;
          this.tableData = res.content;
          this.serialNumberArray = this.tableData.map((_, i) => i + 1);
          this.pagination.calculatePageSize.next({
            totalData: this.totalData,
            pageSize: res.pageDetails.totalRecordsPerPage,
            tableData: this.tableData,
            serialNumberArray: this.serialNumberArray,
          });
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(
            err?.error?.msg || 'Failed to load invoices'
          );
        },
      });
  }

  // ───────────────────────────────────────────────────────────────────────────
  // UTILITY HELPERS
  // ───────────────────────────────────────────────────────────────────────────

  /**
   * Returns the CSS modifier class for the status badge.
   */
  statusBadgeClass(status: string): string {
    switch (status?.toUpperCase()) {
      case 'GENERATED': return 'bg-success';
      case 'PENDING':   return 'bg-warning';
      case 'FAILED':    return 'bg-danger';
      case 'CANCELLED': return 'bg-secondary';
      default:          return 'bg-info';
    }
  }

  /**
   * Converts a SNAKE_CASE / UPPER_CASE string to a readable label.
   * e.g. "NET_PAYABLE" → "Net Payable"
   */
  toLabel(value: string): string {
    if (!value) return '-';
    return value
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
      .join(' ');
  }

  /**
   * Triggers download of the invoice PDF.
   * Calls GET /api/invoices/download/{id}, receives the multipart/PDF blob,
   * and forces the browser to save it as "Invoice-<id>.pdf".
   */
  downloadInvoice(invoice: IInvoice): void {
    this.commonService.spinnerShow();
    this.invoicesService
      .downloadInvoice(invoice.invoiceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          this.commonService.spinnerHide();

          // Build a temporary object URL from the blob and click an
          // invisible anchor to trigger the browser's Save dialog.
          const url = window.URL.createObjectURL(blob);
          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download = `Invoice-${invoice.invoiceId}.pdf`;
          anchor.style.display = 'none';
          document.body.appendChild(anchor);
          anchor.click();

          // Clean up: remove the element and release the object URL.
          document.body.removeChild(anchor);
          window.URL.revokeObjectURL(url);
        },
        error: (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(
            err?.error?.msg || 'Failed to download invoice. Please try again.'
          );
        },
      });
  }
}
