import { NgModule } from "@angular/core";
import { CommonModule } from "@angular/common";
import { RouterModule } from "@angular/router";
import { DatePickerModule } from "primeng/datepicker";
import { TableModule } from "primeng/table";
import { sharedModule } from "src/app/core.index";
import { CustomElementModule } from "src/app/core/shared/custom-elements/custom-elemets.module";
import { SelectModule } from "primeng/select";
import { ReactiveFormsModule } from "@angular/forms";
import { InputTextModule } from "primeng/inputtext";
import { ReportsComponent } from "./reports.component";
import { ReportsRoutingModule } from "./reports-routing.module";
import { CdrsComponent } from './cdrs/cdrs.component';


@NgModule({
  declarations: [
    ReportsComponent,
    CdrsComponent,

  ],
  imports: [
    CommonModule,
    ReportsRoutingModule,
    sharedModule,
    CustomElementModule,
    RouterModule,
    DatePickerModule,
    TableModule,
    SelectModule,
    CommonModule,
    ReactiveFormsModule,
    InputTextModule,
  ],
})
export class ReportsModule {}
