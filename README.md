# Ultra Hertz

Controle de taxa de atualização para celulares Samsung Galaxy: modos **Padrão / Adaptativo / Alta**
com faixa de Hz configurável (inclusive abaixo do mínimo que o sistema oferece), regras por situação
e por app, e alguns extras.

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

## Instalação

> **Resumo:** o Ultra Hertz não instala tocando no APK ("App não instalado"). Você instala pelo computador,
> com um cabo USB, e depois dá uma permissão ao app. Não precisa de root. São 5 passos.

Neste guia, **ADB** é o programa do computador que conversa com o celular pelo cabo.
Os comandos abaixo são copiados e colados no **PowerShell** ou no **CMD** do Windows, **um de cada vez**.

### Passo 1: preparar o celular
1. Abra *Configurações > Sobre o telefone > Informações do software*.
2. Toque **7 vezes** em **Número da versão**. Isso liga as *Opções do desenvolvedor*.
3. Volte para *Configurações*, abra *Opções do desenvolvedor* e ligue a **Depuração USB**.
4. Conecte o celular ao computador por cabo USB. Na tela do celular, toque em **Permitir** na pergunta
   "Permitir depuração USB?".
5. Se já tiver o Ultra Hertz instalado, **desinstale** antes.

### Passo 2: preparar o computador
Você precisa do `adb.exe`. Há duas formas:
- **Tem o Android Studio?** O ADB já está instalado em
  `C:\Users\SEU_USUARIO\AppData\Local\Android\Sdk\platform-tools\adb.exe`
  (o `%LOCALAPPDATA%` dos comandos abaixo aponta para essa pasta). Nada a fazer.
- **Não tem?** Baixe o *SDK Platform-Tools* em
  https://developer.android.com/tools/releases/platform-tools, extraia e anote o caminho da pasta.
  Nos comandos abaixo, troque o início pelo caminho dessa pasta.

Para testar, rode o comando abaixo. Ele deve mostrar a versão do ADB (não precisa do celular):

PowerShell:
```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" version
```
CMD:
```
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" version
```

### Passo 3: descobrir a versão do Android do celular
Em *Configurações > Sobre o telefone > Informações do software*, veja **Versão do Android**.
- **Android 14 ou mais novo (14, 15, 16, 17...)** → use o **Passo 4A**.
- **Android 13 ou mais antigo (13, 12, 11...)** → use o **Passo 4B**.

