import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  Validators,
} from '@angular/forms';
import { CommonService } from 'src/app/core/service/common.service';
import { StaffManagementService } from '../../staff-management.service';
import { CommonModule } from '@angular/common';
import { catchError, Subject, takeUntil } from 'rxjs';
import { SelectModule } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { sharedModule } from 'src/app/core.index';

@Component({
  selector: 'app-staff-wallet-ledger',
  standalone: true,
  imports: [CommonModule, sharedModule, SelectModule, FloatLabel],
  templateUrl: './staff-wallet-ledger.component.html',
  styleUrl: './staff-wallet-ledger.component.scss',
})
export class StaffWalletLedgerComponent implements OnInit, OnDestroy {
  @Input() bankList: any[] = [];
  @Input() currency: string = '₹';
  @Input() staffDetails: any;

  destroy$ = new Subject<void>();

  walletForm!: FormGroup;
  additionalDetails: any[] = [];
  paymentModes: { label: string; value: string }[] = [];
  staffLedgerData: any[] = [];
  staffLedgerChequeData: any[] = [];

  WalletAmount: number = 0;

  searchOption = [
    { label: 'Mode', value: 'Mode' },
    { label: 'Status', value: 'Status' },
  ];
  selectSearchOption = new FormControl('Mode');
  paymentModeControl = new FormControl();
  statusControl = new FormControl();

  constructor(
    private fb: FormBuilder,
    private staffManagementService: StaffManagementService,
    private commonService: CommonService,
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadWalletData();
  }

  initForm(): void {
    this.walletForm = this.fb.group({
      date: ['', Validators.required],
      paymentMode: ['', Validators.required],
      amount: [''],
      bankId: ['', Validators.required],
      remarks: ['', Validators.required],
    });
  }

  loadWalletData(): void {
    if (!this.staffDetails?.id) return;

    this.staffManagementService
      .getFromCMS(`/staff_ledger_details/walletAmount/${this.staffDetails.id}`)
      .pipe(
        takeUntil(this.destroy$),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.errorMessage || 'Failed to load wallet amount.',
          );
          throw error;
        }),
      )
      .subscribe((res: any) => {
        this.WalletAmount = res?.availableAmount || 0;
        this.loadLedgerData();
      });
  }

  loadLedgerData(): void {
    this.staffManagementService
      .getFromCMS(
        `/staff_ledger_details/getStaffLedgerDetailsbyStaffId/${this.staffDetails.id}`,
      )
      .pipe(takeUntil(this.destroy$))
      .subscribe((response: any) => {
        const ledgerData = response.dataList || [];

        this.staffLedgerData = ledgerData;
        this.staffLedgerChequeData = ledgerData.filter(
          (entry: any) =>
            entry.paymentMode === 'Cheque' && entry.status === 'Pending',
        );

        const uniqueModes = Array.from(
          new Set(ledgerData.map((entry: any) => entry.paymentMode)),
        ).filter(Boolean);

        this.paymentModes = uniqueModes.map((mode) => ({
          label: mode as string,
          value: mode as string,
        }));

        this.generateSummary(ledgerData);
      });
  }

  generateSummary(data: any[]): void {
    const summaryMap = new Map<string, { credit: number; withdraw: number }>();

    data.forEach((entry) => {
      const mode = entry.paymentMode;
      if (!summaryMap.has(mode)) {
        summaryMap.set(mode, { credit: 0, withdraw: 0 });
      }

      const current = summaryMap.get(mode)!;

      if (entry.action === 'Collected') {
        current.credit += entry.amount;
      } else if (entry.action === 'Withdraw') {
        current.withdraw += entry.amount;
      }
    });

    this.additionalDetails = Array.from(summaryMap.entries()).map(
      ([mode, { credit, withdraw }]) => ({
        mode,
        credit,
        withdraw,
      }),
    );
  }

  search(): void {
    const option = this.selectSearchOption.value;
    const mode = this.paymentModeControl.value;
    const status = this.statusControl.value;

    if (option === 'Mode') {
      this.staffLedgerData = this.staffLedgerData.filter(
        (entry) => entry.paymentMode === mode,
      );
    } else if (option === 'Status') {
      this.staffLedgerData = this.staffLedgerData.filter(
        (entry) => entry.status === status,
      );
    }
  }

  clearSearch(): void {
    this.selectSearchOption.setValue('Mode');
    this.paymentModeControl.reset();
    this.statusControl.reset();
    this.staffLedgerData = [...this.staffLedgerData];
  }

  getBankName(bankId: number): string {
    return this.bankList.find((b) => b.id === bankId)?.bankname || '';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
