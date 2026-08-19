import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "src/app/core.index";
import { RoleAuthGuard } from "src/app/core/guard/role-auth.guard";
import { ReportsComponent } from "./reports.component";
import { CdrsComponent } from "./cdrs/cdrs.component";


const routes: Routes = [
  {
    path: "",
    component: ReportsComponent,
    children: [
      {
        path: "",
        redirectTo: "country",
        pathMatch: "full",
      },
      {
          path: "cdrs",
          component: CdrsComponent,
          canActivate: [AuthGuard, RoleAuthGuard],
      },

    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class ReportsRoutingModule {}
