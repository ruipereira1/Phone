# connect-simple-ui

Interface para idosos. Depende apenas de `connect-core` e não conhece os fornecedores de vídeo.

| Ecrã | O que tem |
|---|---|
| `HomeActivity` | Fotografias grandes dos favoritos da agenda, com o nome por baixo. 2 a 5 colunas, conforme a largura (telemóvel até tablet de 15″). |
| `ContactActivity` | Fotografia, nome e dois botões enormes: **Chamada** (telefone normal, através da Fossify) e **Videochamada** (o fornecedor configurado). |
| `IncomingCallActivity` | Videochamada recebida em ecrã inteiro: **ATENDER** e **REJEITAR**, com toque durante até 60 s e por cima do ecrã de bloqueio. |
| `CaregiverActivity` | Abre com um toque longo de 3 s na engrenagem. Família, configuração de vídeo por contacto, modo simplificado, uso como ecrã principal e acesso à Fossify Phone completa. |

Acessibilidade: texto de 32 a 48 sp, botões com pelo menos 120 dp, cores com contraste ≥ 4,5:1 e
descrições para o TalkBack. O botão "voltar" não fecha o ecrã principal, nem a chamada recebida
ou em curso.
