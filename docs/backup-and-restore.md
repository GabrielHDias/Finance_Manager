# Backup e restauração separada

Não foi acessada infraestrutura operacional. Nada neste runbook instala agendamento, altera IAM, cria bucket, executa backup de produção ou comprova controles operacionais existentes.

## Decisões pendentes

Aprovar responsável, RPO, RTO, retenção, destino externo e custódia da chave. Proposta inicial para discussão: execução diária, RPO 24 h, RTO 2 h, retenção 30 dias; não são metas aprovadas. O padrão de alerta de idade é 26 h e deve acompanhar a frequência aprovada. Um teste sintético local não comprova cópia externa ou atendimento ao RTO/RPO operacional.

## Ferramentas e isolamento

Requer Python 3, cliente PostgreSQL compatível com o servidor e age. Configure `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` e `PGPASSFILE` privado (0600), sem senhas no histórico. Use conta de leitura com privilégios mínimos necessários ao dump; a identidade de restauração deve ser separada. Não usar configurações privadas como fixtures. O diretório de saída deve ser 0700; o script define umask 077 e não imprime dados nem diagnósticos do banco.

```bash
python3 scripts/backup.py backup --output /caminho/privado/backups --recipient age1... --retention-days 30
python3 scripts/backup.py verify --metadata /caminho/privado/backups/backup-IDENTIFICADOR.dump.age.json --max-age-hours 26
```

O script gera dump custom, recusa arquivo vazio, valida a lista do pg_restore, criptografa com age e grava checksum/metadados de idade. Arquivos temporários ficam em diretório privado e são removidos ao terminar. A retenção apaga apenas pares reconhecidos e íntegros após um novo backup bem-sucedido. Falhas de dump, validação ou criptografia saem com código não zero; alertar também quando nenhuma execução recente existir, escolhendo o último manifesto íntegro. Metadados não contêm nomes de usuários, saldos ou credenciais.

## Cópia externa e monitoramento

Transferir arquivo `.age` e seu `.json` para destino externo aprovado, criptografado e com controle de acesso separado; nunca copiar dump plaintext. Validar checksum após transporte. Proteger a integridade do manifesto pelo canal e pelas permissões: SHA-256 sozinho não é assinatura contra alteração maliciosa. A chave privada age fica em cofre separado; documentar recuperação e responsável substituto. Credenciais e configuração operacional exigem plano de recuperação separado, com destino/custódia aprovados.

Exemplo proposto de cron, para instalação manual somente após aprovação: `0 2 * * * /caminho/backup-wrapper`. O wrapper deve carregar apenas configuração privada necessária, executar backup, copiar o par ao destino aprovado, verificar integridade e disparar alerta sanitizado por falha/atraso. O script desta tarefa não instala cron nem envia mensagens. Confirmar timezone do host antes de agendar.

## Exercício de restauração

1. Registrar início UTC, origem/checksum do par, versão PostgreSQL, revisão Flyway e responsável. Usar dados sintéticos em testes automatizados.
2. Verificar checksum/idade da cópia transportada. Criar banco vazio separado com nome `fm_restore_...` em instância descartável. O script exige PGHOST local explícito; para ambiente isolado remoto, usar túnel autorizado.
3. Executar `python3 scripts/backup.py restore --metadata /backup.dump.age.json --identity /chave-privada --database fm_restore_ensaio`. Use `--max-age-hours` explícito para ensaio histórico. Não usar --clean sobre instalação existente.
4. `pg_restore` executa uma transação e falha em primeiro erro. Conferir versões/histórico Flyway, contagem por entidade, totais e vínculos em SQL de leitura. Subir a aplicação da revisão alvo contra esse banco e executar verificações sanitizadas.
5. Registrar duração, versão restaurada e diferença entre hora do backup e último dado esperado; comparar com metas aprovadas. Apagar somente recursos descartáveis criados para o ensaio. Não substituir a instalação ativa automaticamente.

Para migrações futuras, ensaiar a revisão nova sobre backup anterior e documentar se imagens antigas ainda leem o esquema. Quando incompatível, rollback requer banco restaurado separado e decisão explícita sobre dados novos.
