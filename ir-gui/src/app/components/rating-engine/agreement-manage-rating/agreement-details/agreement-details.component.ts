import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { routes, CommonService } from "src/app/core.index";
import { AgreementManageService } from "../agreement-manage-rating.service";

@Component({
  selector: "app-agreement-details",
  templateUrl: "./agreement-details.component.html",
  styleUrl: "./agreement-details.component.scss",
  standalone: false,
})
export class AgreementDetailsComponent implements OnInit {
  public routes = routes;
  selectedAgreement: any = {};

  constructor(
    private route: ActivatedRoute,
    private commonService: CommonService,
    private agreementManageService: AgreementManageService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get("id");
    const id = idParam ? Number(idParam) : null;
    if (!id) {
      this.commonService.toastError("Invalid agreement id");
      return;
    }
    this.fetchDetails(id);
  }

  private fetchDetails(id: number): void {
    this.commonService.spinnerShow();
    this.agreementManageService.getById(id).subscribe({
      next: (res: any) => {
        this.commonService.spinnerHide();
        this.selectedAgreement = res || {};
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.msg || "Failed to load agreement details"
        );
      },
    });
  }
}
