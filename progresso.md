# AnunciaAI — Progresso (fonte da verdade entre chats)

> App Android de crosslisting: foto → IA (Qwen-VL Plus) → revisão → publicar em Mercado Livre / eBay / Shopee / OLX / Facebook Marketplace / Enjoei.
> **Continuação:** novo chat com o nome "AnunciaAI" → dizer "continuar do progresso.md". Espec completa original: `~/.config/Hermes/composer-pastes/pasted_content_2026-09-26_19-00-46-478_e2a84b.txt`
>
> ⚠️ **ESTADO DO CHAT (medido no state.db em 30/09): 3.573 KB — 940% do limite de 380 KB.** 897 mensagens, 467 tool calls, 403 chamadas de API, 2,08M tokens de entrada. Pela convenção: **chat ENCERRADO — novo chat criado**; este arquivo tem tudo que o novo chat precisa.

## ⚠️ Convenção do chat (definida por Lucas em 26/09/2026)
1. Limite do chat: **380 KB**. Avisar o usuário quando estiver chegando perto.
2. Ao passar do limite: **PARAR a tarefa**, fazer resumo da seção corrente (tudo que foi feito nela + o que falta) e atualizar este arquivo.
3. Este arquivo é sempre atualizado com: coisas **feitas**, **planejadas** e **não terminadas**.

## Contexto do MVP
- Uso pessoal, single-user, APK sideload (sem Play Store). Público: Brasil.
- **Sem backend próprio na v1**: o app fala direto com as APIs e com a IA.
- Segurança: sessão das plataformas sem API fica só no CookieManager do WebView; tokens OAuth em EncryptedSharedPreferences; nada de credencial em log/console.

## Integrações
| Plataforma | Método |
|---|---|
| Mercado Livre | OAuth 2.0 + API REST (`/items`) |
| eBay | OAuth 2.0 + Sell API (Inventory API) |
| Shopee | Open API (exige aprovação de parceiro); fallback: WebView |
| OLX / FB Marketplace / Enjoei | WebView + JS injetado preenche e publica (sessão logada do usuário) |

Interface comum: `PublicadorDePlataforma` (módulos isolados por plataforma).
IA: interface `ServicoDeIA` — Qwen-VL Plus principal (prompt fixo, retorna JSON `{titulo, descricao, categoria_sugerida, preco_sugerido_reais, condicao}`); trocável por Gemini 2.5 Flash.

## Stack
Kotlin + Jetpack Compose · Room · Retrofit/OkHttp · WebView (`evaluateJavascript`) · EncryptedSharedPreferences · AGP 8.7.3 + Gradle 8.10.2 + Kotlin 2.0.21 · compileSdk 35 · minSdk 26.

## Modelo de dados (Room)
- `Item`: id, fotoUri, titulo, descricao, categoria, precoSugerido, precoFinal, dataCriacao
- `PublicacaoPlataforma`: id, itemId(FK), plataforma(enum), status(PENDENTE/PUBLICADO/ERRO), urlAnuncio?, mensagemErro?, dataPublicacao
- `ContaConectada`: id, plataforma, accessTokenCriptografado, refreshTokenCriptografado, expiraEm

## Fases (ordem obrigatória)
1. **Base**: projeto Kotlin+Compose, navegação, Room, captura de foto, item salvo
2. **IA**: Qwen-VL Plus + tela de revisão com dados reais
3. **APIs**: OAuth+publicação ML, eBay; tentativa Shopee Open API (documentar aprovação)
4. **WebView**: OLX, Facebook, Enjoei (login manual único + script preenche/envia)
5. **Polimento**: Meus anúncios consolidado, retry manual, APK assinado

**Aceite do MVP:** foto → anúncio IA → publicado em ML (API) + OLX (WebView), visível na tela de status, APK instalado direto no aparelho.

**Fora de escopo:** Play Store, monetização, multiusuário, mensagens de compradores, Correios.

## Ambiente (PCBOMBA — verificado 26/09/2026)
- Linux, OpenJDK 21.0.11; sem gradle/adb/SDK no sistema; **sem sudo**.
- Tudo dentro do projeto: `/mnt/SSD_Games_2/Projetos/AnunciaAI`
  - `android-sdk/` — SDK Android local (cmdline-tools/latest, platform-tools, platforms;android-35, build-tools;35.0.0)
  - `gradle-8.10.2/` — Gradle local
  - `.gradle-home/` — GRADLE_USER_HOME (caches fora da raiz)
- Internet OK (dl.google.com, github.com).

