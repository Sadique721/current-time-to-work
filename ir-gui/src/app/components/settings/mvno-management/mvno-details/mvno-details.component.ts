import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CommonService, routes } from 'src/app/core.index';
import { MvnoManagementService } from '../mvno-management.service';
import { finalize, Subject, takeUntil } from 'rxjs';
import { MvnoInvoiceComponent } from './mvno-invoice/mvno-invoice.component';
import { MvnoPaymentComponent } from './mvno-payment/mvno-payment.component';
import { M } from '@angular/material/ripple-loader.d-C3HznB6v';
import { MvnoDunningAuditComponent } from './mvno-dunning-audit/mvno-dunning-audit.component';
import { MvnoLedgerComponent } from './mvno-ledger/mvno-ledger.component';

@Component({
  selector: 'app-mvno-details',
  templateUrl: './mvno-details.component.html',
  styleUrl: './mvno-details.component.scss',
  imports: [
    CommonModule,
    RouterModule,
    MvnoInvoiceComponent,
    MvnoPaymentComponent,
    MvnoDunningAuditComponent,
    MvnoLedgerComponent,
  ],
})
export class MvnoDetailsComponent implements OnInit {
  routes = routes;
  mvnoData: any;
  mvnoId: any;
  private destroy$ = new Subject<void>();
  isMvnoBasicdetailsShow = true;
  isMvnoInvoiceShow = false;
  isMvnoPaymentShow = false;
  isMvnoDunningAuditShow = false;
  isMvnoLedgerShow = false;

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private sanitizer: DomSanitizer,
    private mvnoService: MvnoManagementService,
  ) {}

  ngOnInit(): void {
    const mvnoId = this.route.snapshot.paramMap.get('id');
    if (mvnoId) {
      this.getMvnoById(mvnoId);
    }
  }

  getMvnoById(id: string): void {
    this.commonService.spinnerShow();
    const url = `/mvno/${id}`;
    this.mvnoService
      .getMethod(url)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.commonService.spinnerHide();
        }),
      )
      .subscribe({
        next: (response: any) => {
          this.mvnoData = response.data || {};
        },
        error: (error: any) => {
          this.commonService.toastError(
            error?.error?.ERROR ||
              error?.error?.msg ||
              'Error fetching MVNO details',
          );
        },
      });
  }
  MvnoBasicDetail(): void {
    this.isMvnoBasicdetailsShow = true;
    this.isMvnoInvoiceShow = false;
    this.isMvnoPaymentShow = false;
    this.isMvnoDunningAuditShow = false;
    this.isMvnoLedgerShow = false;
  }
  showMvnoInvoice(): void {
    this.isMvnoInvoiceShow = true;
    this.isMvnoBasicdetailsShow = false;
    this.isMvnoPaymentShow = false;
    this.isMvnoDunningAuditShow = false;
    this.isMvnoLedgerShow = false;
  }
  showMvnoPayment(): void {
    this.isMvnoPaymentShow = true;
    this.isMvnoBasicdetailsShow = false;
    this.isMvnoInvoiceShow = false;
    this.isMvnoDunningAuditShow = false;
    this.isMvnoLedgerShow = false;
  }
  showDunningAudit(): void {
    this.isMvnoDunningAuditShow = true;
    this.isMvnoBasicdetailsShow = false;
    this.isMvnoInvoiceShow = false;
    this.isMvnoPaymentShow = false;
    this.isMvnoLedgerShow = false;
  }
  showMvnoLedger(): void {
    this.isMvnoLedgerShow = true;
    this.isMvnoBasicdetailsShow = false;
    this.isMvnoInvoiceShow = false;
    this.isMvnoPaymentShow = false;
    this.isMvnoDunningAuditShow = false;
  }
}
