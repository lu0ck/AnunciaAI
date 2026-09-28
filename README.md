# AnunciaAI

App Android de crosslisting: você tira uma foto do produto, a IA (Qwen-VL Plus) identifica o item e sugere título, descrição, categoria e preço, e você marca em quantas plataformas quer publicar de uma vez.

**Plataformas:** Mercado Livre (API oficial) · eBay (Sell API) · Shopee (WebView — Open API exige aprovação de parceiro) · OLX (WebView) · Facebook Marketplace (WebView) · Enjoei (WebView)

## Instalar no celular (sideload, sem Play Store)

1. Copie **`AnunciaAI-2.0.apk`** para o celular (WhatsApp, cabo USB, Drive...)
2. Toque no arquivo → permita "instalar app desconhecido"
3. Abra o AnunciaAI — o ícone é uma **etiqueta branca em fundo verde**, e em Ajustes → Apps aparece a versão **2.0**

> Se tiver uma versão antiga instalada, pode instalar por cima (mesma assinatura) — o Android vai atualizar porque a versão nova é 2.0 (versionCode 2).
> Se aparecer "app não instalado", desinstale a antiga e instale esta de novo.
> A senha da keystore fica no `keystore.properties` (local, fora do git) — nunca é commitada.

## Build (comandos exatos)

Requisitos já instalados dentro do projeto: `jdk-17.0.20.1+1/`, `gradle-8.10.2/`, `android-sdk/`.

```bash
cd /mnt/SSD_Games_2/Projetos/AnunciaAI

# Debug (teste rápido, ~1 min)
GRADLE_USER_HOME=$PWD/.gradle-home ANDROID_HOME=$PWD/android-sdk JAVA_HOME=$PWD/jdk-17.0.20.1+1 \
  ./gradle-8.10.2/bin/gradle assembleDebug --console=plain

# Release assinado (~1,5 min, com R8/ofuscação)
GRADLE_USER_HOME=$PWD/.gradle-home ANDROID_HOME=$PWD/android-sdk JAVA_HOME=$PWD/jdk-17.0.20.1+1 \
  ./gradle-8.10.2/bin/gradle assembleRelease --console=plain
```

APKs saem em `app/build/outputs/apk/{debug,release}/`.

## Chaves (opcional — só para publicação real)

Edite `local.properties`, descomente e preencha:

```properties
ANUNCIAAI_QWEN_KEY=sua-chave-dashscope   # IA — https://bailian.console.alibabacloud.com/ (~R$0,001/foto)
ANUNCIAAI_ML_CLIENT_ID=...               # app criado em https://developers.mercadolivre.com.br/devcenter
ANUNCIAAI_ML_CLIENT_SECRET=...           # redirect URI: br.com.anunciaai://oauth/ml
ANUNCIAAI_EBAY_CLIENT_ID=...             # opcional — https://developer.ebay.com (Production keys)
ANUNCIAAI_EBAY_CLIENT_SECRET=...
```

Depois reconstrua o APK (as chaves vão embutidas via BuildConfig, ofuscadas pelo R8 no release).

**Sem chave da IA:** o app avisa na tela de revisão e você preenche título/descrição/preço manualmente — o resto (publicar) funciona igual.

## Como funciona (fluxo)

1. "+ Novo item" → foto (câmera ou galeria)
2. IA gera título/descrição/categoria/preço → tudo editável
3. Marque as plataformas → "Publicar"
4. APIs publicam direto; plataformas WebView abrem a página real de criar anúncio com sua sessão logada, preenchem sozinhas e clicam em publicar (você acompanha a tela)
5. Tela de status mostra o resultado por plataforma; histórico fica em "Meus anúncios" → detalhe do item

## Segurança

- Sessão das plataformas sem API: só no CookieManager do WebView (nunca no banco do app)
- Tokens OAuth (ML/eBay): `EncryptedSharedPreferences` (AES-256, chave no Keystore do Android)
- Nenhuma credencial em log/console

## Estrutura

```
app/src/main/java/br/com/anunciaai/
├── AnunciaAIApp.kt / MainActivity.kt      # entrada
├── dados/                                  # Room: Entidades, Daos, AppDatabase, Repositorio
├── ia/                                     # ServicoDeIA (interface) + QwenVLService + modelo/
├── oauth/                                  # TokenStore, Credenciais, OAuthCallback*
├── publica/                                # OrquestradorDePublicacao, PublicadorDePlataforma
│   ├── mercadolivre/                       # API oficial ML
│   ├── ebay/                               # Sell API eBay
│   ├── shopee/                             # stub documentado + script WebView
│   └── webview/                            # scripts OLX/FB/Enjoei + SessaoPublicacaoWeb
├── plataformas/webview/                    # LoginWebViewActivity (executa o fluxo WebView)
└── ui/                                     # Compose: telas, tema, navegação, FotoUtil
```

Continuação entre chats: **progresso.md** (fonte da verdade — feito/planejado/pendente).
