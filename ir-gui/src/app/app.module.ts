import { NgModule, OnInit } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { ToastModule } from 'primeng/toast';
import { ConfirmationService, MessageService } from 'primeng/api';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { AuthInterceptor } from './core/interceptor/auth.interceptor';
import { LoaderComponent } from './core/common-component/loader/loader.component';
import { sharedModule } from './core.index';
import { CommonModule } from '@angular/common';
import { Route, RouterModule } from '@angular/router';
import { Error404Component } from './core/error-pages/error-404/error-404.component';
import { Error500Component } from './core/error-pages/error-500/error-500.component';

@NgModule({
  declarations: [
    AppComponent,
    LoaderComponent,
    Error404Component,
    Error500Component,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    sharedModule,
    BrowserAnimationsModule,
    ToastModule,
    ConfirmDialogModule,
    RouterModule,
  ],
  providers: [
    ConfirmationService,
    MessageService,
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    provideHttpClient(withInterceptorsFromDi()),
  ],
  exports: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
