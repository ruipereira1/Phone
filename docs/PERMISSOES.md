# Permissões do Fossify Connect

Princípio: as permissões mínimas, pedidas só quando a função é ligada.

| Permissão | Módulo | Quando é pedida | Para quê |
|---|---|---|---|
| Todas as da Fossify Phone (contactos, telefone, registo de chamadas, etc.) | `app` | Como na original | Chamadas normais, histórico, papel de marcador |
| `READ_CONTACTS` | `connect-simple-ui` | Ao abrir o ecrã de fotografias | Ler os favoritos (só leitura; nada sai do aparelho) |
| `CALL_PHONE` | `connect-simple-ui` | Na primeira chamada | Chamada telefónica a partir do ecrã simplificado |
| `POST_NOTIFICATIONS` | `connect-simple-ui` | Na configuração do cuidador (Android 13+) | Avisos de videochamada recebida |
| `USE_FULL_SCREEN_INTENT` | `connect-simple-ui` | Concedida aos marcadores; Android 14+ pode pedir confirmação | Chamada recebida em ecrã inteiro |
| `INTERNET` | `connect-facetime` | — | Página facetime.apple.com |
| `CAMERA`, `RECORD_AUDIO` | `connect-facetime` | Na primeira videochamada | Videochamada |
| `MODIFY_AUDIO_SETTINGS` | `connect-facetime` | — | Botão Microfone e volume |
| `SEND_SMS` | `connect-facetime` | Só se o cuidador ligar "Avisar por SMS" | Avisar o familiar de que o idoso está à espera |

## Dados guardados

| Dado | Onde | Proteção |
|---|---|---|
| Links FaceTime por contacto | SharedPreferences privadas | AES-256-GCM, chave no Android Keystore |
| Nome com que o tablet entra na chamada | Idem | Idem |
| Modo simplificado ligado/desligado | SharedPreferences privadas | Não é sensível |
| Último convite recebido (para evitar duplicados) | SharedPreferences privadas | Só um hash SHA-256 do link |

Não há credenciais Apple: o tablet **não** inicia sessão na Apple. As chamadas não são gravadas.
Os contactos nunca são enviados para fora do aparelho.

## Permissões que a app NÃO pode declarar

Fora da loja de apps, a Google Play Protect ("enhanced fraud protection") bloqueia a instalação de
apps que declarem `RECEIVE_SMS`, `READ_SMS`, um serviço de escuta de notificações ou um serviço de
acessibilidade. Como o Fossify Connect se instala por APK, não usa nenhuma destas. Por isso foi
retirada a deteção automática de convites FaceTime por SMS.
