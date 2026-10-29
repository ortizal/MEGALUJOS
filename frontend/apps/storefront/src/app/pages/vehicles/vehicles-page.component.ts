import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { environment } from '../../../environments/environment';
import { waLink } from '../../shared/whatsapp.util';

interface VehicleModelOption {
  name: string;
  versions: string[];
}

interface VehicleBrandOption {
  name: string;
  models: VehicleModelOption[];
}

/**
 * Datos de referencia (placeholder). El backend aún no expone catálogo
 * público de vehículos; la compatibilidad se confirma por WhatsApp.
 */
const VEHICLE_BRANDS: VehicleBrandOption[] = [
  {
    name: 'Toyota',
    models: [
      { name: 'Corolla', versions: ['Estándar', 'Full'] },
      { name: 'Hilux', versions: ['Estándar', 'Full', 'Deportiva'] },
      { name: 'RAV4', versions: ['Estándar', 'Full'] },
    ],
  },
  {
    name: 'Chevrolet',
    models: [
      { name: 'Aveo', versions: ['Estándar', 'Full'] },
      { name: 'Spark GT', versions: ['Estándar'] },
      { name: 'D-Max', versions: ['Estándar', 'Full'] },
    ],
  },
  {
    name: 'Kia',
    models: [
      { name: 'Rio', versions: ['Estándar', 'Full'] },
      { name: 'Sportage', versions: ['Estándar', 'Full'] },
      { name: 'Picanto', versions: ['Estándar'] },
    ],
  },
  {
    name: 'Hyundai',
    models: [
      { name: 'Accent', versions: ['Estándar', 'Full'] },
      { name: 'Tucson', versions: ['Estándar', 'Full'] },
    ],
  },
  {
    name: 'Nissan',
    models: [
      { name: 'Versa', versions: ['Estándar', 'Full'] },
      { name: 'Navara', versions: ['Estándar', 'Full'] },
    ],
  },
  {
    name: 'Mazda',
    models: [
      { name: 'Mazda 3', versions: ['Estándar', 'Full'] },
      { name: 'CX-5', versions: ['Estándar', 'Full'] },
    ],
  },
];

const CURRENT_YEAR = new Date().getFullYear();
const VEHICLE_YEARS = Array.from({ length: 16 }, (_, index) => String(CURRENT_YEAR - index));

@Component({
  selector: 'app-vehicles-page',
  standalone: true,
  imports: [RouterLink],
  styleUrl: './vehicles-page.component.scss',
  templateUrl: './vehicles-page.component.html',
})
export class VehiclesPageComponent {
  readonly brands = VEHICLE_BRANDS;
  readonly years = VEHICLE_YEARS;

  readonly selectedBrand = signal('');
  readonly selectedModel = signal('');
  readonly selectedYear = signal('');
  readonly selectedVersion = signal('');

  readonly models = computed(() => {
    const brand = this.brands.find((item) => item.name === this.selectedBrand());
    return brand?.models ?? [];
  });

  readonly versions = computed(() => {
    const model = this.models().find((item) => item.name === this.selectedModel());
    return model?.versions ?? [];
  });

  readonly isComplete = computed(
    () =>
      !!this.selectedBrand() &&
      !!this.selectedModel() &&
      !!this.selectedYear() &&
      !!this.selectedVersion(),
  );

  readonly whatsappUrl = computed(() => {
    const summary = `${this.selectedBrand()} ${this.selectedModel()} ${this.selectedYear()} (${this.selectedVersion()})`;
    const message = this.isComplete()
      ? `Hola MEGALUJOS 👋 Tengo un vehículo *${summary}*. ¿Qué accesorios me recomiendan y cuáles son compatibles?`
      : environment.whatsappDefaultMessage;
    return waLink(message, environment.whatsappNumber);
  });

  onBrandChange(value: string): void {
    this.selectedBrand.set(value);
    this.selectedModel.set('');
    this.selectedVersion.set('');
  }

  onModelChange(value: string): void {
    this.selectedModel.set(value);
    this.selectedVersion.set('');
  }

  onWhatsappClick(event: MouseEvent): void {
    if (!this.isComplete()) {
      event.preventDefault();
    }
  }
}
