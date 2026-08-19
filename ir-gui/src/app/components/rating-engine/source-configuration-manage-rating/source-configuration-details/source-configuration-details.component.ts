import { Component, OnInit, OnDestroy } from "@angular/core";
import { Subject, takeUntil } from "rxjs";
import { Isourceconfig, ISourceCdrConfig } from "../source-configuration-manage-rating.interface";
import { SourceConfigurationManageService } from "../source-configuration-manage-rating.service";
import { CommonService, SidebarService, routes } from "src/app/core.index";
import { ActivatedRoute, Router } from "@angular/router";

@Component({
  selector: "app-source-configuration-details",
  templateUrl: "./source-configuration-details.component.html",
  styleUrl: "./source-configuration-details.component.scss",
  standalone: false,
})
export class SourceConfigurationDetailsComponent implements OnInit, OnDestroy {
  public routes = routes;
  selectedSource: Isourceconfig | null = null;

  sourceCdrListData: ISourceCdrConfig[] = [];
  isCollapsed = false;
  cols: any[] = [];
  private destroy$ = new Subject<void>();

  constructor(
    private sourceConfigurationManageService: SourceConfigurationManageService,
    private commonService: CommonService,
    private sidebar: SidebarService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeColumns();
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid source id");
      return;
    }
    this.fetchSourceAndCdr(id);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeColumns(): void {
    this.cols = [
      { field: 'fieldName', header: 'Field Name' },
      { field: 'sequence', header: 'Sequence' },
    ];
  }

  private fetchSourceAndCdr(sourceId: number): void {
    this.commonService.spinnerShow();
    
    this.sourceConfigurationManageService.getById(sourceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (src: any) => {
          this.selectedSource = src as Isourceconfig;
          this.loadSourceCdrData(sourceId);
        },
        (err: any) => {
          this.commonService.spinnerHide();
          this.commonService.toastError(err?.error?.msg || 'Failed to load source details');
        }
      );
  }

  private loadSourceCdrData(sourceId: number): void {
    this.sourceConfigurationManageService
      .getSourceCdrBySource(sourceId)
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        (data: any) => {
          this.commonService.spinnerHide();
          this.sourceCdrListData = data || [];
          
          if (this.sourceCdrListData.length === 0) {
            this.commonService.toastInfo('No source CDR records found.');
          }
        },
        (error: any) => {
          this.commonService.spinnerHide();
                    this.commonService.toastError(
            error?.error?.errorMessage || 'Error fetching source CDR data'
          );
        }
      );
  }

  onRowReorder(event: any): void {
    this.commonService.toastInfo(`Row moved from index ${event.dragIndex} to ${event.dropIndex}`);

    
    this.sourceCdrListData.forEach((item, index) => {
      item.sequence = index + 1;
    });
  }

  updateSourceCdr(): void {
    if (!this.sourceCdrListData || this.sourceCdrListData.length === 0) {
      this.commonService.toastWarn('No CDR data to update');
      return;
    }

    if (!this.selectedSource?.sourceId) {
      this.commonService.toastError('Source ID not found');
      return;
    }

    const payload = this.sourceCdrListData.map(item => ({
      id: item.id,
      sequence: item.sequence
    }));

    this.commonService.spinnerShow();
    this.sourceConfigurationManageService
      .updateSourceCdrSequences(this.selectedSource.sourceId, payload)
      .pipe(takeUntil(this.destroy$))
      .subscribe(
        () => {
          this.commonService.spinnerHide();
          this.commonService.toastSuccess('Source CDR updated successfully.');
          if (this.selectedSource?.sourceId) {
            this.loadSourceCdrData(this.selectedSource.sourceId); 
          }
        },
        (error: any) => {
          this.commonService.spinnerHide();
                    this.commonService.toastError(
            error?.error?.errorMessage || 'Error updating source CDR'
          );
        }
      );
  }

  goBackToList(): void {
    this.router.navigate([routes.ratingsourceconfiguration]);
  }

  toggleCollapse(): void {
    this.sidebar.toggleCollapse();
    this.isCollapsed = !this.isCollapsed;
  }
}
