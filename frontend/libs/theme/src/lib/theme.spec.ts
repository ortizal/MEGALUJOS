import { TestBed } from '@angular/core/testing';
import { MEGA_TOKENS, MEGA_THEME_STORAGE_KEY } from './mega-tokens';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;

  beforeEach(() => {
    localStorage.removeItem(MEGA_THEME_STORAGE_KEY);
    TestBed.configureTestingModule({});
    service = TestBed.inject(ThemeService);
  });

  it('debe crear la instancia con un modo válido', () => {
    expect(service).toBeTruthy();
    expect(['light', 'dark']).toContain(service.mode());
  });

  it('aplica data-theme sobre <html> al cambiar el modo', () => {
    service.setMode('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem(MEGA_THEME_STORAGE_KEY)).toBe('dark');

    service.setMode('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('expone reducedMotion como signal booleano', () => {
    expect(typeof service.reducedMotion()).toBe('boolean');
  });

  it('contiene los tokens obligatorios de la paleta', () => {
    expect(MEGA_TOKENS['--mega-red']).toBe('#D71920');
    expect(MEGA_TOKENS['--mega-black']).toBe('#111318');
    expect(MEGA_TOKENS['--mega-silver']).toBe('#AEB4BC');
  });
});
