import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { errorApi, erroresPorCampo } from '../core/errores';
import { RegistroCliente } from '../core/modelos';
import { SesionService } from '../core/sesion.service';
import { dniValido, rucValido } from '../core/validacion';
import { CampoClave } from './campo-clave';
import { PanelMarca } from './panel-marca';

/** Registro de clientes: persona natural (DNI) o empresa (RUC). El backend vuelve a validar todo. */
@Component({
  selector: 'app-registro',
  imports: [FormsModule, RouterLink, CampoClave, PanelMarca],
  host: { class: 'acceso' },
  templateUrl: './registro.html',
})
export class Registro {
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  protected datos: RegistroCliente = {
    tipo: 'N',
    nombres: '',
    apellidos: '',
    razonSocial: '',
    nombreContacto: '',
    documento: '',
    email: '',
    telefono: '',
    password: '',
    confirmar: '',
    terminos: false,
    sitioWeb: '',
  };

  protected errores = signal<Record<string, string>>({});
  protected errorGeneral = signal('');
  protected enviando = signal(false);

  protected get empresa() {
    return this.datos.tipo === 'J';
  }

  protected soloNumeros(campo: 'documento' | 'telefono', valor: string) {
    this.datos[campo] = valor.replace(/\D/g, '');
    this.limpiar(campo);
  }

  protected limpiar(campo: string) {
    if (this.errores()[campo]) {
      const { [campo]: _, ...resto } = this.errores();
      this.errores.set(resto);
    }
  }

  /** Revisión rápida en el navegador para avisar antes de enviar. */
  private validar(): Record<string, string> {
    const d = this.datos;
    const e: Record<string, string> = {};
    if (this.empresa) {
      if (!d.razonSocial.trim()) e['razonSocial'] = 'Ingresa la razón social.';
      if (!d.nombreContacto.trim()) e['nombreContacto'] = 'Ingresa el nombre de la persona de contacto.';
      if (!rucValido(d.documento)) e['documento'] = 'El RUC no es válido. Revisa los 11 dígitos.';
    } else {
      if (!d.nombres.trim()) e['nombres'] = 'Ingresa tus nombres.';
      if (!d.apellidos.trim()) e['apellidos'] = 'Ingresa tus apellidos.';
      if (!dniValido(d.documento)) e['documento'] = 'El DNI debe tener 8 dígitos.';
    }
    if (!/^9\d{8}$/.test(d.telefono)) e['telefono'] = 'Ingresa un celular de 9 dígitos que empiece con 9.';
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.email.trim())) e['email'] = 'Ingresa un correo válido.';
    if (d.password.length < 8) e['password'] = 'La contraseña debe tener al menos 8 caracteres.';
    else if (!/[A-Za-z]/.test(d.password) || !/\d/.test(d.password)) e['password'] = 'La contraseña debe tener letras y números.';
    else if (d.password !== d.confirmar) e['confirmar'] = 'Las contraseñas no coinciden.';
    if (!d.terminos) e['terminos'] = 'Debes aceptar los términos y condiciones.';
    return e;
  }

  protected async crear() {
    this.errorGeneral.set('');
    const locales = this.validar();
    this.errores.set(locales);
    if (Object.keys(locales).length) {
      this.errorGeneral.set('Revisa los datos marcados.');
      return;
    }
    this.enviando.set(true);
    try {
      await this.sesion.registrar(this.datos);
      // El correo viaja en el estado de navegación, no en la URL (no queda en el historial)
      await this.router.navigate(['/login'], { state: { registrado: this.datos.email.trim().toLowerCase() } });
    } catch (e) {
      const err = errorApi(e);
      this.errores.set(erroresPorCampo(err));
      this.errorGeneral.set(err.tipo === 'VALIDACION' ? 'Revisa los datos marcados.' : err.mensaje);
      this.datos.password = '';
      this.datos.confirmar = '';
    } finally {
      this.enviando.set(false);
    }
  }
}
