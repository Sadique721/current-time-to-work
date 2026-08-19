import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountManageRatingComponent } from './account-manage-rating.component';

describe('AccountManageRatingComponent', () => {
  let component: AccountManageRatingComponent;
  let fixture: ComponentFixture<AccountManageRatingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountManageRatingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AccountManageRatingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
