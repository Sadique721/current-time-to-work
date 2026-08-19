import { DragDropModule } from '@angular/cdk/drag-drop';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FullCalendarModule } from '@fullcalendar/angular';
import { LightgalleryModule } from 'lightgallery/angular';
import { NgApexchartsModule } from 'ng-apexcharts';
import { PopoverModule } from 'ngx-bootstrap/popover';
import { TimepickerModule } from 'ngx-bootstrap/timepicker';
import { CountUpModule } from 'ngx-countup';
import { NgxDropzoneModule } from 'ngx-dropzone';
import { NgxEditorModule } from 'ngx-editor';
import { CarouselModule } from 'ngx-owl-carousel-o';
import { CalendarModule } from 'primeng/calendar';
import { CommonModule } from '@angular/common';
import { CustomInputComponent } from './custom-input/custom-input.component';
import { CustomSelectComponent } from './custom-select/custom-select.component';
import { CustomRadioComponent } from './custom-radio/custom-radio.component';
import { CustomCheckboxComponent } from './custom-checkbox/custom-checkbox.component';
import { CustomCheckboxGroupComponent } from './custom-checkbox-group/custom-checkbox-group.component';
import { SelectModule } from 'primeng/select';
import { MultiSelectModule } from 'primeng/multiselect';
import { MaterialModule } from '../material/material.module';
import { CustomPaginationModule } from '../custom-pagination/custom-pagination.module';
import { NgxbootstrapModule } from '../ngx-bootstrap/ngxbootstrap.module';
import { CustomDateComponent } from './custom-date/custom-date.component';
import { DatePickerModule } from 'primeng/datepicker';
import { TextareaModule } from 'primeng/textarea';
import { CustomTextareaComponent } from './custom-textarea/custom-textarea.component';
import { CustomNumberInputComponent } from './custom-number-input/custom-number-input/custom-number-input.component';
import { InputNumberModule } from 'primeng/inputnumber';
@NgModule({
  declarations: [
    CustomInputComponent,
    CustomSelectComponent,
    CustomRadioComponent,
    CustomCheckboxComponent,
    CustomCheckboxGroupComponent,
    CustomDateComponent,
    CustomTextareaComponent,
    CustomNumberInputComponent,
  ],
  imports: [
    TextareaModule,
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
    NgxDropzoneModule,
    MatStepperModule,
    MatFormFieldModule,
    MatChipsModule,
    MatAutocompleteModule,
    ReactiveFormsModule,
    LightgalleryModule,
    CountUpModule,
    TimepickerModule,
    CommonModule,
    SelectModule,
    MultiSelectModule,
    DatePickerModule,
    InputNumberModule,
  ],
  exports: [
    CustomTextareaComponent,
    CustomInputComponent,
    CustomSelectComponent,
    CustomRadioComponent,
    CustomCheckboxComponent,
    CustomCheckboxGroupComponent,
    CustomDateComponent,
    CustomNumberInputComponent,
  ],
})
export class CustomElementModule {}
