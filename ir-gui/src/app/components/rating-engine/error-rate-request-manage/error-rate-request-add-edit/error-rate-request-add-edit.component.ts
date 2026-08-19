import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { CommonService, SidebarService } from 'src/app/core.index';
import { ErrorRateRequestDTO, ErrorRateRequestService, CdrQueryConfigDTO } from '../error-rate-request.service';
import { routes } from 'src/app/core/helpers/routes';

@Component({
  selector: 'app-error-rate-request-add-edit',
  templateUrl: './error-rate-request-add-edit.component.html',
  standalone: false,
  styleUrls: []
})
export class ErrorRateRequestAddEditComponent implements OnInit, OnDestroy {
  form!: FormGroup;
  parameters: Array<{ parameterField: string; parameterValue: string }> = [];
  submitted = false;

  commonFields = [
    'source_id', 'incoming_account_id', 'outgoing_account_id', 'zone_name',
    'incoming_rating_status', 'outgoing_rating_status', 
    'incoming_rate_package_id', 'outgoing_rate_package_id',
    'imsi', 'msisdn', 'imei', 'home_plmn', 'visited_plmn'
  ];

  voiceFields = [...this.commonFields, 'calling_number', 'called_number', 'call_type', 'duration_seconds'];
  smsFields = [...this.commonFields, 'calling_number', 'called_number', 'call_type', 'event_nos'];
  usageFields = [...this.commonFields, 'subscriber_identity', 'access_point_name', 'total_usage', 'measurement_unit'];

  getFieldsForService(service: string): string[] {
    switch (service) {
      case 'VOICE': return this.voiceFields;
      case 'SMS': return this.smsFields;
      case 'USAGE': return this.usageFields;
      case 'ALL':
      default:
        return this.commonFields;
    }
  }
  
  requestId: number | null = null;
  isEditMode = false;
  isCollapsed = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private svc: ErrorRateRequestService,
    private commonService: CommonService,
    private route: ActivatedRoute,
    private router: Router,
    private sidebar: SidebarService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      requestName: ['', Validators.required],
      serviceType: ['VOICE', Validators.required],
      queryConfigId: [null],
      lineOfBusiness: ['', Validators.required],
      queryIsActive: [true],
      startDate: [null],

      endDate: [null],
      enable: [true]
    });

    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.requestId = +id;
        this.isEditMode = true;
        this.loadRequest(this.requestId);
      }
    });
  }

  private loadRequest(id: number): void {
    this.commonService.spinnerShow();
    this.svc.fetchPage(0, 1000).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        const request = res.content?.find((x: any) => x.id === id);
        if (request) {
          let serviceType = 'VOICE';
          let lineOfBusiness = '';
          let queryIsActive = true;
          let queryConfigId = null;

          if (request.usageQueryConfig) {
            serviceType = 'USAGE';
            queryIsActive = request.usageQueryConfig.isActive !== false;
            queryConfigId = request.usageQueryConfig.id;
          } else if (request.smsQueryConfig) {
            serviceType = 'SMS';
            queryIsActive = request.smsQueryConfig.isActive !== false;
            queryConfigId = request.smsQueryConfig.id;
          } else if (request.voiceQueryConfig) {
            serviceType = 'VOICE';
            queryIsActive = request.voiceQueryConfig.isActive !== false;
            queryConfigId = request.voiceQueryConfig.id;
          }

          if (request.requestParameters) {
            request.requestParameters.split(',').forEach((p: string) => {
              const parts = p.split('=');
              if (parts.length === 2) {
                let field = parts[0];
                if (field.includes(':')) {
                   const typeAndField = field.split(':');
                   field = typeAndField[1];
                }
                if (field === 'line_of_business') {
                  lineOfBusiness = parts[1];
                } else {
                  this.parameters.push({ parameterField: field, parameterValue: parts[1] });
                }
              }
            });
          }

          this.form.patchValue({
            requestName: request.requestName,
            serviceType: serviceType,
            queryConfigId: queryConfigId,
            lineOfBusiness: lineOfBusiness,
            queryIsActive: queryIsActive,
            startDate: request.startDate ? new Date(request.startDate as string) : null,
            endDate: request.endDate ? new Date(request.endDate as string) : null,
            enable: request.enable
          });
        }
      },
      error: () => {
        this.commonService.spinnerHide();
        this.commonService.toastError('Failed to load request details');
        this.onClose();
      }
    });
  }

  addEmptyParameter(): void {
    this.parameters.push({ parameterField: '', parameterValue: '' });
  }

  removeParameter(index: number): void {
    this.parameters.splice(index, 1);
  }

  submit(): void {
    this.submitted = true;
    const v = this.form.value;
    const hasEmptyParams = this.parameters.some(p => !p.parameterField?.trim() || !p.parameterValue?.trim());

    if (this.form.invalid || this.parameters.length === 0 || hasEmptyParams) {
      this.form.markAllAsTouched();
      if (hasEmptyParams) {
        this.commonService.toastError('Please fill out all parameter fields or remove empty rows.');
      }
      return;
    }

    if (!v.lineOfBusiness) {
       this.commonService.toastError('Please select Line Of Business.');
       return;
    }

    const formatDate = (d: any) => {
      if (!d) return undefined;
      const date = new Date(d);
      if (isNaN(date.getTime())) return undefined;
      const pad = (n: number) => n.toString().padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`;
    };

    const createConfigDto = (type: string, id: any, active: boolean) => {
      return { id, queryName: null, serviceType: type, isActive: active };
    };

    const payload: ErrorRateRequestDTO = {
      ...v,
      voiceQueryConfig: v.serviceType === 'VOICE' ? createConfigDto('VOICE', v.queryConfigId, v.queryIsActive) : null,
      smsQueryConfig: v.serviceType === 'SMS' ? createConfigDto('SMS', v.queryConfigId, v.queryIsActive) : null,
      usageQueryConfig: v.serviceType === 'USAGE' ? createConfigDto('USAGE', v.queryConfigId, v.queryIsActive) : null,
      startDate: formatDate(v.startDate),
      endDate: formatDate(v.endDate),
      requestParameters: `${v.serviceType}:line_of_business=${v.lineOfBusiness}` + (this.parameters.length > 0 ? ',' + this.parameters.map(p => `${v.serviceType}:${p.parameterField}=${p.parameterValue}`).join(',') : '')
    };

    const call = this.isEditMode && this.requestId
      ? this.svc.update(this.requestId, payload)
      : this.svc.create(payload);

    this.commonService.spinnerShow();
    call.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.commonService.spinnerHide();
        this.commonService.toastSuccess(this.isEditMode ? 'Updated successfully' : 'Created successfully');
        this.onClose();
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(err?.error?.message || (this.isEditMode ? 'Update failed' : 'Create failed'));
      }
    });
  }

  onClose(): void {
    this.router.navigate([routes.errorRateRequests]);
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
