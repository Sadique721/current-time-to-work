import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TelecomCircleComponent } from './telecom-circle.component';

describe('TelecomCircleComponent', () => {
  let component: TelecomCircleComponent;
  let fixture: ComponentFixture<TelecomCircleComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [TelecomCircleComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TelecomCircleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
