import { Component } from '@angular/core';
import { environment } from '../../../environments/environment';
import { generalWhatsappMessage, waLink } from '../../shared/whatsapp.util';

/** Botón flotante fijo de WhatsApp (bottom-right). */
@Component({
  selector: 'app-whatsapp-button',
  standalone: true,
  styleUrl: './whatsapp-button.component.scss',
  template: `
    <a
      class="wa-fab"
      [href]="href"
      target="_blank"
      rel="noopener"
      aria-label="Escríbenos por WhatsApp"
    >
      <span class="wa-fab__icon" aria-hidden="true">💬</span>
      <span class="wa-fab__label">WhatsApp</span>
    </a>
  `,
})
export class WhatsAppButtonComponent {
  readonly href = waLink(generalWhatsappMessage(), environment.whatsappNumber);
}
