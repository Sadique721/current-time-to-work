import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PulseManageRatingComponent } from './pulse-manage-rating.component';

describe('PulseManageRatingComponent', () => {
  let component: PulseManageRatingComponent;
  let fixture: ComponentFixture<PulseManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PulseManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PulseManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
