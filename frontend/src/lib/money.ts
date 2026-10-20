// Money crosses JSON as a decimal string. BigInt cents never lose precision.
export function cents(value: string): bigint {
  if (!/^-?\d+(?:\.\d{1,2})?$/.test(value)) throw new Error("Valor monetário inválido.");
  const negative = value.startsWith("-");
  const [whole, fraction = ""] = value.replace(/^-/, "").split(".");
  const result = BigInt(whole) * BigInt(100) + BigInt(fraction.padEnd(2, "0"));
  return negative ? -result : result;
}
export function decimal(value: bigint): string {
  const absolute = value < BigInt(0) ? -value : value;
  return `${value < BigInt(0) ? "-" : ""}${absolute / BigInt(100)}.${String(absolute % BigInt(100)).padStart(2, "0")}`;
}
export function validInput(value: string): boolean {
  return /^\d{1,17}(?:\.\d{1,2})?$/.test(value) && cents(value) > BigInt(0);
}
// Only a bounded dimensionless ratio is converted for charts/progress bars.
export function ratio(value: string, total: string): number {
  const denominator = cents(total);
  return denominator === BigInt(0) ? 0 : Number(cents(value) * BigInt(1000000) / denominator) / 1000000;
}
export function sign(value: string): number {
  const amount = cents(value);
  return amount > BigInt(0) ? 1 : amount < BigInt(0) ? -1 : 0;
}
