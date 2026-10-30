/**
 * Configuración de producción de la vitrina.
 * Bajo `/megalujos/store/` la API se sirve namespaced: nginx reescribe
 * `/megalujos/api/(.*)` → `/api/v1/$1` hacia el backend en Docker (127.0.0.1:18090).
 * Ajustar datos de contacto reales antes de desplegar.
 */
export const environment = {
  production: true,
  /** Prefijo de la API en producción (namespaced bajo /megalujos). */
  apiBaseUrl: '/megalujos/api',
  /** Número WhatsApp en formato internacional sin '+' (placeholder configurable). */
  whatsappNumber: '593999999999',
  whatsappDefaultMessage:
    'Hola MEGALUJOS 👋 Quiero información sobre productos y servicios automotrices.',
  contactPhone: '+593 9 9999 9999',
  contactEmail: 'contacto@megalujos.example',
  address: 'Av. Ejemplo 123, Guayaquil, Ecuador',
  hours: 'Lun a Vie 09:00–18:00 · Sáb 09:00–13:00',
  facebookUrl: '#',
  instagramUrl: '#',
  youtubeUrl: '#',
};
