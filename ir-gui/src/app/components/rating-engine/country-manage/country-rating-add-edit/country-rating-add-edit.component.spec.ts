import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CountryRatingAddEditComponent } from './country-rating-add-edit.component';

describe('CountryRatingAddEditComponent', () => {
  let component: CountryRatingAddEditComponent;
  let fixture: ComponentFixture<CountryRatingAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CountryRatingAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CountryRatingAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
