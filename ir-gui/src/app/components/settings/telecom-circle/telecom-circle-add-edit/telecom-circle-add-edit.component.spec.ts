import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TelecomCircleAddEditComponent } from './telecom-circle-add-edit.component';

describe('TelecomCircleAddEditComponent', () => {
  let component: TelecomCircleAddEditComponent;
  let fixture: ComponentFixture<TelecomCircleAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TelecomCircleAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TelecomCircleAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
