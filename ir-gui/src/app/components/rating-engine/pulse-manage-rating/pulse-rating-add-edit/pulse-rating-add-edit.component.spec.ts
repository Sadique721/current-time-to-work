import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PulseRatingAddEditComponent } from './pulse-rating-add-edit.component';

describe('PulseRatingAddEditComponent', () => {
  let component: PulseRatingAddEditComponent;
  let fixture: ComponentFixture<PulseRatingAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PulseRatingAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PulseRatingAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
