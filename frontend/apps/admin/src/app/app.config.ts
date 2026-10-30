import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideApiConfig } from '@megalujos/api-client';
import { provideAuthInterceptor } from '@megalujos/auth';
import { environment } from '../environments/environment';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideAuthInterceptor(),
    // Base de la API por environment: `/api/v1` en dev, `/megalujos/api` en prod
    // (nginx reescribe a `/api/v1/$1` hacia el backend en Docker).
    ...provideApiConfig({ baseUrl: environment.apiBaseUrl }),
  ],
};
