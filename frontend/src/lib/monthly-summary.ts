import { parseDisplayDate } from "./dates";

export const MONTH_NAMES = [
  "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
  "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
];

export function monthPeriod(reference: string) {
  const match = /^(\d{4})-(\d{2})$/.exec(reference);
  if (!match || !parseDisplayDate(`01/${match[2]}/${match[1]}`)) {
    throw new Error("Informe um mês e um ano válidos, com quatro dígitos no ano.");
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return { startDate: `${reference}-01`, endDate: `${reference}-${days[month - 1]}` };
}

export function monthlyPeriods(reference: string) {
  const current = monthPeriod(reference);
  const year = Number(reference.slice(0, 4));
  const month = Number(reference.slice(5));
  if (year === 1 && month === 1) throw new Error("Escolha um mês a partir de fevereiro de 0001 para comparar com o mês anterior.");
  const previousReference = month === 1
    ? `${String(year - 1).padStart(4, "0")}-12`
    : `${reference.slice(0, 4)}-${String(month - 1).padStart(2, "0")}`;
  return { reference, previousReference, current, previous: monthPeriod(previousReference) };
}

export function monthLabel(reference: string): string {
  monthPeriod(reference);
  return `${MONTH_NAMES[Number(reference.slice(5)) - 1]} de ${reference.slice(0, 4)}`;
}

export function monthlyDifference(current: number, previous: number): number {
  return (Math.round(current * 100) - Math.round(previous * 100)) / 100;
}
