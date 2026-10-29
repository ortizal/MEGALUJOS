import { Component, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { generalWhatsappMessage, waLink } from '../../shared/whatsapp.util';

@Component({
  selector: 'app-contact-page',
  standalone: true,
  styleUrl: './contact-page.component.scss',
  templateUrl: './contact-page.component.html',
})
export class ContactPageComponent {
  readonly whatsappUrl = waLink(generalWhatsappMessage(), environment.whatsappNumber);
  readonly phone = environment.contactPhone;
  readonly phoneHref = `tel:${environment.whatsappNumber}`;
  readonly email = environment.contactEmail;
  readonly address = environment.address;
  readonly hours = environment.hours;
}
