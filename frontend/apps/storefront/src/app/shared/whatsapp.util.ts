import { environment } from '../../environments/environment';

/** Construye un enlace wa.me con el mensaje prellenado. */
export function waLink(message: string, phone: string = environment.whatsappNumber): string {
  return `https://wa.me/${phone}?text=${encodeURIComponent(message)}`;
}

/** Mensaje por defecto de la vitrina. */
export function generalWhatsappMessage(): string {
  return environment.whatsappDefaultMessage;
}

/** Mensaje prellenado de consulta de producto (nombre, SKU y precio). */
export function productWhatsappMessage(product: {
  name: string;
  sku: string;
  price: string | null;
}): string {
  const price = product.price ? ` a $${product.price}` : '';
  return `Hola MEGALUJOS 👋 Estoy interesado en *${product.name}* (SKU ${product.sku})${price}. ¿Está disponible?`;
}
