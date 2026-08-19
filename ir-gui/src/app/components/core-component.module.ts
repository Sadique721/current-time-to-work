import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CoreComponentRoutingModule } from './core-component-routing.module';
import { CoreComponentComponent } from './core-component.component';
import { HeaderComponent } from '../core/common-component/header/header.component';
import { RouterModule } from '@angular/router';
import { FooterComponent } from '../core/common-component/footer/footer.component';
import { LayoutComponent } from '../core/common-component/layout/layout.component';
import { SidebarOneComponent } from '../core/common-component/sidebar-one/sidebar-one.component';
import { SidebarThreeComponent } from '../core/common-component/sidebar-three/sidebar-three.component';
import { sharedModule } from '../core/shared/shared.module';
import { DashboardComponent } from './dashboard/dashboard.component';

@NgModule({
  declarations: [
    CoreComponentComponent,
    HeaderComponent,
    SidebarOneComponent,
    SidebarThreeComponent,
    FooterComponent,
    LayoutComponent,
    DashboardComponent,
  ],
  imports: [
    CommonModule,
    CoreComponentRoutingModule,
    sharedModule,
    RouterModule,
  ],
  providers: [],
})
export class CoreComponentModule {}
