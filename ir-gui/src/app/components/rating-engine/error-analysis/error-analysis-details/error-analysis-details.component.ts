import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ErrorConfigCheckStatusDTO } from '../error-analysis.service';

@Component({
  selector: 'app-error-analysis-details',
  templateUrl: './error-analysis-details.component.html',
  styleUrls: [],
  standalone: false
})
export class ErrorAnalysisDetailsComponent implements OnInit {
  recordData: ErrorConfigCheckStatusDTO | null = null;

  constructor(private router: Router) {
    const navigation = this.router.getCurrentNavigation();
    if (navigation?.extras?.state?.['data']) {
      this.recordData = navigation.extras.state['data'];
    } else {
      this.recordData = history.state.data || null;
    }
  }

  ngOnInit(): void {
    if (!this.recordData) {
      this.router.navigate(['/rating-engine/error-analysis']);
    }
  }
  
  goBack(): void {
    this.router.navigate(['/rating-engine/error-analysis']);
  }
}
