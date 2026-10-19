import type { InputHTMLAttributes } from "react";

type DateInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "pattern" | "placeholder" | "maxLength" | "inputMode">;

export default function DateInput(props: DateInputProps) {
  return (
    <input
      {...props}
      type="text"
      inputMode="text"
      placeholder="DD/MM/YYYY"
      pattern="[0-9]{2}/[0-9]{2}/[0-9]{4}"
      maxLength={10}
      title="Informe a data no formato DD/MM/YYYY."
    />
  );
}
