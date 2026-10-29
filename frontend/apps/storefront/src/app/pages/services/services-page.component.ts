import { Component, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { waLink } from '../../shared/whatsapp.util';

interface PublicService {
  icon: string;
  title: string;
  description: string;
  priceFrom: string;
  features: string[];
}

@Component({
  selector: 'app-services-page',
  standalone: true,
  styleUrl: './services-page.component.scss',
  templateUrl: './services-page.component.html',
})
export class ServicesPageComponent {
  readonly whatsappNumber = environment.whatsappNumber;

  readonly services: PublicService[] = [
    {
      icon: '🧽',
      title: 'Detailing premium',
      description:
        'Renovamos la estética de tu vehículo con limpieza profunda, descontaminación de pintura, pulido y sellado.',
      priceFrom: 'Desde $45',
      features: [
        'Lavado de detalle interior y exterior',
        'Pulido de pintura y plásticos',
        'Sellado con cera o cerámico',
      ],
    },
    {
      icon: '🔧',
      title: 'Instalación de accesorios',
      description:
        'Instalamos stickers, vinilos, luces LED, cámaras de retroceso, sensores y más, con acabado profesional.',
      priceFrom: 'Desde $15',
      features: [
        'Stickers y personalización',
        'Luces y electrónica',
        'Cámaras, sensores y accesorios',
      ],
    },
    {
      icon: '🛡️',
      title: 'Protección y sellado',
      description:
        'Protección de largo plazo para la pintura con tratamientos cerámicos y películas de protección (PPF).',
      priceFrom: 'Desde $120',
      features: [
        'Recubrimiento cerámico',
        'Películas de protección PPF',
        'Tratamiento de cristales',
      ],
    },
  ];

  quoteUrl(service?: PublicService): string {
    const message = service
      ? `Hola MEGALUJOS 👋 Quiero cotizar el servicio de *${service.title}*. ¿Me pueden indicar precios y disponibilidad?`
      : 'Hola MEGALUJOS 👋 Quiero cotizar un servicio automotriz.';
    return waLink(message, this.whatsappNumber);
  }
}
