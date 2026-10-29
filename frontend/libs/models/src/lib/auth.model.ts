import { IsoDateTime, Uuid } from './common.model';

/** Solicitud de inicio de sesión. */
export interface LoginRequest {
  username: string;
  password: string;
}

/** Respuesta de inicio de sesión / refresh. */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  userId: Uuid;
  username: string;
  fullName: string;
  roles: string[];
  permissions: string[];
}

/** Solicitud de refresh token. */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/** Solicitud de cambio de contraseña. */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** Usuario administrado. */
export interface UserDto {
  id: Uuid;
  username: string;
  fullName: string;
  email?: string | null;
  phone?: string | null;
  roles: string[];
  permissions: string[];
  active?: boolean;
  createdAt?: IsoDateTime | null;
}

/** Creación de usuario. */
export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email?: string | null;
  phone?: string | null;
  roleIds: Uuid[];
}

/** Actualización de usuario. */
export interface UpdateUserRequest {
  fullName: string;
  email?: string | null;
  phone?: string | null;
  roleIds: Uuid[];
}

/** Rol con sus permisos. */
export interface RoleDto {
  id: Uuid;
  code: string;
  name: string;
  description?: string | null;
  permissions: PermissionDto[];
}

/** Permiso del sistema. */
export interface PermissionDto {
  id: Uuid;
  code: string;
  name: string;
  module: string;
}

/** Entrada de auditoría. */
export interface AuditLogDto {
  id: Uuid;
  actorId?: Uuid | null;
  action: string;
  entityType?: string | null;
  entityId?: Uuid | null;
  metadata?: string | null;
  ipAddress?: string | null;
  occurredAt: IsoDateTime;
}

/** Usuario autenticado (estado local del cliente). */
export interface AuthUser {
  /** Id del usuario (nombre del contrato maestro). */
  userId: Uuid;
  /** Alias de `userId` (compatibilidad con plantillas existentes). */
  id: Uuid;
  username: string;
  fullName: string;
  roles: string[];
  permissions: string[];
}
