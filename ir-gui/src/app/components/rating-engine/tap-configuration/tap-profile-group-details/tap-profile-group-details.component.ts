import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { routes, CommonService } from 'src/app/core.index';
import { TapConfigurationService } from '../tap-configuration.service';

@Component({
  selector: 'app-tap-profile-group-details',
  templateUrl: './tap-profile-group-details.component.html',
  standalone: false
})
export class TapProfileGroupDetailsComponent implements OnInit {
  public routes = routes;
  selectedGroup: any = null;

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
    this.tapService.getTapProfileGroupById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.selectedGroup = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.errorMessage || 'Failed to load tap profile group details'
        );
      }
    });
  }
}
