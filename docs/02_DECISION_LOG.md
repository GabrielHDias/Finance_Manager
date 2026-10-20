# Decisões de implementação — F01–F11

Data: 20/10/2026. Base: `4f5d781e116091d42fb14d6392ec700452634191`. Estas decisões descrevem o código da alteração local, sem atestar implantação em produção.

| Tarefa | Decisão | Consequência e limite |
|---|---|---|
| F01 | Strings decimais em JSON e BigInt em centavos no cliente | Entrada positiva até 99999999999999999.99, até duas casas. BigDecimal/NUMERIC preservados; agregados não são limitados a 17 inteiros. Respostas usam duas casas sem arredondamento implícito. Formulários, orçamento, CSV, totais e diferenças usam o mesmo contrato. Proporções dimensionais dos gráficos usam aproximação numérica de seis casas; legendas e totais permanecem exatos. |
| F02 | Limitação local por socket peer + rota | Evita confiar em headers arbitrários. Cada réplica tem janela própria; proxy intermediário compartilha cota. Ajustar limites à população/NAT antes de exposição pública; limites globais requerem gateway distribuído validado. |
| F03 | Sessão stateless de uma hora, sem refresh ou revogação imediata | Logout remove a cópia local, tokens roubados continuam válidos até expirar. Não se adotaram issuer/audience porque há um único emissor e consumidor; delimitar ambos antes de compartilhar chaves com outros serviços. |
| F03 | CSP avaliada, sem alteração nesta tarefa | O inicializador inline de tema exige nonce/hash e ensaio específico. Não adicionar uma política que quebre inicialização/hidratação nem apresentar CSP como proteção já implementada. |
| F04 | Node + Playwright/Chromium + axe | Exercita ações reais, primeira preferência de tema, persistência, teclado, estados de tela e contraste. Checks CI definidos não equivalem a execução remota comprovada. |
| F05 | Liveness independente e readiness com banco | Timeout de aquisição 2,5 s e validação 2 s; Compose usa curl com limite de 4 s, retries e período de partida. Falha temporária do banco não derruba liveness. |
| F06 | Dump custom PostgreSQL, criptografia age, checksum e restauração separada | Sem agendamento instalado ou destino externo criado. Metas, responsável e controles operacionais precisam de aprovação; ver runbook. |
| F07 | Release apenas de checkout limpo, SHA completo e IDs das imagens no manifesto | Build usa `git archive` do SHA, sem arquivos ignorados ou alterações concorrentes. Pacotes anteriores são preservados. Tags únicas de transporte offline são verificáveis por IDs, sem depender de registry. Checks devem pertencer ao SHA empacotado. |
| F08 | Consulta de saldos em lote e exportação com teto de 10000 linhas | O banco retorna no máximo 10001 linhas para detectar excesso. Nenhum CSV truncado. Aplicar filtros para reduzir volume. Não foi introduzido índice por suposição; ver benchmark e seus limites. |
| F09 | Erros JSON consistentes, UTC e constraints explicitamente conhecidas | Duplicidades e vínculos conhecidos são 409; corpo/parâmetros inválidos 400; integridade desconhecida permanece 500 sanitizado. Corridas são ensaiadas com barreiras e banco real. |
| F10 | Actions por SHA e imagens por digest verificados em fontes públicas | `dependency-pins.json` registra referências. Tipos Node alinhados ao runtime 24. Atualização mensal proposta, por mantenedor do repositório, via revisão e checks completos; atualizar extraordinariamente após aviso de segurança. |
| F11 | Contexto inicial preservado como histórico | Arquitetura, contratos, runbooks e relatório desta alteração são fontes atuais. |

## Compatibilidade monetária

Clientes devem ler respostas financeiras como strings. A alteração de resposta exige atualizar clientes coordenadamente; não há negociação de versão automática. O backend continua aceitando números JSON em entradas BigDecimal por compatibilidade temporária, mas clientes antigos que os produzem podem ter perdido centavos antes do envio. Novos clientes devem enviar strings; somente esse caminho garante a precisão ponta a ponta. ID, contadores, páginas, ano e mês continuam números. CSV mantém vírgula decimal, BOM e neutralização de fórmulas.

## Promoção e rollback

O mantenedor revisa os checks sanitizados, o SHA completo, as identidades de imagem e os checksums, ensaia migrações/restauração em ambiente isolado e só promove mediante autorização de deploy. Validar `SHA256SUMS` antes de carregar imagens e conferir `docker image inspect --format '{{.Id}}'` contra `IMAGE_IDENTITIES` após carregar. Manter pacote e backup anteriores. Reverter imagens exige esquema compatível; migração incompatível demanda recuperação separada do backup e plano aprovado para perda de dados posterior. Não executar rollback de banco automaticamente.
