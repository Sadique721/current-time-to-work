import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { CoreComponentComponent } from "./core-component.component";
import { AuthGuard } from "../core.index";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { RoleAuthGuard } from "../core/guard/role-auth.guard";

const routes: Routes = [
  {
    path: "",
    component: CoreComponentComponent,
    children: [

      {
        path: "settings",
        loadChildren: () =>
          import("../components/settings/settings.module").then(
            (m) => m.SettingsModule
          ),
        canActivate: [AuthGuard, RoleAuthGuard],
      },
      {
        path: "rating-engine",
        loadChildren: () =>
          import("./rating-engine/rating-engine.module").then(
            (m) => m.RatingEngineModule
          ),
        canActivate: [AuthGuard, RoleAuthGuard],
      },

      {
        path: "users",
        loadChildren: () =>
          import("./users/users.module").then(
            (m) => m.UsersModule
          ),
        canActivate: [AuthGuard, RoleAuthGuard],
      },

      {
        path: "audit",
        loadChildren: () =>
          import("../components/audit/audit.module").then((m) => m.AuditModule),
        canActivate: [AuthGuard],
      },
     {
        path: "reports",
        loadChildren: () =>
          import("../components/reports/reports.component.module").then((m) => m.ReportsModule),
        canActivate: [AuthGuard],
      }, 

      {
        path: "dashboard",
        component: DashboardComponent,
        canActivate: [AuthGuard],
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class CoreComponentRoutingModule {}