## Feito
- [x] 26/09 — Ambiente verificado (Java 21, 20GB livres no SSD, rede, sem sudo)
- [x] 26/09 — progresso.md criado + convenção do chat registrada (memória)
- [x] 26/09 — cmdline-tools + Gradle 8.10.2 instalados (zips em tools/)
- [x] 26/09 — SDK Android completo instalado (platform-tools, android-35, build-tools;35.0.0, licenças OK — 437MB)
- [x] 26/09 — Estrutura Gradle completa (settings/build/gradle.properties/wrapper, AGP 8.7.3 + Kotlin 2.0.21 + KSP)
- [x] 26/09 — FASE 1 código: Room (Item/PublicacaoPlataforma/ContaConectada + Daos + Repositorio), MainActivity, NavGraph (lista/captura/revisao/status/detalhe), captura de foto (câmera c/ FileProvider + galeria PickVisualMedia + EXIF)
- [x] 26/09 — FASE 2 código: ServicoDeIA (interface), SugestaoIA (parser tolerante + DoubleAdapter p/ preço "12,50"), QwenVLService (DashScope compatible-mode, base64, prompt fixo)
- [x] 26/09 — FASE 3 código: MercadoLivreApi (OAuth troca/renova token, POST /items, domain_discovery p/ categoria), MercadoLivrePublicador, EbayApi (Inventory API: item→offer→publish), EbayPublicador, TokenStore (EncryptedSharedPreferences), Credenciais/ChavesIA via BuildConfig←local.properties, OAuthCallbackActivity + Handler (deep link br.com.anunciaai://oauth/)
- [x] 26/09 — FASE 4 código: SessaoPublicacaoWeb (fila + PENDENTE no Room), LoginWebViewActivity (WebView + evaluateJavascript, até 10 tentativas, UA normal), scripts OLX/Facebook/Enjoei/Shopee + JsPublicar (clica submit), ShopeePublicador (stub documentado — Open API exige aprovação de parceiro)
- [x] 26/09 — TELAS: RevisaoScreen (IA auto + campos editáveis + checkboxes plataformas + publicar), StatusScreen (resultado por plataforma), ListaItensScreen, DetalheItemScreen (histórico + republicar + apagar)
- [x] 26/09 — local.properties (sdk.dir + chaves comentadas), proguard-rules.pro (kotlinx-serialization + ofuscação)

## Em andamento
- (nada — todos os builds passaram)

## Feito (adicional, depois do build 1)
- [x] 08/10 — **FIX PONTUAL: botão voltar em Conexões (código aplicado, teste incompleto):**
  - `ConexoesScreen` ganhou `onVoltar` + seta ArrowBack na top bar (mesmo padrão de Revisão/Status/Detalhe); NavGraph passa `nav.popBackStack()`. Commit pendente de push (ver "Não terminado").
  - IA e Sobre NÃO são telas — cards/linhas estáticas na própria Config (não há como ficar preso nelas; confirmar visual no emulador no chat novo).
  - **Emulador**: build OK, APK instalado, mas a verificação do tap em Conexões ficou INCOMPLETA (uiautomator dump saiu vazio; o app estava na tela Início — o tap na linha Conexões não registrou). RETOMAR: abrir Config → Conexões → conferir seta ← → tocar → confirmar volta. Emulador estava RODANDO com o build do fix instalado.
  - **DevCenter (instrução do Lucas, passo a passo recebido)**: redirect URI EXATA confirmada no código = **`br.com.anunciaai://oauth/ml`** (Manifest: scheme `br.com.anunciaai` + host `oauth`; MercadoLivreApi.REDIRECT_URI). NÃO usar `anunciaai://oauth/mercadolivre`. Escopos: read, write, offline_access. Copiar APP ID/Secret → local.properties como ANUNCIAAI_ML_CLIENT_ID/ANUNCIAAI_ML_CLIENT_SECRET → rebuild → testar OAuth no emulador.
- [x] 05/10 — **FIX PONTUAL: fundo claro em Início/Vitrine/Config (emulador pegou):**
  - Causa raiz DUPLA: (a) as 3 telas eram `Column` puro sem fundo (Mensagens tinha `Scaffold(containerColor=CorFundo)`, elas não); (b) o tema XML era `Theme.Material.Light.NoActionBar` — windowBackground claro aparecendo por baixo do Compose. Corrigido: Column raiz ganhou `.background(CorFundo)` nas 3 + `themes.xml` trocou pra `Theme.Material.NoActionBar` com `windowBackground=#12151A` (colors.xml: fundo_escuro).
  - **Verificação no emulador com análise de PIXEL** (screencap + decode PNG em Python puro): Início/Vitrine/Config = fundo exato RGB(18,21,26)=#12151A em todos os pontos de amostra (o RGB(27,31,38) perto do topo é a surface da TopAppBar, correto). Commit `3bea0fa`.
- [x] 03/10 — **EMULADOR DE TESTE NO PC (KVM — CAMINHO A):** `/dev/kvm` existe com acesso real (ACL; lucas fora do grupo kvm mas abre o device). Este "servidor" é o próprio desktop do Lucas (X11 DISPLAY=:0) — emulador abre JANELA DIRETO no monitor, sem túnel/VNC. Instalado: emulator + system-images;android-34;google_apis;x86_64 + platforms;android-34 (~2,3GB no SDK do projeto). AVD `anunciaai` (Pixel 5, Android 14, userdata em ~/.android/avd na ext4). Scripts: **`emulador.sh start|stop|status`** (boot ~1min, -no-snapshot) e **`testar.sh [--no-build]`** (gradle assembleDebug → adb install -r → am start — UM comando após cada mudança). Primeira rodada: build+install+launch OK, MainActivity em foco, 0 crashes. Nota: câmera física não existe no emulador (testar fluxo com fotos da galeria); scanner EAN sem código físico.
- [x] 03/10 — **v12.0 (BUILD 36) — REESCRITA TOTAL (spec consolidação, 6 telas do zero):**
  - **Tema v12 de volta ao ESCURO**: #12151A fundo, #1B1F26 superfície, destaque ÚNICO #00C896, texto #F2F2F0 SEMPRE em preço/títulos, sec #8B909A, erro #FF5470, Manrope 400/600/800, título tela 28/800. Paleta v11 clara EXTINTA do binário (F4F4F7/A3E635/65A30D = 0 ocorrências). SystemBarStyle.dark de volta.
  - **NavCapsula (arquitetura nova)**: cápsula glass (Surface RoundedCornerShape(32.dp), padding 16/12dp, blur 16.dp API 31+ com fallback sólido Black60% <31) com FAB "+" ÚNICO — mora no NavGraph RAIZ (um Box raiz, cápsula só nas 4 abas), NUNCA mais declarada por tela. BarraDockNova.kt + FabCentral + brilhoNeon/BrilhoNeon.kt DELETADOS. Telas perderam bottomBar/onNavBottom de vez.
  - **Tela 1 Início (do zero)**: hero card gradiente #00C896→#0A5C6E cantos 20dp com "+ Novo item" DENTRO; vendas por plataforma SÓ CONECTADAS com selo % verde/vermelho; "Itens recentes" em LazyVerticalGrid(Fixed(2)) DE NÍVEL SUPERIOR (GridItemSpan p/ hero/seções) — zero aninhamento; preço CorTexto peso 800; badges sobrepostos. Zero decoração de fundo.
  - **Tela 2 Vender (do zero)**: câmera real + moldura de BORDA PONTILHADA (dashPathEffect) central; "Tirar foto" sólido destaque + "Escolher da galeria (até 10)" texto SUBLINHADO (sem pill); scanner EAN ML Kit no botão QrCodeScanner; tela escura integral. Análise reescrita com PILHA DE CARDS EMPILHADOS (offset fixo (i*26).dp, card em foco com zIndex+scale+elevação, check verde spring, revelação 650ms/card) — lógica v11.1 mantida (persiste antes de navegar, gerarComContrato retry).
  - **Tela 3 Revisar (do zero)**: campos #1B1F26 sem borda (tom só no foco), estado inicial DIRETO do item persistido (chip condição = valor EXATO da IA via remember(item?.id)), 4 chips fixos em FlowRow, categoria+preço Row weight(1f) maxLines=1+Ellipsis, chips "Onde publicar" com COR DA MARCA (preenchido/contorno), carrossel miniaturas alinhadas + DOTS, botão Gerar persiste condição no Room também.
  - **Tela 4 Conexões (do zero)**: linhas com separador 1px (sem card/sombra), badge quadrado cor de marca, status por TEXTO (Conectado destaque/Não conectado cinza), card IA no topo mantido.
  - **Tela 5 Mensagens (do zero)**: mesma estrutura de linha (badge+nome+texto, separador 1px), prévia + HORÁRIO nas perguntas ML (API real), "Abrir conversa" nas WebView; diálogo de resposta ML mantido; pills de filtro REMOVIDAS (spec não pede).
  - **Config/Vitrine/Status/Detalhe/EstadoVazio**: reescritos pro tema v12 (sem dock, sem FAB, cores do tema). EstadoVazio neutro #23272E.
  - **Verificação binária 18/18**: 7 cores v12 presentes, 3 da v11 AUSENTES, 8 strings da spec, BarraDockNova sumiu do mapping, FabCentral 0 usos, NavCapsula/ItemCapsula/FabDaCapsula no mapping (536), versionCode 21 / v12.0, assinatura 5bb25361, testes unitários 4/4 PASS. APK 24MB.
- [x] 03/10 — **v11.1 (BUILD 35) — PACOTE DE CORREÇÕES DEFINITIVAS (6 fixes do prompt do Lucas):**
  - **FIX 1 (FAB duplicado — bug de arquitetura)**: confirmado no código — a cápsula já tinha o "+" lime integrado E cada Scaffold redeclarava `floatingActionButton = { FabCentral() }` = dois "+" sobrepostos em 4 telas. Corrigido: `FabCentral` EXTINTO (função removida, 0 usos), `floatingActionButton`/`FabPosition` removidos dos 4 Scaffolds (Lista/Vitrine/Mensagens/Config). O "+" agora existe UMA vez, integrado na cápsula (BarraDockNova). **Glass**: fundo translúcido 69% + `Modifier.blur(16.dp)` na camada de fundo (API 31+; fallback 94% opaco sem blur no <31) + sombra 6dp no círculo lime. Bônus: na v11 a sombra da cápsula vinha DEPOIS do clip (invisível) — corrigido.
  - **FIX 2 (condição da IA desconectada da UI)**: causa raiz era MAIS FUNDA — a entidade `Item` NÃO TINHA campo `condicao`; a AnaliseScreen salvava título/preço/categoria e DESCARTAVA a condição; a Revisão reconstruía a SugestaoIA sem ela → chip caía sempre em "bom estado". Corrigido em 3 camadas: (a) DB **v5**: `Item` ganha `condicao` + `ean`; (b) AnaliseScreen persiste `s.condicaoNormalizada` no item (e leitura via `itemNow()` — antes o save era PULADO se o Flow do Room ainda não tivesse emitido); (c) RevisaoScreen inicializa os estados DIRETAMENTE do item persistido via `remember(item?.id)` — `condicao = item.condicao normalizada`, nunca constante. LaunchedEffect de auto-preenchimento removido (era a fonte da dessincronia). `publicar()` também persiste a condição editada. Detalhe mostra "Condição: X".
  - **FIX 3 (prompt contraditório)**: PROMPT_MULTI (NVIDIA, usado também pelo Gemini) + PROMPT_SISTEMA (Qwen) ganham REGRAS ANTI-CONTRADIÇÃO ("nunca atributos tecnicamente contraditórios; se incerto, OMITA o atributo; descrição coerente com a condição") + contrato das 4 strings exatas. Qwen alinhado ao contrato novo (taxonomia ' > ', preco_sugerido + comparativo). NOVO: `ServicoDeIA.gerarComContrato()` — wrapper com RETRY (2 tentativas) quando a condicao vem fora das 4 exatas; usado na Análise e na Revisão. **4 testes unitários (ContratoCondicaoTest): PASS** — inclui "IA devolve 'novo' → chip 'novo' atravessa íntegro" e "IA erra → retry devolve contrato válido".
  - **FIX 4 (vitrine quebrada + contraste)**: causa raiz do card espremido = `LazyVerticalGrid` ANINHADO dentro de `item {}` do LazyColumn (Home) com altura calculada na mão. Corrigido: linhas do grid viram itens do próprio LazyColumn via `chunked(2)` + `Row(weight(1f))` por célula + célula fantasma na linha ímpar. Preço: `#EDEFF7` (branco da v10 sobre card BRANCO = invisível) → `CorTexto #17171F` peso 800 na Home; na Vitrine preço gigante + categoria em Row com `weight(1f)`, `maxLines=1`, `Ellipsis` (não quebra mais linha). `brilhoNeon` (círculos desfocados) REMOVIDO da Home e da Vitrine (0 usos restantes).
  - **FIX 5 (OLX/UA)**: UA Chrome atualizado 129 → **141** (Pixel 8/Android 14), aplicado em `configurarWebView()` antes de qualquer `loadUrl` (ordem já correta confirmada).
  - **FIX 6 (scanner EAN)**: já funcionava desde a v8 (scanner ML Kit → rota `?ean=` → PROMPT_EAN na IA); agora o EAN também PERSISTE no Item (DB v5) — o dado não se perde se o app morrer entre captura e análise.
  - **Verificação binária (AnunciaAI-11.1.apk)**: versionCode 20 / v11.1; strings do retry ("fora do contrato"), anti-contradição, UA Chrome/141, grid e EAN presentes no dex; `FabCentral` AUSENTE do mapping.txt (um único "+" comprovado); assinatura 5bb25361 (mesma). APK 24MB.
- [x] 30/09 — **v11.0 (BUILD 34) — REBOOT VISUAL "Figma Bike Shop"** (Lucas rejeitou a v10: "só mudou a cor pra roxo"):
  - **App agora é CLARO**: fundo #F4F4F7, cards BRANCOS, texto quase-preto #17171F, statusbar clara (SystemBarStyle.light). Adeus tema escuro/índigo — 0 resíduo no dex.
  - **Nav CÁPSULA FLUTUANTE** (Figma): BottomAppBar extinta de vez — cápsula RoundedCornerShape(50%), fundo #1E1E24, sombra 18dp, flutuando com padding 24/16dp. FAB verde-lime #A3E635 INTEGRADO no centro (52dp, maior que os itens), sem notch.
  - **Paleta**: VerdeNeon #A3E635 (FAB/chips ativos), Destaque #65A30D (lime legível sobre claro), cápsula #1E1E24, erro #FF3B30.
  - **Vitrine catálogo de luxo**: cards brancos 24dp + sombra, foto 150dp arredondada 18dp dominando o card, nome cinza pequeno + **preço GIGANTE headlineSmall ExtraBold quase-preto**.
  - **Chips pílula na Revisão**: ativo = fundo lime neon + texto escuro; inativo = border fino cinza + texto cinza. FlowRow mantido.
  - **Verificado no binário**: cores bike 3/3 presentes, aurora 0/4 resíduos; versionCode 19 / v11.0; nvapi- OK; assinatura 5bb25361. APK: AnunciaAI-11.0.apk (24MB), servidor :8899.
- [x] 30/09 — **v10.0 (BUILD 33) — RUPTURA DE DESIGN: "Aurora Tech"** (Lucas: "vc está preso nesse design, muda ele"):
  - **Paleta NOVA inteira**: fundo **azul-abissal #0A0E1A** (não mais cinza #12151A), superfície azulada **#141B2E**, destaque **índigo elétrico #7C6BFF** (adeus verde-menta #00C896), acento secundário **ciano aurora #4FD8EB**, gradiente do resumo índigo→**violeta profundo #2E1B6B**, textos azulados #EDEFF7/#8A93AD. Containers do tema (primaryContainer/onPrimary etc.) recalculados pro índigo.
  - **Verificação binária**: as 5 cores novas presentes no dex; as 5 antigas (00C896/12151A/1B1F26/0A5C6E/06231B) **AUSENTES** — troca 100%, zero resíduo. brilhoNeon agora emite glow índigo. versionCode 18 / v10.0, assinatura 5bb25361, nvapi- OK.
  - Todas as telas herdam automaticamente (cores via tema); hardcoded remanescentes trocados via sed em 10 arquivos.
- [x] 30/09 — **v9.0 (BUILD 32) — aba Configurações, Perfil completo com redes, badge de chat, dock 5 itens:**
  - **Dock v9**: agora 5 itens — **Início | Vitrine | [entalhe/FAB] | Chat | Config**. "Mensagens" virou "Chat" com **badge de não-lidas** (EstadoInbox StateFlow alimentado pela contagem de perguntas ML; badge "9+" quando >9). Micro-press nos itens (indication=null).
  - **ConfigScreen (nova)**: hub com Perfil, Conexões (absorvidas pra cá), estado da IA e Sobre. Rotas novas CONFIG/CONEXOES/PERFIL_EDIT.
  - **PerfilScreen vira o perfil de verdade**: avatar 116dp com anel verde + botão Edit (foto da galeria via PickVisualMedia, copiada pro storage interno), campos **Nome, Nick, Bio + redes: Instagram, WhatsApp, Telegram, TikTok** — tudo salvo no Room (**DB v4**, tabela perfil_usuario, PerfilDao + repositório). Confirmação "Salvo ✓" animada.
  - **Fabs padronizados**: `FabCentral` reutilizável (ConfigScreen) substituiu os FABs duplicados de Início/Vitrine/Mensagens; todos os docks via `BarraDockComBadge`.
  - **Verificado**: versionCode 17 / v9.0; strings (Configurações, Salvar perfil, redes, Chat/Config, perfil_usuario) no dex; nvapi- OK; assinatura 5bb25361. APK 24MB (AnunciaAI-9.0.apk).
- [x] 30/09 — **v8.0 (BUILD 31) — SUPER PROMPT: 5 PILARES implementados:**
  - **PILAR 1 (Dock premium)**: BottomAppBar oficial com **NotchShape** — Shape custom (createOutline, sem Canvas flutuante) que recorta entalhe circular central via clip(30dp); FAB ancorado pelo Scaffold `FabPosition.Center` encaixa no círculo. Ícones com animateFloatAsState: ativo cresce **15%** (spring bouncy) + cor 300ms.
  - **PILAR 2 (Neon Glassmorphism)**: novo modificador `brilhoNeon()` — glow radial verde-menta **10% de opacidade** por trás dos cards (Home no card de resumo, Vitrine em todos) + `fundoGradienteEscuro`. Cards **20dp** de raio. Vitrine: itens surgem **um a um** com AnimatedVisibility (slideInVertically + fadeIn, stagger 60ms/item).
  - **PILAR 3 (fim das sobreposições)**: AnaliseScreen reescrita — Column `spacedBy(16.dp)` SEM sobreposição; cada card surge com spring (slideInVertically+fadeIn) e **check verde animado** (scale spring 0→1). Prompt com **taxonomia exata** ("Eletrônicos > Hardware > Periféricos > Mouses", separador ' > ', 4 níveis, taxonomia ML/OLX).
  - **PILAR 4 (Scanner EAN ML Kit)**: dep `barcode-scanning:17.3.0` (bundled). Botão QrCodeScanner junto à câmera liga/desliga `ImageAnalysis` (re-bind via AndroidView.update); ao ler EAN-13 mostra chip verde "EAN ✓", e o código viaja pela rota `analise/{itemId}?ean=...` até a IA: novo método `ServicoDeIA.gerarAnuncioComEan()` + PROMPT_EAN (modelo EXATO do fabricante, temperature 0.2). APK 2,7→24MB (modelo ML Kit embutido).
  - **PILAR 5 (HTTP error)**: `onReceivedHttpError` capturado no WebView (login E publicação) — HTTP 403/anti-bot da OLX agora mostra a tela amigável "A plataforma respondeu HTTP NNN — pode estar bloqueando automação" com Tentar novamente (UA Chrome real já estava da v4.1).
  - **Verificado**: versionCode 16 / v8.0; strings dos pilares no dex; 4 entradas mlkit no APK; nvapi- OK; assinatura 5bb25361. APK: AnunciaAI-8.0.apk (24MB).
- [x] 29/09 — **v7.0 (BUILD 30) — correção da BASE: crash, duplicações e IA comparativa:**
  - **CAUSA DO CRASH DO PERFIL encontrada**: scroll aninhado — PerfilScreen tem `verticalScroll` e a ConexoesScreen embutida TAMBÉM tinha. Scroll vertical dentro de scroll vertical = exceção imediata no Compose. Corrigido: ConexoesScreen `embutida` rola pelo pai.
  - **Botão "+ Novo item" do TOPO removido** do card de resumo (conflitava com FAB central e "Explorar itens"). Entrada única = FAB central do Scaffold (`FabPosition.Center`) — verificado no dex: string "Novo item" sumiu.
  - **Vitrine confirmada**: já era `LazyVerticalGrid(GridCells.Fixed(2))` com contentPadding 16dp e arrangement 12dp (atende o item 2 sem mudança).
  - **IA v7 — contrato novo**: prompt reescrito exigindo JSON `{titulo, descricao, preco_sugerido, preco_comparativo_mercado, condicao}` com condição ESTRITAMENTE em "novo"/"como novo"/"bom estado"/"marcas de uso". SugestaoIA ganhou `precoSugerido` + `precoComparativoMercado` + `condicaoNormalizada` (normalizador tolerante) + `melhorPreco`. Item (Room) ganhou `precoComparativoMercado` (DB v3, destructive). AnaliseScreen salva os dois preços; Revisão mostra "Preço médio no mercado: R$ X" e o CHIP DE CONDIÇÃO JÁ VEM MARCADO com o valor exato da IA (4 chips nos valores exatos).
  - **TESTE E2E REAL**: novo prompt enviado à API NVIDIA com foto → HTTP 200 em 7,1s, JSON no contrato exato, condicao="novo" dentro das 4 permitidas, preco_comparativo presente.
  - **Verificado**: versionCode 15 / v7.0; strings v7 no dex; nvapi- OK; assinatura 5bb25361. APK: AnunciaAI-7.0.apk.
- [x] 29/09 — **v6.1 (BUILD 29) — CORREÇÃO DO CRASH v6.0 + estabilidade:**
  - **CAUSA DO CRASH**: o dock custom da v6.0 usava Canvas/Path/arcTo manual dentro de Box com FAB posicionado à mão — sobreposição quebrada no Início (2 FABs) e geometria frágil. REMOVIDO por completo.
  - **PASSO 1**: FAB antigo do canto inferior direito REMOVIDO do Início (era duplicado com o central — erro meu da v6.0). Único FAB do app = "Vender" central.
  - **PASSO 2**: `BarraDockNova` com **BottomAppBar OFICIAL do Material 3** — 4 itens com weight + `Spacer(weight 1f)` central abrindo o vão; FAB 58dp verde ancorado pelo **Scaffold com FabPosition.Center** em cada tela (Início/Vitrine/Mensagens/Perfil), sobrepondo o vão com o recorte nativo do M3. Animações mantidas: escala spring 1.0→1.15 + cor 300ms.
  - **PASSO 3**: aba ativa em `rememberSaveable` sincronizada com a rota via LaunchedEffect — rota continua sendo a fonte da verdade; sem recomposição infinita.
  - Labels "Vitrine" agora com espaço real (weight 1f cada lado; não espreme mais).
  - **Verificado**: versionCode 14 / v6.1; NavigationBar ausente; BottomAppBar confirmado no mapping.txt do R8; nvapi- OK; assinatura 5bb25361. APK: AnunciaAI-6.1.apk.
- [x] 29/09 — **v6.0 PONTO 1 (BUILD 28) — Custom Bottom Dock com FAB central (NavigationBar padrão EXTINTA):**
  - Novo componente **BottomDockCurvo**: barra desenhada com **Path custom em Canvas** — retângulo com vão semicircular central (arcTo, curvas cúbicas de entrada/saída) que acomoda o **FAB "Vender" 64dp verde-menta flutuando sobreposto** no centro.
  - Itens (esq→dir): **Início | Vitrine | [vão/FAB] | Mensagens | Perfil** — exatamente a spec.
  - Animações: **escala do ícone ativo 1.0→1.15 com spring bouncy** (animateFloatAsState), **cor animada 300ms** cinza→verde-menta (animateColorAsState), transições de tela fade+slide já ativas do NavHost.
  - **Telas novas**: VitrineScreen (grid 2 colunas do catálogo inteiro, com selos sobre a foto) e PerfilScreen (IA ativa + Conexões absorvidas da antiga aba — ConexoesScreen virou `embutida=true`).
  - BarraInferior.kt **deletada**; rotas: CONEXOES removida, VITRINE/PERFIL adicionadas; empty state Mensagens aponta pro Perfil.
  - **Verificado no binário**: versionCode 13 / v6.0; `androidx/compose/material3/NavigationBar` AUSENTE do dex (regra de ouro cumprida); strings Vitrine/Perfil/Vender OK; nvapi- OK; assinatura 5bb25361.
- [x] 29/09 — **v5.4 (BUILD 27) — micro-interações premium + interface de credenciais ML:**
  - **Transições de tela** no NavHost: fade+slide horizontal sutil (enter 220ms, exit 180ms, pop reverso).
  - **Count-up animado** do "Valor em estoque": 0 → valor real com spring (DampingRatioLowBouncy/StiffnessLow).
  - **FAB com spring** de escala; **balão da câmera flutuando** (infinite transition, ±4dp, 1400ms reverso).
  - **PASSO 2 (interface credenciais ML)**: cartão na aba Conexões mostrando estado do OAuth — ícone Key/KeyOff + "Credenciais do Mercado Livre prontas" (verde) ou "Faltam as credenciais" com instrução completa do DevCenter (developers.mercadolivre.com.br → ANUNCIAAI_ML_CLIENT_ID/SECRET → rebuild). Lê do BuildConfig — nenhum segredo em runtime.
  - **Verificação**: versionCode 12 / v5.4; strings do cartão OK no dex; animações confirmadas via mapping.txt do R8 (BalaoDica linhas 329-340 mapeadas — R8 ofusca nomes de API, strings de usuário passam). Assinatura 5bb25361, nvapi- OK.
- [x] 29/09 — **v5.3 ITENS 2+3+4 (BUILD 26) — acabamento premium:**
  - **ITEM 2 — Pills de filtro nas Mensagens**: LazyRow no topo com "Todas / Não lidas / Ofertas". Ativo = fundo destaque #00C896 + texto branco; inativo = cinza-escuro #23272E + texto claro. Filtro real: Não lidas = perguntas do ML (API), Ofertas = plataformas WebView, Todas = inbox completo.
  - **ITEM 3 — Empty states premium**: novo componente `EstadoVazio` (reutilizável) — ícone grande 64dp minimalista, título em destaque, subtítulo explicativo e botão secundário LARGO fundo cinza #2E3440 + texto branco. Aplicado em Mensagens (ícone envelope, "Nada por aqui ainda" + "Conectar plataformas" que navega pra aba Conexões) e na vitrine (ícone caixa aberta Inventory2, "Explorar itens" que abre a câmera).
  - **ITEM 4 — Vitrine em GRID de 2 colunas**: LazyVerticalGrid substituiu a LazyRow horizontal (spec revogou o formato anterior). Foto proeminente 130dp com **selos das plataformas sobrepostos no canto da própria foto** (pilha tipo moedas), abaixo Nome (linha 1, cinza claro #8B909A) e Preço (linha 2, branco #F2F2F0 negrito).
  - **Verificado no binário**: versionCode 11 / v5.3, strings dos 3 itens OK, LazyVerticalGrid no dex, nvapi- OK, assinatura 5bb25361.
- [x] 29/09 — **v5.2 ITEM 1 (BUILD 25) — Câmera Imersiva full-screen (CameraX in-app):**
  - Trocada a câmera do SISTEMA (TakePicture) por **CameraX in-app**: preview live ocupando a tela toda (moldura tracejada e tela "Vender" de formulário REMOVIDAS).
  - **4 cantoneiras brancas finas** (3dp, 36dp de comprimento, cantos arredondados) no centro como guia de enquadramento — Canvas custom.
  - **Balão de diálogo branco** com cantos 16dp + seta triangular apontando pra baixo (pro botão de captura): "Fotografe o item" (negrito) / "A IA fará a avaliação e a precificação".
  - Botão de captura 76dp (anel branco + centro escuro) disparando takePicture via gatilho hoisted; botão galeria circular translúcido; X pra fechar. Foto capturada → correção de rotação → análise da IA (fluxo v4 preservado).
  - Deps novas: camera-core/camera2/lifecycle/view 1.4.0. APK cresceu 1,9→2,7MB (CameraX).
  - **Verificado no binário**: versionCode 10 / v5.2, strings do balão OK, ProcessCameraProvider no dex, string antiga "Escolher da galeria" removida, nvapi- OK, assinatura 5bb25361.
- [x] 29/09 — **v5.1 (BUILD 24): REVERSÃO DO RENAME — app volta a ser AnunciaAI.** Lucas desfez o equívoco ("não é pra mudar de nome, é pra continuar anunciaai"). Label strings.xml + top bar revertidos, verificado no arsc (zero resíduo de "VendeAi"). versionCode 9 / v5.1. NOTA: durante a entrega o SSD_Games_2 DESMONTOU sozinho DUAS VEZES (NTFS sda1 instável; sistema reiniciou antes) — remontado via `udisksctl mount -b /dev/sda1`, nada perdido. Se sumir de novo: remontar com o mesmo comando.
- [x] 29/09 — **v5.0 PASSO 1 (BUILD 23) — rename VendeAi + audit do dashboard contra a spec:**
  - **Rename visível**: app_name e top bar agora **VendeAi** (verificado no arsc + dex). Pacote segue `br.com.anunciaai` de propósito — trocar applicationId quebraria upgrade da assinatura e o deep link OAuth já registrado.
  - **Itens recentes agora é scroll HORIZONTAL** (spec pedia; era vertical): LazyRow de cards 150dp com foto 110dp no topo, título, preço destaque e pilha de selos de plataforma.
  - **Card de resumo com brilho GLOSSY**: faixa de luz branca (alpha 0.22→0) sobre o gradiente #00C896→#0A5C6E, cantos 20dp; "+ Novo item" virou FilledTonalButton dentro do card.
  - **Vendas por plataforma lista TODAS as 6 marcas** (era só conectadas): badge quadrado da marca + valor + selo % com seta (verde #00A97A / vermelho #FF5470).
  - FAB verde-menta "+" no canto, acima da bottom bar (único ponto de entrada) — mantido.
  - **Verificado no binário**: versionCode 8 / v5.0, label 'VendeAi' no arsc, strings OK, nvapi- no dex, assinatura 5bb25361.
- [x] 29/09 — **v4.1 (BUILD 22) — correções de bugs reais da v4.0 + nivelamento visual:**
  - **Bug OLX WebView (ERR_HTTP_RESPONSE_CODE_FAILURE)**: WebView agora usa **UA de Chrome Android real** (Pixel 7/Chrome 129 — o default com "; wv" é bloqueado por anti-bot), domStorage+database+cache OK, `setAcceptThirdPartyCookies(true)`, pré-visita ao Google (sessão de navegação "normal" antes da página de anúncio), `onReceivedError` só no main frame, e **tela de erro amigável** ("Não consegui abrir a plataforma" + "Tentar novamente" + aviso de bloqueio de automação) em vez do erro cru. Aplicado a login E publicação (OLX/FB/Enjoei/Shopee). Anti-duplicação de tentativas via tag (ids.xml novo).
  - **Bug "l ivros" (texto quebrado em L maiúsculo)**: causa raiz = fonte VARIÁVEL + variationSettings (bug de shaping no Android). Trocada por **Manrope em 5 pesos ESTÁTICOS** (regular/medium/semibold/bold/extrabold, ~97KB cada, baixados de repo fonte real). Variável antiga removida do APK (verificado: 5 TTFs estáticos, nenhum de 164KB).
  - **Bug chip condição vertical ("ma/rca/s")**: chips saíram de Row fixo para **FlowRow** — quebra por palavra, largura pelo conteúdo.
  - **Bug topo claro na "Analisando fotos"**: `enableEdgeToEdge(SystemBarStyle.dark(CorFundo))` no MainActivity — statusbar/navbar sempre #12151A em qualquer tema do sistema; tela também ganhou statusBarsPadding.
  - **Nivelamento Vender/Revisão**: campos com fundo elevado #1B1F26 SEM borda (só tom mais claro no foco), chips "Onde publicar" com **COR DA MARCA** (preenchido na seleção, contorno da marca quando não; eBay contorno branco, ML texto azul #2D3277), aviso de plataforma sem conexão com **ícone de alerta**, carrossel com miniaturas 96dp alinhadas + indicador **dots** (era "1/1") + botão adicionar do tamanho da foto com X de remover sobreposto.
  - **Verificado no binário**: versionCode 7 / v4.1, UA Chrome + setAcceptThirdPartyCookies + strings de erro no dex, 5 TTFs estáticos, mesma assinatura 5bb25361.
- [x] 29/09 — **v4.0 (BUILD 21, de primeira) — alinhado à referência SellRaze:**
  - **Badges de marca coloridos** (v4): quadrado 12dp, fundo na cor REAL da marca, inicial branca (ML: azul #2D3277 sobre amarelo; eBay: quadrado MULTICOR original desenhado com 4 retângulos — não achatado; OLX #7C1FD6, FB #1877F2, Enjoei #FF2D78, Shopee #EE4D2D). Cor de marca = identidade em todas as listas; estado continua por TEXTO.
  - **Início = dashboard**: card de resumo com GRADIENTE #00C896→#0A5C6E, cantos 20dp, "Valor em estoque" (soma real dos itens não vendidos), contagem de itens/vendidos e botão "+ Novo item" dentro do card. Seção "Vendas por plataforma" (só conectadas; valor vendido + selo % vs semana anterior com seta verde/vermelha; "Sem vendas ainda" quando vazio, sem quebrar layout). Seção "Itens recentes" (ordenados por data, foto real, preço destaque, PILHA de minibadges de plataforma sobrepostos como moedas). Busca movida pra DENTRO da seção.
  - **Tela Vender → Análise com PILHA DE CARDS (a estrela)**: nova rota analise/{itemId} entre captura e revisão. 5 cards no leque (Categoria→Título→Descrição→Preço→Condição), ícone por campo, card ativo na frente (zIndex+scale+elevação), preenchidos ganham check verde #00C896 + valor, revelação sequencial 650ms/card. IA salva o resultado no item ANTES da revisão (revisão não re-chama a IA — sem custo dobrado). Botão "Ver anúncio" → revisão editável.
  - **Verificado no binário**: versionCode 6 / v4.0, cores v4 + petróleo + azul ML presentes, strings do dashboard e da análise OK, nvapi- no dex, assinatura 5bb25361 (mesma).
- [x] 29/09 — **v3.0 REDESIGN COMPLETO (BUILD 20) — sistema de design novo, saiu do Material genérico:**
  - **Tema**: fundo #12151A, superfície única #1B1F26 (um tom acima), UMA cor de destaque #00C896 (verde-menta) em CTA/aba ativa/status positivo, texto #F2F2F0/#8B909A, neutro #4B505B, erro #FF5470. Mostarda #D9A441 REMOVIDA (verificado no dex: sumiu). Tipografia: Manrope 28/800 (título), 16/600 (linha), 14/400 (corpo). App é sempre escuro (claro recebe mesma paleta).
  - **Bug 1 (FAB duplicado/cortado)**: "Começar a vender" REMOVIDO; único CTA = FAB redondo "+" no canto, posicionado pelo Scaffold ACIMA da barra de navegação (não sobrepõe).
  - **Bug 2 (typo)**: "vitre" → "Sua vitrine está vazia" ✓ (verificado no dex).
  - **Bug 3 (bolinhas coloridas)**: REMOVIDAS. Novo componente IconePlataforma: monograma (ML/EB/SH/OLX/FB/EJ) cinza quando desconectado, cor da marca só quando conectado. Estado comunica por TEXTO ("Conectado" em destaque / "Não conectado").
  - **Aba ativa**: verde-menta em toda a navegação (era mostarda inconsistente).
  - **Início**: busca com borda 1px superfície (sem preenchimento chapado), linhas com borda fina 1px sem sombra, preço em destaque 800, empty state com ícone outline.
  - **Vender**: moldura tracejada 190dp "encaixe o item aqui" (era círculo colorido), botão sólido destaque + secundário texto sublinhado.
  - **Conexões**: linhas com divisor 1px, monogramas, menu "⋯" p/ desconectar, só cartão da IA com superfície elevada.
  - **Mensagens**: TODAS as linhas mesma estrutura (monograma+remetente+prévia+horário); sem-API mostra "Abrir conversa" (era lista de pills separada).
  - **Revisão/Detalhe/Status**: chips limpos, botões IA/Publicar sólidos na destaque, dots coloridos → texto de status.
  - **Verificação no binário**: versionCode 5 / v3.0, cores v3 presentes + mostarda ausente, nvapi- no dex, strings novas OK, assinatura 5bb25361.
- [x] 29/09 — **REDESIGN v2.2 (BUILD 19)**: Início — busca pill com ícone e botão limpar, empty state com hero circle + botão "Começar a vender", cards com borda suave e **preço em verde/negrito** + status em badge pill. Vender — hero circle com gradiente verde + ícone PhotoCamera, botões largos com ícones Material (emojis 📷🖼️ removidos). Revisão — plataformas viraram **chips coloridos FlowRow** (dot da marca + nome, tap marca/desmarca) no lugar de checkbox list, botão "Gerar com a IA" virou OutlinedButton full-width com ícone AutoAwesome + spinner inline, back arrow de verdade (ArrowBack). versionCode 4 / 2.2, mesma assinatura, `nvapi-` no dex, strings v2.2 conferidas no dex.
- [x] 29/09 — **APK 2.0 ESTAVA SEM CHAVE DE IA** (local.properties todo comentado; dex tinha 0 `nvapi-`). Causa do "não tem API". Corrigido: chave NVIDIA recuperada do bash_history, validada (HTTP 200), gravada no local.properties. BUILD 18 assembleRelease **BUILD SUCCESSFUL** (28s falhou por `}` extra meu; fix; depois ~2min). **Verificação no binário: versionCode 3 / 2.1, `nvapi-` 1 ocorrência no dex, strings da UI nova presentes, assinatura 5bb25361 (mesma).** Teste E2E real: POST chat/completions com foto → JSON válido em 62s (NVIDIA lenta hoje; timeout do app = 180s OK). Modelos `llama-3.2-11b/90b-vision` confirmados vivos no catálogo (81 modelos).
- [x] 29/09 — v2.1: marcador `v2.1` no topo da lista (TopAppBar) + cartão "Inteligência artificial" na tela Conexões mostrando provedor ativo (✓ IA ativa: nvidia) ou "sem chave neste build". Antigos APKs apagados, só `AnunciaAI-2.1.apk` na raiz. Servidor LAN :8899 com só ele.
- [x] 26/09 — BUILD 4 `assembleDebug` **BUILD SUCCESSFUL** (52s) — todas as fases compilam
- [x] 26/09 — JDK Temurin 17 portátil instalado (jdk-17.0.20.1+1/) — o Java 21 do sistema é só JRE, sem javac
- [x] 26/09 — Keystore criada (anunciaai.keystore, alias anunciaai, senha no ambiente — NÃO commitada)
- [x] 26/09 — BUILD 6 `assembleRelease` **BUILD SUCCESSFUL** (1m22s, R8 com dontwarn errorprone do Tink)
- [x] 26/09 — APKs na raiz: anunciaai-v1-debug.apk (18MB) e anunciaai-v1-release.apk (1,6MB), assinatura verificada com apksigner (CN=AnunciaAI, SHA-256 5bb25361...)
- [x] 26/09 — README.md com instruções de build + chaves + fluxo
- [x] 27/09 — IA multi-provedor: FabricaIA (nvidia → gemini → qwen automático, override ANUNCIAAI_AI_PROVIDER), GeminiService (2.5 Flash), NvidiaVLService (NIM). DeepSeek NÃO entra: API deles é só texto, sem visão. BUILD 7 **BUILD SUCCESSFUL** (2m03s), APK release regenerado (mesma assinatura)
- [x] 27/09 — TESTES REAIS de chaves (via curl/python): NVIDIA NIM **VÁLIDA** — modelo qwen2.5-vl-7b foi REMOVIDO (EOL 26/08), trocado p/ meta/llama-3.2-11b-vision-instruct: **3.8s por foto, RÁPIDO**; llama-3.2-90b timeouts; Gemini do Lucas é chave "AQ." (Vertex express) — REJEITADA no AI Studio e no Vertex API keys; DeepSeek **VÁLIDA** (1.8s). NVIDIA+llama-3.2-11b virou padrão. BUILD 8 **BUILD SUCCESSFUL** (49s), APK regenerado
- [x] 27/09 — .env do mass-downloader preenchido com NVIDIA+DeepSeek (chmod 600) — pendente antigo de lá RESOLVIDO
- [x] 27/09 — .gitignore criado no AnunciaAI (local.properties/keystore/apk fora de git)
- [x] 27/09 — UI nova estilo SellRaze: tema laranja fogo (claro+escuro), barra inferior Início/Vender/Conexões (navegação em abas c/ saveState), FAB "+ Vender", thumbnails de foto reais (FotoMini c/ inSampleSize) na lista e no detalhe, dots coloridos por plataforma. BUILD 10 **BUILD SUCCESSFUL** (50s), APK regenerado
- [x] 27/09 — Tela de Conexões (spec §8.5): ML OAuth real (abre navegador), Shopee/OLX/FB/Enjoei login manual no WebView (modo somente_login), desconectar, mensagem de status (EstadoConexao)
- [x] 27/09 — .env do mass-downloader REVERTIDO a pedido do Lucas (linhas vazias de novo) — lição: credencial de um projeto só entra nele se ele pedir
- [x] 28/09 — V2 COMPLETA (prompt de revisão): **design verde profundo + papel** (#1F7A5C/#F7F5F1/#14171C, mostarda p/ pendente, erro #B33A3A), Manrope (fonte variável, família única, raios de borda distintos), indicador INLINE (dot+texto), empty states diretos, voz direta ("Publicar"→"Publicado"), FAB "Publicar item"
- [x] 28/09 — MÚLTIPLAS FOTOS: entidade FotoItem(id,itemId,uri,ordem) — DB v2 (destructive), até 10 por item, captura câmera repetida + galeria PickMultipleVisualMedia(10), carrossel reordenável (subir/abaixar/remover/adicionar), IA recebe TODAS na mesma chamada (prompt_multi — defeito reflete na descrição), ML sobe via /pictures (limite 10) e publica com URLs
- [x] 28/09 — MENSAGENS: tela Mensagens (inbox unificado) — ML perguntas REAIS via API (puxarPerguntas + responderPergunta c/ diálogo), OLX/FB/Enjoei/Shopee atalhos "Ver no site" (WebView, sem scraping), indicador não-lida
- [x] 28/09 — VENDIDO/ENCERRAR: botão "Vendi" na publicação → status VENDIDO + encerra no ML via API (idExterno persistido), ind. cinza "Encerrado"
- [x] 28/09 — BUSCA: campo "Buscar por nome" na lista (LIKE %...%)
- [x] 28/09 — BUILD 13 `assembleRelease` **BUILD SUCCESSFUL** (1m51s) → anunciaai-v2-release.apk (1,8MB, mesma assinatura 5bb25361...)
- [x] 28/09 — GIT: repo criada em github.com/lu0ck/AnunciaAI (PÚBLICA — Lucas pediu; verificada HTTP 200 sem login), commit f03287f pushed (HTTPS + token credential store, igual Monitor). .gitignore cobre toolchain (SDK/Gradle/JDK/.gradle) + chaves/keystore/apk. Scrub da senha do keystore no README/progresso/build.gradle antes do push. Licença OFL da Manrope incluída. 59 arquivos versionados.
- [x] 28/09 — AUDITORIA de segurança da repo pública: 3 chaves do Lucas NÃO estão no código (git grep = 0), senha do keystore não commitada (único match: keyAlias, sem o arquivo .keystore que não vai pro git), nenhum arquivo sensível (local.properties/keystore/apk/.env ignorados), histórico com 1 commit só (nada vazado em versão pré-scrub). App NÃO vai pra VPS (engano do Lucas) — arquitetura sem backend: APK fala direto com as APIs.
- [x] 28/09 — CORREÇÃO DA CONFUSÃO DE VERSÕES (Lucas reclamou que "não mudou nada"): achado — todos os APKs tinham versionCode 1 + MESMO ícone roxo, e o `anunciaai-v1-release.apk` (tema laranja) ficou na pasta junto. Cliente instalava o arquivo velho e via "tudo igual". Corrigido: **versionCode 2 / versionName "2.0"**, **ícone verde (#1F7A5C)**, APKs antigos APAGADOS, agora só **`AnunciaAI-2.0.apk`** na raiz. Assinatura via `keystore.properties` (local, gitignored) — build.gradle sem senha hardcoded (repo pública). BUILD 17 **BUILD SUCCESSFUL** + verificação real do APK: versionCode 2 ✓, ícone verde no arsc ✓ (roxo sumiu), Manrope embutida ✓, strings da UI v2 no dex ✓ (Buscar por nome, Fotografe o item, Onde publicar, Ver no site, Vendi), launchable-activity ✓, assinatura 5bb25361 ✓

## Comandos prontos (copiar daqui)
```bash
cd /mnt/SSD_Games_2/Projetos/AnunciaAI
# Debug
GRADLE_USER_HOME=$PWD/.gradle-home ANDROID_HOME=$PWD/android-sdk JAVA_HOME=$PWD/jdk-17.0.20.1+1 ./gradle-8.10.2/bin/gradle assembleDebug --console=plain
# Release
GRADLE_USER_HOME=$PWD/.gradle-home ANDROID_HOME=$PWD/android-sdk JAVA_HOME=$PWD/jdk-17.0.20.1+1 ./gradle-8.10.2/bin/gradle assembleRelease --console=plain
```

## ⚠️ ESTADO ATUAL (03/10 v12.0 — ler isto primeiro no chat novo)
- **APK atual: `AnunciaAI-12.0.apk` (versionCode 21, v12.0, 24MB)** na raiz. **NÃO testado no celular** — reescrita total das 6 telas + arquitetura nova de navegação (cápsula na raiz) aguardam prova real no aparelho.
- Git: tudo commitado e pushado até `049babf`. Repo: github.com/lu0ck/AnunciaAI (PÚBLICA).
- IA: NVIDIA NIM (llama-3.2-11b-vision) FUNCIONAL — chave em local.properties. Contrato: condicao estrita em 4 valores + REGRAS ANTI-CONTRADIÇÃO + retry automático (gerarComContrato). 4 testes unitários PASS.
- Design atual (v12 REESCRIPTO): tema ESCURO #12151A/#1B1F26, destaque única #00C896, texto #F2F2F0, Manrope 400/600/800; NavCapsula glass (blur API 31+) com FAB único na RAIZ; Início com hero gradiente + grid 2 col top-level; Vender com moldura pontilhada + pilha de cards na análise; Revisão com chips de marca e condição vinda direto da IA; Conexões/Mensagens com separador 1px.
- **Convite ao chat novo**: este chat está crescendo — manter respostas curtas, poupar contexto.

## Continuação (chat novo → "continuar do progresso.md")
Novo chat com nome "AnunciaAI" → dizer "continuar do progresso.md". Estado completo acima; comandos de build abaixo.

## Planejado (próximas tarefas, em ordem)
- [ ] **TESTAR v12.0 no celular** (prioridade máxima — reescrita total, tudo abaixo depende de bugs reais): cápsula glass flutuando com "+" único nas 4 abas? Chip de condição bate com a resposta da IA? Grid 2 colunas na Home? Moldura pontilhada + câmera? Pilha de cards na análise? Preços legíveis #F2F2F0?
- [ ] Corrigir o que vier do teste (histórico de crashes da v6/v6.1 mostra que câmera/dock precisam de prova real)
- [ ] Credenciais ML: app no DevCenter (developers.mercadolivre.com.br) → ANUNCIAAI_ML_CLIENT_ID/SECRET no local.properties + redirect br.com.anunciaai://oauth/ml → rebuild → testar OAuth + publicação via API + Mensagens ML
- [ ] Testar OLX WebView de novo (v8.0 tem onReceivedHttpError + tela amigável + UA Chrome — ver se a OLX agora deixa logar/publicar)
- [ ] Considerar APK thin do ML Kit (24MB → ~3MB; exige Play Services) se o tamanho incomodar
- [ ] (opcional) eBay Production keys; Shopee Open Platform (aprovação de parceiro — fora do nosso controle)

## Não terminado / Pendente usuário
- **Commit do fix do voltar em Conexões: código aplicado mas NÃO commitado** — no chat novo: `git status` → commitar (ConexoesScreen.kt + AnunciaAINavGraph.kt) → push. Depois testar o voltar no emulador.
- App criado no https://developers.mercadolivre.com.br/devcenter → `ANUNCIAAI_ML_CLIENT_ID` + `ANUNCIAAI_ML_CLIENT_SECRET` (redirect: **br.com.anunciaai://oauth/ml** — confirmado no código; se recusar deep link, avisar o Hermes p/ ajustar o app)
  - (opcional) eBay Production keys → `ANUNCIAAI_EBAY_CLIENT_ID` + `ANUNCIAAI_EBAY_CLIENT_SECRET`
- Gemini: chave "AQ." do Lucas NÃO serve no AI Studio (precisa AIza... — criar nova em aistudio.google.com/apikey se quiser 2º provedor)
- Shopee: aprovação de parceiro na Open Platform (fora do nosso controle) — v1 cai no WebView como fallback
- Qwen DashScope: Lucas pagou mas desistiu (site pede dados pessoais) — não usar
- GeminiService/QwenVLService NÃO implementam gerarAnuncioComEan (só NvidiaVLService) — se trocar de provedor, implementar lá também (herdam o default que ignora o EAN)
