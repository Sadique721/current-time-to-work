import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountRatingAddEditComponent } from './account-rating-add-edit.component';

describe('AccountRatingAddEditComponent', () => {
  let component: AccountRatingAddEditComponent;
  let fixture: ComponentFixture<AccountRatingAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountRatingAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AccountRatingAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
