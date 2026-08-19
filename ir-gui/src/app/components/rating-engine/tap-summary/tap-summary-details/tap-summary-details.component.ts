import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil, finalize } from 'rxjs/operators';
import { CommonService } from 'src/app/core.index';
import { TapSummaryService } from '../tap-summary.service';

@Component({
  selector: 'app-tap-summary-details',
  templateUrl: './tap-summary-details.component.html',
  styleUrls: ['./tap-summary-details.component.scss'],
  standalone: false
})
export class TapSummaryDetailsComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  tapFileId: number | null = null;
  serviceType: string = 'SMS';
  selectedCdrDetails: any = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private commonService: CommonService,
    private tapSummaryService: TapSummaryService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe(params => {
      const id = params.get('tapFileId');
      if (id) {
        this.tapFileId = +id;
        this.serviceType = this.route.snapshot.queryParamMap.get('serviceType') || 'SMS';
        this.fetchDetails();
      } else {
        this.commonService.toastError('Invalid TAP File ID');
        this.goBack();
      }
    });
  }

  fetchDetails(): void {
    if (!this.tapFileId) return;

    this.commonService.spinnerShow();
    this.tapSummaryService
      .getTapSummaryCdrs(this.tapFileId, this.serviceType)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.commonService.spinnerHide())
      )
      .subscribe({
        next: (res: any) => {
          this.selectedCdrDetails = res || {};
        },
        error: (err: any) => {
          this.commonService.toastError(
            err?.error?.msg || 'Failed to fetch CDR details'
          );
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/rating-engine/tap-summary']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
