import {
  Component,
  ElementRef,
  booleanAttribute,
  computed,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';

/**
 * Selector de archivos (uploader) MEGALUJOS: drag & drop, botón nativo,
 * validación de tamaño/tipo y previsualización de la primera imagen.
 * Es UI pura: emite los `File` seleccionados; la subida la hace el contenedor
 * (p. ej. `MediaService.upload`).
 */
@Component({
  selector: 'mega-uploader',
  standalone: true,
  styleUrl: './mega-uploader.component.scss',
  template: `
    <div
      class="mega-uploader"
      [class.mega-uploader--dragging]="dragging()"
      [class.mega-uploader--invalid]="!!error()"
      role="button"
      tabindex="0"
      [attr.aria-label]="label()"
      (dragover)="onDragOver($event)"
      (dragleave)="onDragLeave()"
      (drop)="onDrop($event)"
      (keydown.enter)="openPicker()"
      (keydown.space)="openPicker()"
    >
      @if (previewUrl()) {
        <div class="mega-uploader__preview">
          <img [src]="previewUrl()" [attr.alt]="label()" />
          <button
            type="button"
            class="mega-uploader__remove"
            aria-label="Quitar archivo"
            (click)="clear($event)"
          >
            ✕
          </button>
        </div>
      } @else {
        <span class="mega-uploader__icon" aria-hidden="true">⬆</span>
        <p class="mega-uploader__title">{{ label() }}</p>
        <p class="mega-uploader__hint">{{ hint() }}</p>
      }

      <input
        #fileInput
        class="mega-uploader__input"
        type="file"
        [attr.accept]="accept()"
        [multiple]="multiple()"
        (change)="onFileChange($event)"
      />
    </div>

    @if (error()) {
      <p class="mega-uploader__error" role="alert">{{ error() }}</p>
    }

    @if (fileNames().length > 1) {
      <ul class="mega-uploader__list">
        @for (name of fileNames(); track name) {
          <li>{{ name }}</li>
        }
      </ul>
    }
  `,
})
export class MegaUploaderComponent {
  /** Texto principal (etiqueta accesible del área). */
  label = input('Arrastra un archivo o haz clic para seleccionar');

  /** Texto secundario (formatos y tamaño permitido). */
  hint = input('PNG, JPG, WEBP o PDF');

  /** Aceptación MIME/tipos (attr accept del input). */
  accept = input('.png,.jpg,.jpeg,.webp,.pdf,image/*,application/pdf');

  /** Selección múltiple. */
  multiple = input(false, { transform: booleanAttribute });

  /** Tamaño máximo por archivo en MB. */
  maxSizeMb = input(8);

  /** true mientras arrastra un archivo sobre la zona (señal interna). */
  readonly dragging = signal(false);

  /** Emite los archivos validados seleccionados. */
  filesSelected = output<File[]>();

  /** Emite al quitar la selección. */
  cleared = output<void>();

  /** Archivos seleccionados (para listar nombres). */
  readonly files = signal<File[]>([]);

  /** Error de validación visible. */
  readonly error = signal<string | null>(null);

  /** URL de previsualización de la primera imagen. */
  readonly previewUrl = signal<string | null>(null);

  readonly fileNames = computed(() => this.files().map((file) => file.name));

  private readonly input = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  openPicker(): void {
    this.input().nativeElement.click();
  }

  onFileChange(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    this.acceptFiles(inputElement.files ? Array.from(inputElement.files) : []);
    inputElement.value = '';
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  onDragLeave(): void {
    this.dragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const dropped = event.dataTransfer?.files ? Array.from(event.dataTransfer.files) : [];
    this.acceptFiles(dropped);
  }

  clear(event?: Event): void {
    event?.stopPropagation();
    this.revokePreview();
    this.files.set([]);
    this.error.set(null);
    this.cleared.emit();
  }

  private acceptFiles(selected: File[]): void {
    if (selected.length === 0) {
      return;
    }
    const limit = this.maxSizeMb();
    const tooBig = selected.find((file) => file.size > limit * 1024 * 1024);
    if (tooBig) {
      this.error.set(`"${tooBig.name}" supera el máximo de ${limit} MB.`);
      return;
    }
    this.error.set(null);
    this.revokePreview();
    this.files.set(selected);

    const first = selected[0];
    if (first.type.startsWith('image/')) {
      this.previewUrl.set(URL.createObjectURL(first));
    } else {
      this.previewUrl.set(null);
    }
    this.filesSelected.emit(selected);
  }

  private revokePreview(): void {
    const url = this.previewUrl();
    if (url) {
      URL.revokeObjectURL(url);
    }
  }
}
