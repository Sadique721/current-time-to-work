import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { routes, CommonService } from 'src/app/core.index';
import { TapConfigurationService } from '../tap-configuration.service';

@Component({
  selector: 'app-tap-field-details',
  templateUrl: './tap-field-details.component.html',
  standalone: false
})
export class TapFieldDetailsComponent implements OnInit {
  public routes = routes;
  selectedField: any = null;

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private tapService: TapConfigurationService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    if (id) {
      this.fetchDetails(id);
    }
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.tapService.getTapFieldById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.selectedField = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.errorMessage || 'Failed to load tap field details'
        );
      }
    });
  }
}
