import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrefixRatingAddEditComponent } from './prefix-rating-add-edit.component';

describe('PrefixRatingAddEditComponent', () => {
  let component: PrefixRatingAddEditComponent;
  let fixture: ComponentFixture<PrefixRatingAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrefixRatingAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PrefixRatingAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
