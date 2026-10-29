import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Página 404 (ruta inexistente). */
@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <div class="error-screen">
      <span aria-hidden="true" style="font-size: 3rem">🧭</span>
      <h1>404</h1>
      <p>No encontramos la página que buscas. Puede que el enlace esté desactualizado.</p>
      <a routerLink="/dashboard" class="btn btn-danger">Volver al dashboard</a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundComponent {}
