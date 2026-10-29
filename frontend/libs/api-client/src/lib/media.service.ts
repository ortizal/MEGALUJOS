import { Injectable } from '@angular/core';
import { MediaDto } from '@megalujos/models';
import { Observable } from 'rxjs';
import { ApiBaseService } from './api-base';

/**
 * Subida de archivos (imágenes y PDF) al MediaController.
 * Usa multipart/form-data con el campo `file` e `isPublic` (query param).
 */
@Injectable({ providedIn: 'root' })
export class MediaService extends ApiBaseService {
  /** Sube un archivo y devuelve sus metadatos (id, url). */
  upload(file: File, isPublic = true): Observable<MediaDto> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.httpPost<MediaDto>('/media', form, {
      params: { isPublic: String(isPublic) },
    });
  }

  /** Alias de `upload` con objeto de opciones. */
  uploadFile(file: File, options?: { isPublic?: boolean }): Observable<MediaDto> {
    return this.upload(file, options?.isPublic ?? true);
  }

  /** Metadatos de un archivo subido. */
  metadata(id: string): Observable<MediaDto> {
    return this.httpGet<MediaDto>(`/media/${id}`);
  }

  /** Alias de `metadata`. */
  getMetadata(id: string): Observable<MediaDto> {
    return this.metadata(id);
  }
}
