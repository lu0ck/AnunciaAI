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
- [ ] **APK 2.0 NÃO TEM CHAVE DE IA (achado 28/09, verificado no dex: 0 ocorrências de `nvapi-`/`AIza`/`DASHSCOPE`)** — `local.properties` está com todas as chaves comentadas (mtime 27/09 14:59). Sem chave, `FabricaIA.criar()` → null e a revisão cai em preenchimento manual. **Antes de testar a IA: descomentar/preencher `ANUNCIAAI_NVIDIA_KEY` e rebuildar (BUILD 18).**
- [ ] Instalar `anunciaai-v2-release.apk` no celular (substitui a v1 — mesma assinatura) e testar o fluxo completo
- [ ] Teste real: IA multi-fotos (NVIDIA ok), ML OAuth (precisa app no DevCenter), OLX WebView
- [ ] Teste real das Mensagens ML (precisa OAuth + anúncio com perguntas)

## Não terminado / Pendente usuário
- IA: RESOLVIDO 27/09 — NVIDIA NIM (llama-3.2-11b-vision, 3.8s) funcionando com a chave do Lucas em local.properties
- App criado no https://developers.mercadolivre.com.br/devcenter → `ANUNCIAAI_ML_CLIENT_ID` + `ANUNCIAAI_ML_CLIENT_SECRET` (redirect: br.com.anunciaai://oauth/ml; se recusar deep link, avisar o Hermes p/ ajustar o app)
  - (opcional) eBay Production keys → `ANUNCIAAI_EBAY_CLIENT_ID` + `ANUNCIAAI_EBAY_CLIENT_SECRET`
- Gemini: chave "AQ." do Lucas NÃO serve no AI Studio (precisa AIza... — criar nova em aistudio.google.com/apikey se quiser 2º provedor)
- Shopee: aprovação de parceiro na Open Platform (fora do nosso controle) — v1 cai no WebView como fallback
- Qwen DashScope: Lucas pagou mas desistiu (site pede dados pessoais) — não usar
