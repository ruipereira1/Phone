# connect-simple-ui

Interface para idosos. Depende apenas de `connect-core` e não conhece os fornecedores de vídeo.

**Design de referência (Figma):** "Fossify Connect — Interface simplificada"
https://www.figma.com/design/Sajl5wF3Ovz2segmzXIBcn
As cores e medidas estão em `connect-core/src/main/res/values/connect_tokens.xml` e espelham as
variáveis "Connect" do Figma.

Há duas versões de cada ecrã, e o Android escolhe pelo tamanho do aparelho:

| Aparelho | Recursos | Figma |
|---|---|---|
| Telemóvel (< 600 dp) | `res/layout/` | ecrãs "M1 … M5" (412 × 915) |
| Tablet (≥ 600 dp) | `res/layout-sw600dp/` | ecrãs "1 … 5" (1280 × 800) |

As duas versões têm os mesmos IDs, por isso o código é um só.

| Ecrã (Figma) | Código | O que tem |
|---|---|---|
| 1 · Início | `HomeActivity` | Relógio e data grandes; aviso de chamada perdida com "Ligar de volta"; um cartão por familiar (favoritos) com **Ligar** e **Vídeo** diretos, num só toque. Telemóvel: uma coluna a deslizar, com fotografia redonda ao lado do nome. Tablet: 2 a 5 colunas |
| 2 · Chamada recebida | `IncomingCallActivity` | Fotografia redonda, "Ana está a ligar", **Rejeitar** e **Atender** (150 dp), por cima do ecrã de bloqueio, com toque durante até 60 s |
| 3 · A ligar | Ecrã da Fossify (GSM) | A adaptar numa iteração seguinte (ponto de extensão no `CallActivity`) |
| 4 · Videochamada | `connect-facetime` / `FaceTimeCallActivity` | Microfone · Mais baixo · Mais alto · **Desligar** |
| 5 · Configuração | `CaregiverActivity` | Abre com um toque longo de 3 s no relógio |

Simplificações em relação à versão anterior:
- o ecrã intermédio do contacto desapareceu: ligar passou de 2 toques para 1;
- não há botões pequenos nem engrenagem no ecrã inicial;
- as chamadas perdidas de familiares aparecem no topo, com "Ligar de volta";
- a letra é a Atkinson Hyperlegible, desenhada para baixa visão (OFL 1.1).
