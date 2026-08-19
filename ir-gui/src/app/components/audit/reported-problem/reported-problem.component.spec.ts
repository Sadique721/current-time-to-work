import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportedProblemComponent } from './reported-problem.component';

describe('ReportedProblemComponent', () => {
  let component: ReportedProblemComponent;
  let fixture: ComponentFixture<ReportedProblemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportedProblemComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReportedProblemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
