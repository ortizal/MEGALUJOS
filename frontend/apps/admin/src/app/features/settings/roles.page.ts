import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal, viewChild, TemplateRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RolesAdminService } from '@megalujos/api-client';
import { PermissionDto, RoleDto } from '@megalujos/models';
import {
  MegaEmptyStateComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage } from '../../core/util/ui';

/** Catálogo de roles y sus permisos (consulta; la asignación de permisos se administra en backend). */
@Component({
  selector: 'app-roles-page',
  imports: [
    MegaEmptyStateComponent,
    MegaStatusBadgeComponent,
    MegaTableComponent,
  ],
  templateUrl: './roles.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RolesPage {
  private readonly rolesService = inject(RolesAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<RoleDto[]>([]);

  readonly rowKey = (row: RoleDto): string => row.id;

  readonly permissionsCell = viewChild<TemplateRef<MegaTableCellContext<RoleDto>>>('permissionsCell');

  readonly columns = computed<MegaTableColumn<RoleDto>[]>(() => [
    { key: 'name', header: 'Rol' },
    { key: 'code', header: 'Código', width: '13rem' },
    { key: 'description', header: 'Descripción', width: '28rem' },
    { header: 'Permisos', width: '32rem', cell: this.permissionsCell() ?? undefined },
  ]);

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.rolesService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => {
          this.rows.set(list ?? []);
          this.loading.set(false);
        },
        error: (err) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar los roles' });
        },
      });
  }

  permissionCodes(role: RoleDto): string[] {
    return (role.permissions ?? []).map((permission: PermissionDto) => permission.code);
  }

  hasPermissions(role: RoleDto): boolean {
    return (role.permissions ?? []).length > 0;
  }
}
