# connect-matrix

**Reservado, sem implementação.** Destina-se à videochamada Android↔Android (opção D), com o
Matrix só como sinalização e WebRTC como transporte de áudio e vídeo.

Não serve para o FaceTime: o Rustpush-Matrix tem apenas avisos de chamada FaceTime e não permite
fazer nem atender chamadas.

Fica como módulo independente por regra do fork. Quando for implementado, regista um
`VideoCallProvider` em `ConnectServices`, como faz o `connect-facetime`. Não acrescenta dependências
antes da PoC-D.
