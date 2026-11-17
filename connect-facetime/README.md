# connect-facetime

Videochamadas FaceTime através dos **links oficiais da Apple**
([Apple: entrar numa chamada FaceTime a partir de Android](https://support.apple.com/en-gb/109364)).
Não usa engenharia inversa, não precisa de conta Apple no tablet e não depende do rustpush
(SSPL, incompatível com a GPL; ver docs/RELATORIO-FASE1.md).

## Como funciona

| Sentido | Fluxo |
|---|---|
| Tablet → iPhone | O idoso toca em "Videochamada". O tablet abre o link persistente deste familiar e, se estiver configurado, envia uma SMS: "X está à sua espera…". O familiar abre o FaceTime e aceita a entrada. |
| iPhone → tablet | **Ainda não suportado.** A deteção de convites por SMS foi retirada: a permissão `RECEIVE_SMS` faz a Play Protect bloquear a instalação do APK. O ecrã ATENDER/REJEITAR já existe e vai ligar-se a outro canal de aviso. |

## Estado (classificação obrigatória)

O estado é declarado em `FaceTimeProvider.capabilities()` e mostrado ao cuidador. Nada está em
FUNCIONA enquanto não for testado com equipamentos reais (docs/TESTES.md).

## Limitações conhecidas

- O iPhone **não** recebe um toque FaceTime nativo (limitação da Apple).
- O iPhone que criou o link tem de admitir quem entra.
- Câmara ligada/desligada: só nos botões da página da Apple.
- O preenchimento automático do nome é experimental e depende da página da Apple.
- Os links FaceTime não se podem criar em iPhones comprados na China continental.

## Testes

`./gradlew :connect-facetime:testDebugUnitTest`: validação de links (domínio, esquema, porta,
fragmento) e extração a partir de SMS.
