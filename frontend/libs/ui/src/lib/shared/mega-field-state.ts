/**
 * Estado visual de validación de un campo MEGALUJOS.
 * Se traduce a las clases de estado de Bootstrap 5:
 *   - `valid`   -> `is-valid`   (borde/feedback verde, `--mega-success`)
 *   - `invalid` -> `is-invalid` (borde/feedback rojo, `--mega-red`)
 *   - `warning` -> `is-warning` (borde/feedback ámbar, `--mega-warning`)
 *   - `null`    -> sin estado (campo neutro)
 */
export type MegaFieldState = 'valid' | 'invalid' | 'warning' | null;
