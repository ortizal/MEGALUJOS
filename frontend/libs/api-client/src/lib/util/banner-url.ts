import { HttpClient } from '@angular/common/http';
import { BannerDto } from '@megalujos/models';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';

interface MediaMetadata {
  url?: string | null;
}

/**
 * Resuelve la URL de descarga de un media (`/uploads/{storageKey}`)
 * a partir de su `mediaId` (metadatos de `GET /media/{id}`).
 */
export function mediaMetadataUrl(
  http: HttpClient,
  resolveUrl: (path: string) => string,
  mediaId: string,
): Observable<string | null> {
  return http.get<MediaMetadata>(resolveUrl(`/media/${mediaId}`)).pipe(
    map((metadata) => metadata.url ?? null),
    catchError(() => of(null)),
  );
}

/**
 * Completa `BannerDto.url` a partir de `mediaId`.
 * El backend solo envía `mediaId`, así que la URL se obtiene de los metadatos del archivo.
 */
export function withBannerUrls(
  http: HttpClient,
  resolveUrl: (path: string) => string,
  banners: BannerDto[],
): Observable<BannerDto[]> {
  if (!banners.length) {
    return of(banners);
  }
  return forkJoin(
    banners.map((banner) =>
      banner.url || !banner.mediaId
        ? of(banner)
        : mediaMetadataUrl(http, resolveUrl, String(banner.mediaId)).pipe(
            map((url) => ({ ...banner, url: url ?? undefined })),
          ),
    ),
  );
}
