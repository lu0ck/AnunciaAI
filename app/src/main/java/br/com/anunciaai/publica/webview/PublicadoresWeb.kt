package br.com.anunciaai.publica.webview

/**
 * Contrato do script de preenchimento de formulário (plataformas sem API oficial).
 * A LoginWebViewActivity abre a URL real de "criar anúncio", injeta o JS e lê o resultado.
 *
 * Variáveis disponíveis para o script: window.ANUNCIAAI_TITULO / ANUNCIAAI_PRECO / ANUNCIAAI_DESC.
 * O script deve gravar o resultado em window.ANUNCIAAI_RESULT = JSON {ok: bool, msg: string}.
 */
interface ScriptDeFormulario {
    /** URL da página "criar anúncio" da plataforma. */
    val url: String

    /** JavaScript que preenche os campos e publica. */
    val js: String
}

/** OLX — anunciar.olx.com.br (sessão logada do usuário no WebView). */
object ScriptOLX : ScriptDeFormulario {
    override val url = "https://anunciar.olx.com.br/"
    override val js = """
        (function(){
          var rs = {ok:false, msg:''};
          try {
            var t = document.querySelector('input[name="title"], input[placeholder*="título" i], input[placeholder*="Título" i], input#input-title');
            if (t) { t.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_TITULO || ''); }
            else { rs.msg = 'campo título não achado'; }
            var d = document.querySelector('textarea[name="description"], textarea[placeholder*="descri" i], div[contenteditable="true"]');
            if (d) { d.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_DESC || ''); }
            var p = document.querySelector('input[name="price"], input[placeholder*="preço" i], input[placeholder*="Preço" i]');
            if (p) { p.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_PRECO || ''); }
            if (!rs.msg) rs.msg = 'preenchido';
            rs.ok = !!t;
          } catch(e) { rs.msg = 'erro: ' + e.message; }
          window.ANUNCIAAI_RESULT = JSON.stringify(rs);
        })();""".trimIndent()
}

/** Facebook Marketplace — facebook.com/marketplace/create (sessão logada do usuário). */
object ScriptFacebook : ScriptDeFormulario {
    override val url = "https://www.facebook.com/marketplace/create/"
    override val js = """
        (function(){
          var rs = {ok:false, msg:''};
          try {
            // O formulário do FB é React com contenteditable/aria-labels
            var t = document.querySelector('div[contenteditable="true"][aria-label*="título" i], input[name="title"], div[role="textbox"]');
            if (t) { t.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_TITULO || ''); }
            else { rs.msg = 'campo título não achado'; }
            var p = document.querySelector('input[aria-label*="preço" i], input[placeholder*="preço" i]');
            if (p) { p.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_PRECO || ''); }
            var d = document.querySelectorAll('div[contenteditable="true"]')[1];
            if (d) { d.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_DESC || ''); }
            if (!rs.msg) rs.msg = 'preenchido';
            rs.ok = !!t;
          } catch(e) { rs.msg = 'erro: ' + e.message; }
          window.ANUNCIAAI_RESULT = JSON.stringify(rs);
        })();""".trimIndent()
}

/** Enjoei — enjoei.com.br (sessão logada do usuário). */
object ScriptEnjoei : ScriptDeFormulario {
    override val url = "https://enjoei.com.br/produtos/novo"
    override val js = """
        (function(){
          var rs = {ok:false, msg:''};
          try {
            var t = document.querySelector('input[name="product[title]"], input[placeholder*="o que você quer vender" i], input[name="title"]');
            if (t) { t.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_TITULO || ''); }
            else { rs.msg = 'campo título não achado'; }
            var d = document.querySelector('textarea[name="product[description]"], textarea[name="description"], div[contenteditable="true"]');
            if (d) { d.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_DESC || ''); }
            var p = document.querySelector('input[name="product[price]"], input[placeholder*="preço" i]');
            if (p) { p.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_PRECO || ''); }
            if (!rs.msg) rs.msg = 'preenchido';
            rs.ok = !!t;
          } catch(e) { rs.msg = 'erro: ' + e.message; }
          window.ANUNCIAAI_RESULT = JSON.stringify(rs);
        })();""".trimIndent()
}

/** Devolve o script da plataforma (inclui Shopee, cujo script vive no pacote dela). */
object ScriptsWeb {
    fun de(plataforma: String): ScriptDeFormulario? = when (plataforma) {
        "OLX" -> ScriptOLX
        "FACEBOOK_MARKETPLACE" -> ScriptFacebook
        "ENJOEI" -> ScriptEnjoei
        "SHOPEE" -> br.com.anunciaai.publica.shopee.ScriptShopee
        else -> null
    }

    /** Trecho final comum: clica no botão de publicar/enviar do formulário. */
    val JsPublicar = """
        (function(){
          var btn = document.querySelector('button[type="submit"]');
          if (!btn) btn = document.querySelector('button[data-testid*="submit" i], button[class*="publish" i], button[aria-label*="publicar" i], button[class*="botao-enviar" i]');
          if (btn) { btn.click(); }
        })();""".trimIndent()
}
