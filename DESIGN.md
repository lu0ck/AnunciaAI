# AnunciaAI — Design System v2 (verde profundo + papel)

## Cores (Material 3, claro e escuro)
| Papel | Valor |
|---|---|
| Base clara / base escura | #F7F5F1 (papel) / #14171C (tinta) |
| Primária | #1F7A5C (verde profundo) |
| Destaque (pendente/tags, com moderação) | #D9A441 (mostarda) |
| Erro | #B33A3A |
| Texto | #22201D (cinza-tinta, nunca preto puro) |
| Dots por plataforma (indicador inline) | ML #FFE600 · eBay #E53238 · Shopee #EE4D2D · OLX #6E0AD6 · FB #1877F2 · Enjoei #FF4081 |

## Tipografia
Família ÚNICA: Manrope (fallback Inter) — hierarchy por peso/tamanho. SEM texto em CAIXA ALTA.
- Fontes em `app/src/main/res/font/manrope_{regular,medium,semibold,bold}.ttf`
- FontFamily.SansSerif default do Compose mapeado pra Manrope em `AnunciaAITheme` (typography custom)

## Layout
- Captura/revisão = momento principal do app (não formulário genérico)
- Fotos: carrossel horizontal de miniaturas (LazyRow), reordenável na revisão (subir/abaixar/remover/adicionar)
- Status por plataforma: indicador INLINE (ponto colorido + texto) — sem badges decorativos
- Empty states: frase direta com a ação, sem ilustração
- Evitar: sombra cinza padrão em todo card, mesmo raio em tudo, eyebrow CAIXA ALTA, "→" em botões

## Voz da UI
Verbos diretos: "Publicar" → mensagem "Publicado" (nunca "Enviado com sucesso!"). "Salvar", "Apagar", "Conectar". Sem gentilezas artificiais.

## Modelo de fotos (v2)
- `FotoItem(id, itemId FK→Item CASCADE, uri, ordem)` — tabela própria (índice em itemId)
- Item NÃO tem mais fotoUri; migramos: foto única antiga não existe em produção (v1 nunca publicada) → versão do DB 2, fallbackMigration destructive
- Até 10 fotos por item
- IA recebe TODAS as fotos na mesma chamada (conteúdo de defeito reflete na descrição)
- Publicação envia todas, respeitando o limite de cada plataforma

## Mensagens (v2)
- ML: inbox real via API (GET /questions/search + POST /answers)
- eBay: inbox real (precisa keys)
- OLX/FB/Enjoei: botão "Ver mensagens" abre WebView no inbox do anúncio (sem scraping)
- Tela unificada: conversas reais + atalhos WebView, ordenado por data, indicador não-lida

## Voz do fluxo WebView
Publicado → mensagem "Publicado" (padronizado no SessaoPublicacaoWeb.reportar)
