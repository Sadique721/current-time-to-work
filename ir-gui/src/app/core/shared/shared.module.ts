import { NgModule } from '@angular/core';
import { NgApexchartsModule } from 'ng-apexcharts';
import { MaterialModule } from './material/material.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CarouselModule } from 'ngx-owl-carousel-o';
import { CustomPaginationModule } from './custom-pagination/custom-pagination.module';
import { FullCalendarModule } from '@fullcalendar/angular';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgxEditorModule } from 'ngx-editor';
import { PopoverModule } from 'ngx-bootstrap/popover';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  BsDatepickerModule,
  BsDatepickerConfig,
} from 'ngx-bootstrap/datepicker';
import { NgxMaskModule } from 'ngx-mask';
import { NgxDropzoneModule } from 'ngx-dropzone';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { LightgalleryModule } from 'lightgallery/angular';
import { CountUpModule } from 'ngx-countup';
import { TimepickerActions, TimepickerModule } from 'ngx-bootstrap/timepicker';
import { CommonModule, DatePipe } from '@angular/common';
import { FeatherModule } from 'angular-feather';
import { Tool } from 'angular-feather/icons';
import { CalendarModule } from 'primeng/calendar';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeng/themes/aura';
import { GetFormControlPipe } from './pipes/get-form-control.pipe';
import { NgxbootstrapModule } from './ngx-bootstrap/ngxbootstrap.module';

const icons = {
  Tool,
};
@NgModule({
  declarations: [GetFormControlPipe],
  exports: [
    MaterialModule,
    NgApexchartsModule,
    FormsModule,
    CarouselModule,
    CustomPaginationModule,
    DragDropModule,
    FullCalendarModule,
    CalendarModule,
    NgxbootstrapModule,
    NgxMaskModule,
    NgxEditorModule,
    PopoverModule,
    MatTooltipModule,
    BsDatepickerModule,
    NgxDropzoneModule,
    MatStepperModule,
    MatFormFieldModule,
    MatChipsModule,
    MatAutocompleteModule,
    ReactiveFormsModule,
    LightgalleryModule,
    CountUpModule,
    TimepickerModule,
    FeatherModule,
    GetFormControlPipe,
  ],
  imports: [
    CommonModule,
    MaterialModule,
    NgApexchartsModule,
    FormsModule,
    CarouselModule,
    CustomPaginationModule,
    DragDropModule,
    FullCalendarModule,
    CalendarModule,
    NgxbootstrapModule,
    NgxEditorModule,
    PopoverModule,
    MatTooltipModule,
    BsDatepickerModule.forRoot(),
    NgxMaskModule.forRoot({
      showMaskTyped: false,
    }),
    NgxDropzoneModule,
    MatStepperModule,
    MatFormFieldModule,
    MatChipsModule,
    MatAutocompleteModule,
    ReactiveFormsModule,
    LightgalleryModule,
    CountUpModule,
    TimepickerModule,
    FeatherModule.pick(icons),
  ],
  providers: [
    providePrimeNG({
      theme: {
        preset: Aura,
      },
    }),
    DatePipe,
    TimepickerActions,
    BsDatepickerConfig,
  ],
})
export class sharedModule {}
