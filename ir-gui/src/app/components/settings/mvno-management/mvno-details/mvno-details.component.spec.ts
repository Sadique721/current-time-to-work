import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MvnoDetailsComponent } from './mvno-details.component';

describe('MvnoDetailsComponent', () => {
  let component: MvnoDetailsComponent;
  let fixture: ComponentFixture<MvnoDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MvnoDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MvnoDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
