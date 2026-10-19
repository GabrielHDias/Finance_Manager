/** Convert a calendar date without using time zones or browser locale. */
export function parseDisplayDate(value: string): string | null {
  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
  if (!match) return null;
  const [, day, month, year] = match;
  const y = Number(year);
  const m = Number(month);
  const d = Number(day);
  if (y < 1 || m < 1 || m > 12 || d < 1) return null;
  const leapYear = y % 4 === 0 && (y % 100 !== 0 || y % 400 === 0);
  const days = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (d > days[m - 1]) return null;
  return `${year}-${month}-${day}`;
}

export function formatDate(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return value;
  const [, year, month, day] = match;
  return `${day}/${month}/${year}`;
}
