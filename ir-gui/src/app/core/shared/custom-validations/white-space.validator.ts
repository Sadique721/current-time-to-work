import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

export class WhiteeSpaceValidator {
  static cannotContainSpace(control: AbstractControl): ValidationErrors | null {
    if (control.value != null) {
      if (control.value.endsWith(" ") || control.value.startsWith(" ")) {
        return { cannotContainSpace: true };
      }
    }

    return null;
  }
}

export class CustomValidators {
  static max(maxValue: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (control.value == null || control.value === "") return null;

      const value = Number(control.value);
      if (isNaN(value)) return null;

      return value > maxValue
        ? { max: { requiredMax: maxValue, actual: value } }
        : null;
    };
  }

  static min(minValue: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (control.value == null || control.value === "") return null;

      const value = Number(control.value);
      if (isNaN(value)) return null;

      return value < minValue
        ? { min: { requiredMin: minValue, actual: value } }
        : null;
    };
  }
}
