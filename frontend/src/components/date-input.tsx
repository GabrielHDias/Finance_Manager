"use client";

import { useRef, type InputHTMLAttributes } from "react";
import { formatDate, parseDisplayDate } from "@/lib/dates";

type DateInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, "type" | "pattern" | "placeholder" | "maxLength" | "inputMode" | "onChange" | "value" | "defaultValue"> & {
  value?: string;
  defaultValue?: string;
  onValueChange?: (value: string) => void;
};

export default function DateInput({ value, defaultValue, onValueChange, ...props }: DateInputProps) {
  const textInput = useRef<HTMLInputElement>(null);

  return (
    <div className="date-input">
      <input
        {...props}
        ref={textInput}
        type="text"
        value={value}
        defaultValue={defaultValue}
        onChange={event => onValueChange?.(event.target.value)}
        inputMode="text"
        placeholder="DD/MM/YYYY"
        pattern="[0-9]{2}/[0-9]{2}/[0-9]{4}"
        maxLength={10}
        title="Informe a data no formato DD/MM/YYYY."
      />
      <span className="date-input__picker">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" aria-hidden="true">
          <rect x="3" y="5" width="18" height="16" rx="2" />
          <path d="M16 3v4M8 3v4M3 11h18" />
        </svg>
        <input
          type="date"
          min="0001-01-01"
          max="9999-12-31"
          disabled={props.disabled}
          aria-label={`Abrir calendário: ${props.id === "date" ? "Data" : props.name === "startDate" ? "Data inicial" : "Data final"}`}
          onFocus={event => {
            event.currentTarget.value = parseDisplayDate(textInput.current?.value ?? "") ?? "";
          }}
          onChange={event => {
            const nextValue = formatDate(event.target.value);
            if (value === undefined && textInput.current) textInput.current.value = nextValue;
            onValueChange?.(nextValue);
          }}
        />
      </span>
    </div>
  );
}
