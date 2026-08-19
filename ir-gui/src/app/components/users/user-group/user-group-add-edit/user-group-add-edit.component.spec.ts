import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGroupAddEditComponent } from './user-group-add-edit.component';

describe('UserGroupAddEditComponent', () => {
  let component: UserGroupAddEditComponent;
  let fixture: ComponentFixture<UserGroupAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [UserGroupAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGroupAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
