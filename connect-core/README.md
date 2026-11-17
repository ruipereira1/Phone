# connect-core

Contratos partilhados pelos módulos do Fossify Connect. Não tem dependências externas.

| Ficheiro | O que é |
|---|---|
| `VideoCallProvider` | Interface de um transporte de videochamada, com a matriz de capacidades (FUNCIONA / PARCIAL / NÃO SUPORTADO / POR VALIDAR) |
| `ConnectServices` | Registo onde os módulos se encontram sem se conhecerem |
| `ModuleInitializer` | Base para o registo automático de cada módulo no arranque do processo |
| `FamilyContacts` | Leitura dos favoritos da agenda do Android (só leitura) |
| `SecureStore` | Armazenamento cifrado AES-256-GCM com chave no Android Keystore |
| `ConnectSettings` | Preferências não sensíveis (modo simplificado) |
