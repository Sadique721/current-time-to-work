import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CountryManageComponent } from './country-manage.component';

describe('CountryManageComponent', () => {
  let component: CountryManageComponent;
  let fixture: ComponentFixture<CountryManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CountryManageComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CountryManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
