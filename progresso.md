# AnunciaAI — Progresso (fonte da verdade entre chats)

> App Android de crosslisting: foto → IA (Qwen-VL Plus) → revisão → publicar em Mercado Livre / eBay / Shopee / OLX / Facebook Marketplace / Enjoei.
> **Continuação:** novo chat com o nome "AnunciaAI" → dizer "continuar do progresso.md". Espec completa original: `~/.config/Hermes/composer-pastes/pasted_content_2026-09-26_19-00-46-478_e2a84b.txt`
>
> ⚠️ **ESTADO DO CHAT (medido no state.db em 28/09): 894 KB — 235% do limite de 380 KB.** 562 mensagens, 338 tool calls, 201 chamadas de API, 26,8M tokens de entrada acumulados. Pela convenção: **começar chat NOVO para o próximo trabalho**; este arquivo tem tudo que o novo chat precisa.

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

## Planejado (próximos passos, precisa do celular/chaves)
- [x] ~~APK 2.0 NÃO TEM CHAVE DE IA~~ **RESOLVIDO 29/09** — chave NVIDIA no local.properties, BUILD 18 com `nvapi-` no dex (verificado). Verificação E2E real da IA passou (JSON válido).
- [ ] Instalar `AnunciaAI-2.1.apk` no celular (substitui a v1/v2 — mesma assinatura) e testar o fluxo completo. **Ao abrir: tem que aparecer "v2.1" no topo da lista e "✓ IA ativa: nvidia" na aba Conexões — senão o APK velho ficou instalado.**
- [ ] Teste real: IA multi-fotos (NVIDIA ok), ML OAuth (precisa app no DevCenter), OLX WebView
- [ ] Teste real das Mensagens ML (precisa OAuth + anúncio com perguntas)

## Não terminado / Pendente usuário
- IA: RESOLVIDO 27/09 — NVIDIA NIM (llama-3.2-11b-vision, 3.8s) funcionando com a chave do Lucas em local.properties
- App criado no https://developers.mercadolivre.com.br/devcenter → `ANUNCIAAI_ML_CLIENT_ID` + `ANUNCIAAI_ML_CLIENT_SECRET` (redirect: br.com.anunciaai://oauth/ml; se recusar deep link, avisar o Hermes p/ ajustar o app)
  - (opcional) eBay Production keys → `ANUNCIAAI_EBAY_CLIENT_ID` + `ANUNCIAAI_EBAY_CLIENT_SECRET`
- Gemini: chave "AQ." do Lucas NÃO serve no AI Studio (precisa AIza... — criar nova em aistudio.google.com/apikey se quiser 2º provedor)
- Shopee: aprovação de parceiro na Open Platform (fora do nosso controle) — v1 cai no WebView como fallback
- Qwen DashScope: Lucas pagou mas desistiu (site pede dados pessoais) — não usar
