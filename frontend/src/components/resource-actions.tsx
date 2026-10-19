interface ResourceActionsProps {
  kind: "conta" | "categoria";
  name: string;
  confirming: boolean;
  deleting: boolean;
  disabled: boolean;
  errorMessage: string;
  onEdit: () => void;
  onRequestDelete: () => void;
  onConfirmDelete: () => void;
  onCancelDelete: () => void;
}

export default function ResourceActions({
  kind,
  name,
  confirming,
  deleting,
  disabled,
  errorMessage,
  onEdit,
  onRequestDelete,
  onConfirmDelete,
  onCancelDelete,
}: ResourceActionsProps) {
  return (
    <div className="resource-actions">
      {confirming ? (
        <>
          <p>
            Excluir a {kind} “{name}”? Esta ação não pode ser desfeita.
            {" "}{kind === "conta" ? "Contas" : "Categorias"} com transações vinculadas não podem ser excluídas.
          </p>
          {errorMessage && <p className="error-message" role="alert">{errorMessage}</p>}
          <button type="button" disabled={disabled} onClick={onConfirmDelete}>
            {deleting ? "Excluindo..." : "Confirmar exclusão"}
          </button>
          <button type="button" disabled={disabled} onClick={onCancelDelete}>
            Cancelar exclusão
          </button>
        </>
      ) : (
        <>
          <button type="button" disabled={disabled} onClick={onEdit} aria-label={`Editar ${kind} ${name}`}>
            Editar
          </button>
          <button type="button" disabled={disabled} onClick={onRequestDelete} aria-label={`Excluir ${kind} ${name}`}>
            Excluir
          </button>
        </>
      )}
    </div>
  );
}
