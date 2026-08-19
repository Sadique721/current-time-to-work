import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuditComponent } from './audit.component';
import { ReportedProblemComponent } from './reported-problem/reported-problem.component';
import { AuditLogComponent } from './audit-log/audit-log.component';

const routes: Routes = [
  {
    path: '',
    component: AuditComponent,
    children: [
      {
        path: 'reported-problem',
        component: ReportedProblemComponent,
      },
      {
        path: 'audit-log',
        component: AuditLogComponent,
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AuditRoutingModule {}
