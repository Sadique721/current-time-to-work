import { Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, throwError } from 'rxjs';
import { takeUntil, finalize, catchError } from 'rxjs/operators';
import { CommonService, routes } from 'src/app/core.index';
import { TapConfigurationService } from '../tap-configuration.service';

@Component({
  selector: 'app-tap-profile-group-add-edit',
  templateUrl: './tap-profile-group-add-edit.component.html',
  standalone: false
})
export class TapProfileGroupAddEditComponent implements OnInit, OnDestroy {
  groupId: number | null = null;
  groupForm: UntypedFormGroup;
  private destroy$ = new Subject<void>();

  voiceProfiles: any[] = [];
  smsProfiles: any[] = [];
  usageProfiles: any[] = [];
  public routes = routes;

  constructor(
    private commonService: CommonService,
    private tapService: TapConfigurationService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.groupForm = new UntypedFormGroup({
      name: new UntypedFormControl('', [Validators.required]),
      description: new UntypedFormControl(''),
      isActive: new UntypedFormControl(true),
      voiceProfileId: new UntypedFormControl(null),
      smsProfileId: new UntypedFormControl(null),
      usageProfileId: new UntypedFormControl(null)
    });
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const idVal = params.get('id');
      if (idVal) {
        this.groupId = Number(idVal);
      }
      this.loadProfiles();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadProfiles(): void {
    this.commonService.spinnerShow();
    this.tapService.getProfilesByServiceType()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (res: any) => {
          this.voiceProfiles = res.voice || [];
          this.smsProfiles = res.sms || [];
          this.usageProfiles = res.usage || [];

          if (this.groupId) {
            this.loadGroupDetail(this.groupId);
          }
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.errorMessage || err?.error?.msg || 'Failed to fetch profiles by service type'
          );
        }
      });
  }

  private loadGroupDetail(id: number): void {
    this.commonService.spinnerShow();
    this.tapService.getTapProfileGroupById(id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (res: any) => {
          if (res) {
            this.patchForm(res);
          }
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.errorMessage || 'Failed to fetch profile group details'
          );
        }
      });
  }

  private patchForm(src: any): void {
    this.groupForm.patchValue({
      name: src.name || '',
      description: src.description || '',
      isActive: src.active ?? src.isActive ?? true,
      voiceProfileId: null,
      smsProfileId: null,
      usageProfileId: null
    });

    const ids = src.tapProfileIds || [];
    ids.forEach((id: any) => {
      const numId = Number(id);
      if (this.voiceProfiles.some(p => p.id === numId)) {
        this.groupForm.get('voiceProfileId')?.setValue(numId);
      } else if (this.smsProfiles.some(p => p.id === numId)) {
        this.groupForm.get('smsProfileId')?.setValue(numId);
      } else if (this.usageProfiles.some(p => p.id === numId)) {
        this.groupForm.get('usageProfileId')?.setValue(numId);
      } else {
        const found = src.tapProfiles?.find((p: any) => p.id === numId);
        if (found) {
          if (found.serviceType === 'VOICE') this.groupForm.get('voiceProfileId')?.setValue(numId);
          else if (found.serviceType === 'SMS') this.groupForm.get('smsProfileId')?.setValue(numId);
          else if (found.serviceType === 'USAGE') this.groupForm.get('usageProfileId')?.setValue(numId);
        }
      }
    });
  }

  submit(): void {
    if (this.groupForm.invalid) {
      this.groupForm.markAllAsTouched();
      return;
    }

    const tapProfileIds: number[] = [];
    const vId = this.groupForm.get('voiceProfileId')?.value;
    const sId = this.groupForm.get('smsProfileId')?.value;
    const uId = this.groupForm.get('usageProfileId')?.value;

    if (vId) tapProfileIds.push(Number(vId));
    if (sId) tapProfileIds.push(Number(sId));
    if (uId) tapProfileIds.push(Number(uId));

    if (tapProfileIds.length === 0) {
      this.commonService.toastError('Please select at least one TAP profile.');
      return;
    }

    const rawForm = this.groupForm.value;
    const payload = {
      name: rawForm.name,
      description: rawForm.description,
      isActive: rawForm.isActive,
      active: rawForm.isActive,
      tapProfileIds: tapProfileIds
    };

    const isEdit = !!this.groupId;
    this.commonService.spinnerShow();
    const request$ = (isEdit && this.groupId)
      ? this.tapService.updateTapProfileGroup(this.groupId, payload)
      : this.tapService.createTapProfileGroup(payload);

    request$
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide()),
        catchError((error) => {
          this.commonService.toastError(
            error?.error?.errorMessage || error?.error?.msg || 'Something went wrong'
          );
          return throwError(() => error);
        })
      )
      .subscribe({
        next: () => {
          this.commonService.toastSuccess(
            `Profile group ${isEdit ? 'updated' : 'created'} successfully`
          );
          this.onClose(true);
        }
      });
  }

  onClose(isReload: boolean = false): void {
    this.groupForm.reset({
      isActive: true,
      voiceProfileId: null,
      smsProfileId: null,
      usageProfileId: null
    });
    this.groupId = null;
    this.router.navigate([this.routes.ratingtapconfiguration], { queryParams: { tab: 'profile-groups' } });
  }
}
