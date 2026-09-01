import { Component, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { routes, CommonService } from "src/app/core.index";
import { AgreementManageService } from "../agreement-manage-rating.service";
import dayjs from "dayjs";

@Component({
  selector: "app-agreement-details",
  templateUrl: "./agreement-details.component.html",
  styleUrl: "./agreement-details.component.scss",
  standalone: false,
})
export class AgreementDetailsComponent implements OnInit {
  public routes = routes;
  selectedAgreement: any = {};
  previewMessage: string = '';

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
        this.generatePreviewMessage();
      },
      error: (err: any) => {
        this.commonService.spinnerHide();
        this.commonService.toastError(
          err?.error?.msg || "Failed to load agreement details"
        );
      },
    });
  }

  private generatePreviewMessage(): void {
    const type = this.selectedAgreement.billingType || 'DAYS';
    const startDateVal = this.selectedAgreement.billingCycleStartDate;
    
    if (!type || !startDateVal) {
      this.previewMessage = '';
      return;
    }

    const start = dayjs(startDateVal);
    let end: dayjs.Dayjs;
    let nextInvoice: dayjs.Dayjs;

    if (type === 'DAYS') {
      const period = Number(this.selectedAgreement.billingCyclePeriod);
      if (!period || period <= 0) {
        this.previewMessage = '';
        return;
      }
      end = start.add(period - 1, 'day');
      nextInvoice = end.add(1, 'day');
    } else if (type === 'WEEKLY') {
      end = start.add(6, 'day');
      nextInvoice = end.add(1, 'day');
    } else if (type === 'FORTNIGHTLY') {
      end = start.add(14, 'day');
      nextInvoice = end.add(1, 'day');
    } else if (type === 'MONTHLY') {
      end = start.add(1, 'month').subtract(1, 'day');
      nextInvoice = end.add(1, 'day');
    } else {
      this.previewMessage = '';
      return;
    }

    this.previewMessage = `First Cycle: ${start.format('YYYY-MM-DD')} to ${end.format('YYYY-MM-DD')}. Next invoice will generate on ${nextInvoice.format('YYYY-MM-DD')}.`;
  }
}
