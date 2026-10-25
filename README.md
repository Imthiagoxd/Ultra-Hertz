# Ultra Hertz

Controle de taxa de atualização para celulares Samsung Galaxy: modos **Padrão / Adaptativo / Alta**
com faixa de Hz configurável (inclusive abaixo do mínimo que o sistema oferece), regras por situação
e por app, e alguns extras.

Algum dia desses eu vou explicar como faz para instalar porem quem souber fazer, pode fazer

> **Status:** projeto experimental, escrito sem acesso a um compilador no ambiente de desenvolvimento
> inicial. Pode ter bugs. Testado só em aparelhos do autor. Use por sua conta e risco.
>
> Projeto independente. **Não tem ligação com a Samsung nem com outros apps de controle de taxa.**
> "Samsung" e "Galaxy" são marcas de seus donos.

## Recursos
- **Taxa:** interruptor principal, modo e faixa de Hz do uso normal, "evitar Padrão na economia".
- **Situações:** faixa própria para economia de energia, brilho baixo, câmera e tela desligada/AOD;
  economia de energia e pausa de sincronização ao apagar a tela (experimental).
- **Apps:** taxa por app (usa um serviço de acessibilidade que só vê qual app está aberto).
- **Extras:** proteger bateria (85%), informações da bateria, velocidade das animações,
  atividades redimensionáveis, monitor de Hz e de velocidade da rede.
- **Ajustes:** tema claro/escuro/sistema, recarregar perfis, reiniciar mods, diagnóstico.
- Só mostra taxas que o painel realmente tem; o modo Adaptativo some em telas sem taxa variável
  (dá para forçar em Ajustes).

## Requisitos
- Celular Samsung (One UI), Android 5.1+ (minSdk 22).
- Permissão `WRITE_SECURE_SETTINGS`, concedida uma vez via ADB. **Não precisa de root.**
- Android Studio (ou Gradle) para compilar.

## Compilar e instalar
1. Abra a pasta no Android Studio (JDK 17 ou 21 para o Gradle) e deixe sincronizar.
2. Desinstale versões antigas do app.
3. Em *Run > Edit Configurations > app > Installation Options > Install Flags* coloque:
   `--bypass-low-target-sdk-block`
   (ou, com o APK pronto: `adb install --bypass-low-target-sdk-block app-debug.apk`).
4. Conceda a permissão (de novo a cada reinstalação). No PowerShell do Windows:
   ```
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm grant com.devthiago.ultrahertz android.permission.WRITE_SECURE_SETTINGS
   ```
   Em Linux/macOS: `adb shell pm grant com.devthiago.ultrahertz android.permission.WRITE_SECURE_SETTINGS`
5. Abra o app. Para o taxa por app, ative o serviço na aba Apps. Para o monitor, permita a sobreposição.
6. Para não ser encerrado em segundo plano: Configurações > Apps > Ultra Hertz > Bateria > Sem restrições.

## Por que targetSdk 22?
Em versões novas do Android (e no One UI 9), apps com `targetSdk` alto têm a gravação de
`min_refresh_rate` / `peak_refresh_rate` recusada com
`You cannot keep your settings in the secure settings.` Com `targetSdk` 22 o sistema aceita.
Por isso o Android mostra um aviso de "app feito para versão antiga" e exige a flag
`--bypass-low-target-sdk-block` na instalação. Não dá para publicar assim na Play Store.

## Como funciona (resumo)
- Grava `refresh_rate_mode` (Settings.Secure) e `min_refresh_rate` / `peak_refresh_rate`
  (Settings.System). Quando o mínimo pedido é o piso do painel, grava 0 para o sistema descer o quanto puder.
- Um serviço em primeiro plano reaplica a taxa quando o sistema mexe nas chaves, quando o brilho muda,
  na economia de energia, ao ligar/desligar a tela, e a cada poucos segundos.
- Prioridade: tela desligada > regra do app > situação (economia > brilho baixo > normal) > modo geral.

## Personalizar
- Cores e formas: objeto `Pal` em `Ui.kt`.
- Valores padrão, tempos e listas: `Config.kt`.

## Limitações conhecidas
- Efeito da taxa com a tela desligada (AOD) varia por aparelho/versão.
- Não há seletor de resolução, "quick-doze" nem limites finos de carga da bateria.
- Código ainda não foi revisado em outras versões do One UI.

## Licença
[MIT](LICENSE)

---

### English (short)
Ultra Hertz lets Samsung Galaxy owners control the display refresh rate (Standard / Adaptive / High)
with a custom Hz range, per-situation and per-app rules. No root needed, but it requires
`WRITE_SECURE_SETTINGS` granted via ADB and is built with `targetSdk 22` (install with
`--bypass-low-target-sdk-block`). Experimental; not affiliated with Samsung. MIT licensed.
