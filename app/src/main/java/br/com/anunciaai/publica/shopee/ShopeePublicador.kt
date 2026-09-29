package br.com.anunciaai.publica.shopee

import br.com.anunciaai.publica.DadosAnuncio
import br.com.anunciaai.publica.PublicadorDePlataforma
import br.com.anunciaai.publica.ResultadoPublicacao
import br.com.anunciaai.publica.webview.ScriptDeFormulario

/**
 * Tentativa de integração Shopee Open API.
 *
 * DOCUMENTAÇÃO (Fase 3): a Open Platform (https://open.shopee.com/) exige aprovação de
 * parceiro (cadastro de empresa/CPF, análise manual). Sem aprovação, não há client_id/secret
 * válidos — por isso na v1 a Shopee é publicada via WebView (fallback), igual a OLX/FB/Enjoei.
 * Quando aprovado: implementar aqui OAuth + /api/v2/product/add_item e rotear a Shopee
 * de volta ao OrquestradorDePublicacao.
 */
class ShopeePublicador : PublicadorDePlataforma {

    override val plataforma = "SHOPEE"

    override suspend fun publicar(dados: DadosAnuncio): ResultadoPublicacao {
        return ResultadoPublicacao.Erro(
            "Shopee Open API exige aprovação de parceiro — na v1 use o método WebView",
            false
        )
    }
}

/** Script de preenchimento do formulário Shopee (seller.shopee.com.br). */
object ScriptShopee : ScriptDeFormulario {
    override val url = "https://seller.shopee.com.br/portal/product/list"
    override val js = """
        (function(){
          var rs = {ok:false, msg:''};
          try {
            var i = document.querySelector('input[name="name"], input[placeholder*="nome" i], input[placeholder*="Nome do produto" i]');
            if (i) { i.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_TITULO || ''); }
            else { rs.msg = 'campo título não achado'; }
            var p = document.querySelector('input[name="price"], input[placeholder*="preço" i], input[placeholder*="Preço" i]');
            if (p) { p.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_PRECO || ''); }
            var d = document.querySelector('textarea[name="description"], textarea[placeholder*="descri" i], div[contenteditable="true"]');
            if (d) { d.focus(); document.execCommand('insertText', false, window.ANUNCIAAI_DESC || ''); }
            if (!rs.msg) rs.msg = 'preenchido';
            rs.ok = !!i;
          } catch(e) { rs.msg = 'erro: ' + e.message; }
          window.ANUNCIAAI_RESULT = JSON.stringify(rs);
        })();""".trimIndent()
}
