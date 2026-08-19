import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { CommonService } from 'src/app/core.index';
import { ReRateRequestDTO, ReRateRequestService } from '../rerate-request.service';
import { routes } from 'src/app/core/helpers/routes';

@Component({
  selector: 'app-rerate-request-details',
  templateUrl: './rerate-request-details.component.html',
  standalone: false,
  styleUrls: []
})
export class ReRateRequestDetailsComponent implements OnInit, OnDestroy {
  public routes = routes;
  requestId: number | null = null;
  selectedRequest: ReRateRequestDTO | null = null;
  lineOfBusiness: string = '';
  parameters: Array<{ parameterField: string; parameterValue: string }> = [];
  
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private svc: ReRateRequestService,
    private commonService: CommonService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.requestId = +id;
        this.loadRequest(this.requestId);
      } else {
        this.router.navigate(['/rating-engine/rerate-requests']);
      }
    });
  }

  private loadRequest(id: number): void {
    this.commonService.spinnerShow();
    this.svc.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (request: ReRateRequestDTO) => {
        this.commonService.spinnerHide();
        if (request) {
          this.selectedRequest = request;
          this.parameters = [];
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
                   this.lineOfBusiness = parts[1];
                } else {
                   this.parameters.push({ parameterField: field, parameterValue: parts[1] });
                }
              }
            });
          }
        }
      },
      error: () => {
        this.commonService.spinnerHide();
        this.commonService.toastError('Failed to load request details');
        this.router.navigate(['/rating-engine/rerate-requests']);
      }
    });
  }

  getStatusClass(status: string | undefined): string {
    const map: any = { NEW: 'badge badge-primary', COMPLETED: 'badge badge-success', FAILED: 'badge badge-danger', IN_PROGRESS: 'badge badge-warning', PENDING: 'badge badge-info' };
    return map[status?.toUpperCase() ?? ''] || 'badge badge-secondary';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
