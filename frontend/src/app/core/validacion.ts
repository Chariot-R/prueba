/** Validaciones de documentos peruanos (las mismas que hace el backend). */

export function dniValido(dni: string): boolean {
  return /^\d{8}$/.test(dni);
}

/** RUC: 11 dígitos, empieza en 10, 15, 17 o 20, y el último es el dígito verificador (módulo 11). */
export function rucValido(ruc: string): boolean {
  if (!/^(10|15|17|20)\d{9}$/.test(ruc)) return false;
  const factores = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
  const suma = factores.reduce((s, f, i) => s + f * Number(ruc[i]), 0);
  let digito = 11 - (suma % 11);
  if (digito === 10) digito = 0;
  if (digito === 11) digito = 1;
  return digito === Number(ruc[10]);
}
