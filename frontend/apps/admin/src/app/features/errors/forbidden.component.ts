import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Página 403 (sin permisos para la sección). */
@Component({
  selector: 'app-forbidden',
  imports: [RouterLink],
  template: `
    <div class="error-screen">
      <span aria-hidden="true" style="font-size: 3rem">🔒</span>
      <h1>403</h1>
      <p>
        Tu usuario no tiene permisos para acceder a esta sección. Solicita al administrador el
        permiso correspondiente.
      </p>
      <a routerLink="/dashboard" class="btn btn-danger">Volver al dashboard</a>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForbiddenComponent {}
