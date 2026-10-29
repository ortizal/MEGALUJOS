import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
  viewChild,
  TemplateRef,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RolesAdminService, UsersAdminService } from '@megalujos/api-client';
import { CreateUserRequest, RoleDto, UpdateUserRequest, UserDto } from '@megalujos/models';
import {
  MegaButtonComponent,
  MegaConfirmService,
  MegaEmptyStateComponent,
  MegaInputComponent,
  MegaModalComponent,
  MegaPaginationComponent,
  MegaSelectComponent,
  MegaStatusBadgeComponent,
  MegaTableColumn,
  MegaTableCellContext,
  MegaTableComponent,
  MegaToastService,
} from '@megalujos/ui';
import { errorMessage, shortId } from '../../core/util/ui';

interface UserFormData {
  username: string;
  password: string;
  fullName: string;
  email: string;
  phone: string;
  roleIds: string[];
}

/** Gestión de usuarios del panel: listado, creación, edición, roles, activación y restablecimiento de contraseña. */
@Component({
  selector: 'app-users-page',
  imports: [
    FormsModule,
    MegaButtonComponent,
    MegaEmptyStateComponent,
    MegaInputComponent,
    MegaModalComponent,
    MegaPaginationComponent,
    MegaSelectComponent,
    MegaStatusBadgeComponent,
    MegaTableComponent,
  ],
  templateUrl: './users.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersPage {
  private readonly users = inject(UsersAdminService);
  private readonly rolesService = inject(RolesAdminService);
  private readonly toast = inject(MegaToastService);
  private readonly confirm = inject(MegaConfirmService);
  private readonly destroyRef = inject(DestroyRef);

  readonly query = signal('');
  readonly activeFilter = signal('');
  readonly page = signal(1);
  readonly size = 10;
  readonly total = signal(0);
  readonly rows = signal<UserDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly allRoles = signal<RoleDto[]>([]);

  readonly rowKey = (row: UserDto): string => row.id;

  readonly userModal = signal(false);
  readonly modalMode = signal<'create' | 'edit'>('create');
  readonly modalUserId = signal<string | null>(null);
  readonly form = signal<UserFormData>({
    username: '',
    password: '',
    fullName: '',
    email: '',
    phone: '',
    roleIds: [],
  });

  readonly resetModal = signal(false);
  readonly resetUserId = signal<string | null>(null);
  readonly resetPassword = signal('');

  readonly rolesCell = viewChild<TemplateRef<MegaTableCellContext<UserDto>>>('rolesCell');
  readonly activeCell = viewChild<TemplateRef<MegaTableCellContext<UserDto>>>('activeCell');
  readonly actionsCell = viewChild<TemplateRef<MegaTableCellContext<UserDto>>>('actionsCell');

  readonly columns = computed<MegaTableColumn<UserDto>[]>(() => [
    { key: 'username', header: 'Usuario', width: '14rem' },
    { key: 'fullName', header: 'Nombre completo' },
    { key: 'email', header: 'Correo', width: '16rem' },
    { header: 'Roles', width: '14rem', cell: this.rolesCell() ?? undefined },
    { header: 'Estado', width: '9rem', cell: this.activeCell() ?? undefined },
    { header: 'Acciones', align: 'end', width: '17rem', cell: this.actionsCell() ?? undefined },
  ]);

  constructor() {
    this.loadRoles();
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.users
      .list({
        q: this.query() || undefined,
        active: this.activeFilter() === '' ? undefined : this.activeFilter() === 'true',
        page: this.page() - 1,
        size: this.size,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (page) => {
          this.rows.set(page.items ?? []);
          this.total.set(page.totalElements ?? 0);
          this.loading.set(false);
        },
        error: (err) => {
          const message = errorMessage(err);
          this.error.set(message);
          this.loading.set(false);
          this.toast.error(message, { title: 'No se pudieron cargar los usuarios' });
        },
      });
  }

  private loadRoles(): void {
    this.rolesService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (list) => this.allRoles.set(list ?? []),
        error: () => this.allRoles.set([]),
      });
  }

  roleOptions(): Array<{ value: string; label: string }> {
    return this.allRoles().map((role) => ({ value: role.id, label: role.name }));
  }

  readonly activeOptions = [
    { value: '', label: 'Todos' },
    { value: 'true', label: 'Activo' },
    { value: 'false', label: 'Inactivo' },
  ];

  isRoleSelected(roleId: string): boolean {
    return this.form().roleIds.includes(roleId);
  }

  toggleRole(roleId: string, checked: boolean): void {
    const current = this.form().roleIds;
    this.form.set({
      ...this.form(),
      roleIds: checked ? [...current, roleId] : current.filter((id) => id !== roleId),
    });
  }

  goToPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  applyFilters(): void {
    this.page.set(1);
    this.load();
  }

  clearFilters(): void {
    this.query.set('');
    this.activeFilter.set('');
    this.page.set(1);
    this.load();
  }

  openCreate(): void {
    this.modalMode.set('create');
    this.modalUserId.set(null);
    this.form.set({ username: '', password: '', fullName: '', email: '', phone: '', roleIds: [] });
    this.userModal.set(true);
  }

  openEdit(user: UserDto): void {
    this.modalMode.set('edit');
    this.modalUserId.set(user.id);
    this.form.set({
      username: user.username,
      password: '',
      fullName: user.fullName ?? '',
      email: user.email ?? '',
      phone: user.phone ?? '',
      roleIds: [...(user.roles ?? [])],
    });
    this.userModal.set(true);
  }

  closeUserModal(): void {
    this.userModal.set(false);
  }

  saveUser(): void {
    const f = this.form();
    if (!f.username.trim() || !f.fullName.trim()) {
      this.toast.warning('Usuario y nombre completo son obligatorios.');
      return;
    }
    if (this.modalMode() === 'create' && !f.password) {
      this.toast.warning('La contraseña es obligatoria.');
      return;
    }

    const request: CreateUserRequest | UpdateUserRequest =
      this.modalMode() === 'create'
        ? {
            username: f.username.trim(),
            password: f.password,
            fullName: f.fullName.trim(),
            email: f.email.trim() || null,
            phone: f.phone.trim() || null,
            roleIds: f.roleIds,
          }
        : {
            fullName: f.fullName.trim(),
            email: f.email.trim() || null,
            phone: f.phone.trim() || null,
            roleIds: f.roleIds,
          };

    const save$ =
      this.modalMode() === 'create'
        ? this.users.create(request as CreateUserRequest)
        : this.users.update(this.modalUserId() as string, request as UpdateUserRequest);

    save$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.closeUserModal();
        this.toast.success(this.modalMode() === 'create' ? 'Usuario creado.' : 'Usuario actualizado.');
        this.load();
      },
      error: (err) => this.toast.error(errorMessage(err), { title: 'No se pudo guardar el usuario' }),
    });
  }

  toggleActive(user: UserDto): void {
    const target = !user.active;
    this.confirm
      .ask(`¿${target ? 'Activar' : 'Desactivar'} al usuario "${user.username}"?`)
      .then((ok) => {
        if (!ok) {
          return;
        }
        this.users
          .setActive(user.id, target)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: () => {
              this.toast.success(`Usuario ${target ? 'activado' : 'desactivado'}.`);
              this.load();
            },
            error: (err) => this.toast.error(errorMessage(err), { title: 'No se pudo cambiar el estado' }),
          });
      });
  }

  openReset(user: UserDto): void {
    this.resetUserId.set(user.id);
    this.resetPassword.set('');
    this.resetModal.set(true);
  }

  closeResetModal(): void {
    this.resetModal.set(false);
  }

  doReset(): void {
    if (!this.resetPassword()) {
      this.toast.warning('Ingresa la nueva contraseña.');
      return;
    }
    this.users
      .resetPassword(this.resetUserId() as string, this.resetPassword())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.closeResetModal();
          this.toast.success('Contraseña restablecida.');
        },
        error: (err) => this.toast.error(errorMessage(err), { title: 'No se pudo restablecer la contraseña' }),
      });
  }

  readonly shortId = shortId;
}