Por quê? Do Android 14 em diante o sistema **bloqueia** apps feitos para versões antigas, e o Ultra Hertz
é um deles (veja [Por que targetSdk 22?](#por-que-targetsdk-22)). A flag `--bypass-low-target-sdk-block`
libera essa instalação. Em versões **mais antigas** essa flag não existe, e usá-la dá o erro
`Unknown option --bypass-low-target-sdk-block`.

### Passo 4: instalar o APK
Baixe o `.apk` na aba **Releases** deste repositório. Dica: no Explorador de Arquivos, clique com o botão
direito no APK e escolha **Copiar como caminho** para pegar o caminho certo. Nos comandos, troque
`C:\caminho\Ultra.Hertz.apk` pelo caminho do seu arquivo (mantenha as aspas).

#### 4A. Android 14 ou mais novo (com a flag)
PowerShell:
```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install --bypass-low-target-sdk-block "C:\caminho\Ultra.Hertz.apk"
```
CMD:
```
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" install --bypass-low-target-sdk-block "C:\caminho\Ultra.Hertz.apk"
```

#### 4B. Android 13 ou mais antigo (sem a flag)
PowerShell:
```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install "C:\caminho\Ultra.Hertz.apk"
```
CMD:
```
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" install "C:\caminho\Ultra.Hertz.apk"
```

O comando deve terminar com a palavra **`Success`**. Se aparecer outra coisa, veja [Problemas comuns](#problemas-comuns).

### Passo 5: dar a permissão ao app (igual para todos)
O app precisa da permissão `WRITE_SECURE_SETTINGS` para mudar a taxa de atualização. Só o ADB consegue concedê-la.
Este comando não mostra nada quando dá certo.

PowerShell:
```
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm grant com.devthiago.ultrahertz android.permission.WRITE_SECURE_SETTINGS
```
CMD:
```
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell pm grant com.devthiago.ultrahertz android.permission.WRITE_SECURE_SETTINGS
```

> **Atenção:** a permissão é perdida sempre que o app for **reinstalado ou atualizado**. Repita o Passo 5.

### Passo 6 (importante): deixar a bateria em "Sem restrições"

Sem isso, o Android pode **encerrar o Ultra Hertz em segundo plano** para economizar bateria. Quando isso
acontece, a taxa de atualização para de ser mantida: a economia de energia, as regras por app e a tela
desligada deixam de funcionar até você abrir o app de novo.

1. Abra *Configurações > Apps* e toque em **Ultra Hertz**.
2. Toque em **Bateria**.
3. Escolha **Sem restrições** (em alguns celulares aparece como "Não otimizar" ou "Irrestrito").
4. Se existir a opção **Dormir apps não usados** (ou "Colocar apps não usados em suspensão"), desligue
   para o Ultra Hertz.

Se mesmo assim ele for encerrado, procure em *Configurações > Bateria > Limites de uso em segundo plano* e
tire o Ultra Hertz de "Apps dormindo" e "Apps em suspensão profunda".
Pronto: abra o **Ultra Hertz** no celular. Se aparecer "Falta uma permissão", repita o Passo 5.


### Depois de instalar
- **Taxa por app:** na aba Apps, ative o serviço de acessibilidade.
- **Monitor na tela:** permita a sobreposição quando o app pedir.
- **Não ser encerrado em segundo plano:** *Configurações > Apps > Ultra Hertz > Bateria > Sem restrições*.

### Alternativa: instalar pelo Android Studio (quem compila o código)
1. Abra a pasta do projeto no Android Studio (use JDK 17 ou 21 no Gradle) e espere o Gradle sincronizar.
2. Menu **Run > Edit Configurations... > app > aba General > Installation Options > Install Flags**.
   - **Android 14 ou mais novo:** escreva `--bypass-low-target-sdk-block`.
   - **Android 13 ou mais antigo:** deixe **em branco**.
3. Selecione o celular na barra de cima e clique em **Run** (triângulo verde).
4. Faça o **Passo 5** (permissão). Pode ser pelo **Terminal** do próprio Android Studio (aba embaixo).
5. Para só gerar o APK: **Build > Build Bundle(s) / APK(s) > Build APK(s)**. O arquivo fica em
   `app/build/outputs/apk/debug/app-debug.apk` e você instala pelo Passo 4.

### Problemas comuns
| Mensagem ou situação | O que fazer |
|---|---|
| "App não instalado" ao tocar no APK | É normal. Instale pelo computador (Passo 4). |
| `Unknown option --bypass-low-target-sdk-block` | Seu Android é 13 ou mais antigo. Use o **Passo 4B** (sem a flag). |
| Erro de "target sdk"/"older version" no Android 14+ | Use o **Passo 4A** (com a flag). |
| `no devices/emulators found` ou `device unauthorized` | Confira o cabo, a Depuração USB e toque em **Permitir** na tela do celular. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Desinstale a versão antiga do app e instale de novo. |
| `adb` não é reconhecido | Use o caminho completo do `adb.exe`, como nos exemplos. |
| Aviso "app feito para uma versão antiga do Android" ao abrir | É esperado, por causa do `targetSdk 22`. |
| Alerta do Play Protect | O app vem de fora da loja; o código é aberto neste repositório. |

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
`--bypass-low-target-sdk-block`). To install a prebuilt APK from Releases: enable USB debugging, then run
`adb install --bypass-low-target-sdk-block ultrahertz.apk` and
`adb shell pm grant com.devthiago.ultrahertz android.permission.WRITE_SECURE_SETTINGS`
(repeat the grant after every reinstall). Experimental; not affiliated with Samsung. MIT licensed.
