import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AuthRoutingModule } from './auth-routing.module';
import { AuthComponent } from './auth.component';
import { InputOtpModule } from 'primeng/inputotp';
import { SigninComponent } from './signin/signin.component';
import { sharedModule } from 'src/app/core.index';

@NgModule({
  declarations: [AuthComponent, SigninComponent],
  imports: [CommonModule, AuthRoutingModule, sharedModule, InputOtpModule],
})
export class AuthModule {}
