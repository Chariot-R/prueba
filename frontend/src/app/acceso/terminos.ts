import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terminos',
  imports: [RouterLink],
  template: `
    <main class="texto-legal">
      <a routerLink="/registro">Volver al registro</a>
      <h1>Términos y condiciones</h1>
      <p>Uso de la plataforma web de TI Solutions.</p>

      <h2>1. Uso de la cuenta</h2>
      <p>
        La cuenta es personal (o de la empresa registrada) y sirve para solicitar servicios, revisar
        cotizaciones y dar seguimiento a los trabajos contratados. Eres responsable de mantener tu
        contraseña en secreto.
      </p>

      <h2>2. Datos personales</h2>
      <p>
        Usamos tus datos (nombre o razón social, DNI o RUC, correo y celular) solo para atender tus
        solicitudes, emitir cotizaciones y comunicarnos contigo, conforme a la Ley N.° 29733 de
        Protección de Datos Personales. Puedes pedir la actualización o eliminación de tus datos
        comunicándote con TI Solutions.
      </p>

      <h2>3. Seguridad</h2>
      <p>
        Las contraseñas se guardan cifradas y nadie del personal puede verlas. Por seguridad, la
        cuenta se bloquea temporalmente después de varios intentos fallidos de inicio de sesión.
      </p>

      <h2>4. Cotizaciones</h2>
      <p>
        Las cotizaciones tienen la vigencia indicada en cada documento. Los precios incluyen IGV salvo
        que se indique lo contrario.
      </p>
    </main>
  `,
})
export class Terminos {}
