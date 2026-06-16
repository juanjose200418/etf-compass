import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { AppComponent } from './app.component';
import { AuthService } from './auth.service';
import { EtfService } from './etf.service';
import { PortfolioApiService } from './portfolio-api.service';

class AuthServiceStub {
  readonly user = signal(null);
  readonly isAuthenticated = signal(false);
  logout(): void {}
}

class EtfServiceStub {
  readonly selectedETFs = signal([]);
  readonly favorites = signal<string[]>([]);
  readonly isAddingETF = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);
  readonly searchQuery = signal('');
  readonly searchResults = signal<Array<{ ticker: string; name: string }>>([]);
  isMaxReached(): boolean { return false; }
  setSearchQuery(): void {}
  clearSearchResults(): void {}
  selectETF(): void {}
  removeETF(): void {}
  clearSelection(): void {}
  toggleFavorite(): void {}
  isFavorite(): boolean { return false; }
  isTickerPending(): boolean { return false; }
  getETFColor(): string { return '#2563eb'; }
  formatText(value: string | null | undefined): string { return value && value.trim() ? value : 'No disponible'; }
  formatTER(value: number | null): string { return value == null ? 'No disponible' : `${value.toFixed(2)}%`; }
  formatRisk(value: number | null): string { return value == null ? 'No disponible' : String(value); }
  formatPrice(value: number | null): string { return value == null ? 'No disponible' : value.toFixed(2); }
  formatDayChange(): string { return 'No disponible'; }
  formatRange(): string { return 'No disponible'; }
  formatFundSize(): string { return 'No disponible'; }
  formatPerformance(): string { return 'No disponible'; }
}

class PortfolioApiServiceStub {}

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        { provide: AuthService, useClass: AuthServiceStub },
        { provide: EtfService, useClass: EtfServiceStub },
        { provide: PortfolioApiService, useClass: PortfolioApiServiceStub }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the current header title', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('ETF Compass');
  });

  it('should show the login screen when the user is signed out', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Inicia sesion para desbloquear dashboard financiero');
  });
});
