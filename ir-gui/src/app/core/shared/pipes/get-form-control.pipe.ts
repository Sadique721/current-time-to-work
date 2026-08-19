import { Pipe, PipeTransform } from '@angular/core';
import { FormGroup, UntypedFormControl } from '@angular/forms';

@Pipe({
  name: 'getControl',
  standalone: false,
})
export class GetFormControlPipe implements PipeTransform {
  transform(formGroup: FormGroup, controlName: string): UntypedFormControl {
    return formGroup?.get(controlName) as UntypedFormControl;
  }
}
