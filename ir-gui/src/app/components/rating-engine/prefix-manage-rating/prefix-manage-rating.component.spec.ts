import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefixManageRatingComponent } from './prefix-manage-rating.component';

describe('PrefixManageRatingComponent', () => {
  let component: PrefixManageRatingComponent;
  let fixture: ComponentFixture<PrefixManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrefixManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrefixManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
