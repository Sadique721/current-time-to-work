import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core.index';
import { Error404Component } from './core/error-pages/error-404/error-404.component';
import { Error500Component } from './core/error-pages/error-500/error-500.component';
import { RoleAuthGuard } from './core/guard/role-auth.guard';

const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'signin',
  },
  {
    path: '',
    loadChildren: () =>
      import('./components/core-component.module').then(
        (m) => m.CoreComponentModule,
      ),
    canActivate: [AuthGuard, RoleAuthGuard],
  },
  {
    path: '',
    loadChildren: () =>
      import('./components/auth/auth.module').then((m) => m.AuthModule),
  },
  {
    path: 'error-pages',
    component: Error500Component,
  },
  {
    path: 'error-404',
    component: Error404Component,
  },
  {
    path: '**',
    redirectTo: '/error-404',
    pathMatch: 'full',
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
