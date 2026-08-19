import { Component, OnInit, OnDestroy } from '@angular/core';
import {
  UntypedFormGroup,
  UntypedFormControl,
  Validators,
} from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { CommonService } from 'src/app/core.index';
import { SchedulerConfigurationService } from './scheduler-configuration.service';

@Component({
  selector: 'app-scheduler-configuration',
  templateUrl: './scheduler-configuration.component.html',
  styleUrls: ['./scheduler-configuration.component.scss'],
  standalone: false,
})
export class SchedulerConfigurationComponent implements OnInit, OnDestroy {
  isActive: boolean = false;
  isLoading: boolean = false;
  isToggling: boolean = false;
  /** True only after a successful GET that returned scheduler data from the DB */
  hasData: boolean = false;
  /** Permanently disabled if a toggle PATCH call fails (e.g. no DB entry yet) */
  isToggleDisabled: boolean = false;

  latestStatus: any = null;
  isStatusLoading: boolean = false;
  statusError: boolean = false;

  schedulerForm: UntypedFormGroup = new UntypedFormGroup({
    startTimestamp: new UntypedFormControl('', [Validators.required]),
    intervalType: new UntypedFormControl('HOUR', [Validators.required]),
    intervalValue: new UntypedFormControl(null, [
      Validators.required,
      Validators.min(1),
    ]),
    targetedTimestamp: new UntypedFormControl('', [Validators.required]),
    targetedIntervalType: new UntypedFormControl('DAY', [Validators.required]),
    targetedMaxIntervalValue: new UntypedFormControl(null, [
      Validators.required,
      Validators.min(1),
    ]),
  });

  private destroy$ = new Subject<void>();

  /** Always returns the current local datetime in "YYYY-MM-DDTHH:mm" format,
   *  used as the `min` attribute on the Start Timestamp input. */
  get minStartTimestamp(): string {
    const now = new Date();
    // toISOString() gives UTC; adjust to local time offset
    const offset = now.getTimezoneOffset() * 60000;
    const local = new Date(now.getTime() - offset);
    return local.toISOString().substring(0, 16);
  }

  constructor(
    private schedulerService: SchedulerConfigurationService,
    private commonService: CommonService
  ) {}

  ngOnInit(): void {
    this.fetchConfig();
    this.fetchLatestStatus();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** GET /api/schedular — load existing configuration and pre-fill the form */
  private fetchConfig(): void {
    this.isLoading = true;
    this.schedulerService
      .getConfig()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isLoading = false))
      )
      .subscribe({
        next: (res: any) => {
          if (!res) return;

          // Mark that the DB has an entry — toggle becomes interactive
          this.hasData = true;
          this.isToggleDisabled = false;

          // API returns "active", not "isActive"
          this.isActive = !!res.active;

          this.schedulerForm.patchValue({
            startTimestamp: this.toInputFormat(res.startTimestamp),
            intervalType: res.intervalType || 'HOUR',
            intervalValue: res.intervalValue ?? null,
            targetedTimestamp: this.toInputFormat(res.targetedTimestamp),
            targetedIntervalType: res.targetedIntervalType || 'DAY',
            targetedMaxIntervalValue: res.targetedMaxIntervalValue ?? null,
          });
        },
        error: () => {
          // No data in DB — keep toggle disabled so user cannot activate
          this.hasData = false;
          this.isToggleDisabled = true;
          this.isActive = false;
        },
      });
  }

  /** GET /api/scheduler-status/latest/ROAMING */
  fetchLatestStatus(): void {
    this.isStatusLoading = true;
    this.statusError = false;
    this.latestStatus = null;

    this.schedulerService
      .getLatestStatus()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isStatusLoading = false))
      )
      .subscribe({
        next: (res: any) => {
          if (res) {
            this.latestStatus = res;
          } else {
            this.statusError = true;
          }
        },
        error: () => {
          this.statusError = true;
          this.latestStatus = null;
        },
      });
  }

  /** PATCH /api/schedular?isActive=... — called immediately on toggle flip */
  toggleActive(): void {
    // Guard against double-fire (e.g. label + input both emitting change)
    if (this.isToggling) return;
    this.isToggling = true;

    const newState = !this.isActive;

    this.schedulerService
      .patchActive(newState)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => (this.isToggling = false))
      )
      .subscribe({
        next: () => {
          this.isActive = newState;
          this.commonService.toastSuccess(
            `Scheduler ${newState ? 'enabled' : 'disabled'} successfully`
          );
        },
        error: (err: any) => {
          // Revert the attempted toggle and permanently disable the switch
          // so the user cannot activate a scheduler that has no DB entry.
          this.isActive = false;
          this.isToggleDisabled = true;
          this.commonService.toastError(
            err?.error?.msg || 'Failed to update scheduler status. Please save a configuration first.'
          );
        },
      });
  }

  /** PUT /api/schedular — called on Save button */
  saveConfig(): void {
    if (this.schedulerForm.invalid) {
      this.schedulerForm.markAllAsTouched();
      return;
    }

    const raw = this.schedulerForm.getRawValue();
    const payload = {
      // type: 'interconnect',
      'interconnectType': 'IP',
      startTimestamp: this.toTimestamp(raw.startTimestamp),
      intervalType: raw.intervalType as string,
      intervalValue: Number(raw.intervalValue),
      targetedTimestamp: this.toTimestamp(raw.targetedTimestamp),
      targetedIntervalType: raw.targetedIntervalType as string,
      targetedMaxIntervalValue: Number(raw.targetedMaxIntervalValue),
      isActive: this.isActive,
    };

    this.commonService.spinnerShow();
    this.schedulerService
      .putConfig(payload)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: () => {
          this.commonService.toastSuccess(
            'Scheduler configuration saved successfully'
          );
          // Re-fetch so hasData / isToggleDisabled / isActive are updated
          // from the real server state — no manual page refresh needed.
          this.fetchConfig();
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.msg || 'Failed to save scheduler configuration'
          );
        },
      });
  }

  /**
   * Converts an API timestamp string (YYYY-MM-DDTHH:mm:ss)
   * into the value format expected by <input type="datetime-local">
   * which requires YYYY-MM-DDTHH:mm (no seconds).
   */
  private toInputFormat(value: string | null | undefined): string {
    if (!value) return '';
    // datetime-local input needs "YYYY-MM-DDTHH:mm"
    // Slice off seconds if present: "2024-01-15T10:00:00" → "2024-01-15T10:00"
    return value.length >= 16 ? value.substring(0, 16) : value;
  }

  /**
   * Converts a datetime-local input value (YYYY-MM-DDTHH:mm)
   * into the strict API format (YYYY-MM-DDTHH:mm:ss).
   */
  private toTimestamp(value: string): string {
    if (!value) return '';
    // Append :00 for seconds if not already present
    return value.length === 16 ? `${value}:00` : value;
  }
}
