import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { routes, CommonService } from "src/app/core.index";
import { TaxManageService } from "../tax-manage-rating.service";
import { ITaxConfig } from "../tax-manage-rating.interface";

@Component({
  selector: "app-tax-details",
  templateUrl: "./tax-details.component.html",
  standalone: false,
})
export class TaxDetailsComponent implements OnInit {
  public routes = routes;
  selectedTax: any = null;

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private taxManageService: TaxManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (id) {
      this.fetchDetails(id);
    }
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.taxManageService.getById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.selectedTax = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.errorMessage || "Failed to load tax details"
        );
      },
    });
  }
}
