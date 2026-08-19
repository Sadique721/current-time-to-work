import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, Validators } from '@angular/forms';
import {
  combineLatest,
  startWith,
  Subscription,
} from 'rxjs';

@Component({
  selector: 'custom-checkbox-group',
  templateUrl: './custom-checkbox-group.component.html',
  styleUrls: ['./custom-checkbox-group.component.scss'],
  standalone: false,
})
export class CustomCheckboxGroupComponent implements OnInit, OnDestroy {
  @Input() label: string = '';
  @Input() options: any[] = [];
  @Input() optionValue: string = 'value';
  @Input() optionLabel: string = 'label';
  @Input() formControlRef: UntypedFormControl = new UntypedFormControl('');
  checkBoxControls: UntypedFormControl[] = [];
  subs: Subscription;

  constructor() {
    this.subs = new Subscription();
  }

  ngOnInit(): void {
    const selected: string[] = this.formControlRef.value || [];
    this.checkBoxControls = [];

    const valueChanges$ = this.options.map((option) => {
      const control = new UntypedFormControl(
        selected.includes(option[this.optionValue])
      );
      this.checkBoxControls.push(control);
      return control.valueChanges.pipe(startWith(control.value));
    });

    const sub = combineLatest(valueChanges$).subscribe((values) => {
      const selectedValues = this.options
        .filter((_, i) => values[i])
        .map((opt) => opt[this.optionValue]);
      this.formControlRef.setValue(selectedValues);
    });
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  isControlRequired = Validators.required;
}
