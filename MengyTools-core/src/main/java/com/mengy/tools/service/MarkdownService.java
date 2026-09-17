package com.mengy.tools.service;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户内容的 Markdown 渲染与 HTML 消毒。
 *
 * 为什么必须放在服务端：用户帖子的正文是**不可信输入**。管理端内容由可信编辑产出可以直接存 HTML，
 * 但 UGC 若直接存储渲染结果，任何人只要在帖子/评论里写入 <script> 就能影响所有访客（存储型 XSS）。
 *
 * 处理链：Markdown 文本 → flexmark 渲染 → jsoup Safelist 白名单过滤 → 入库的 content_html。
 * 白名单之外的一切标签与属性（script/style/iframe/on* 事件、javascript: 协议、data: 图片等）都会被剥离，
 * 因此即使渲染器有漏洞，最终产物也只包含这里显式允许的元素。
 */
@Slf4j
@Service
public class MarkdownService {

    /** 只允许这两种协议的链接与图片；其余（javascript:/data:/vbscript: 等）一律丢弃 */
    private static final Pattern SAFE_PROTOCOL = Pattern.compile("^(https?:)?//.+|^/.*", Pattern.CASE_INSENSITIVE);

    private static final int MAX_HTML_LENGTH = 200_000;

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                AutolinkExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()
        ));
        // 关闭原始 HTML 透传：Markdown 里内嵌的 <script> 等不会直接进入渲染结果
        options.set(HtmlRenderer.ESCAPE_HTML, true);
        options.set(HtmlRenderer.SUPPRESS_HTML, true);
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /** Markdown → 消毒后的 HTML（入库用） */
    public String renderToSafeHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String rendered = renderer.render(parser.parse(markdown));
        String safe = sanitize(rendered);
        if (safe.length() > MAX_HTML_LENGTH) {
            safe = safe.substring(0, MAX_HTML_LENGTH);
        }
        return safe;
    }

    /** 纯文本摘要（列表页用、通知摘要用）：剥离所有标签后按长度截断 */
    public String toPlainText(String markdown, int maxLength) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = renderer.render(parser.parse(markdown));
        String text = Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    /**
     * 白名单消毒。允许的标签与属性都显式列出：
     * 即便渲染阶段出现意外标签，也会在这里被移除。
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Safelist safelist = Safelist.none()
                .addTags("p", "br", "hr", "strong", "b", "em", "i", "del", "s", "sup", "sub",
                        "code", "pre", "blockquote",
                        "ul", "ol", "li",
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "a", "img",
                        "table", "thead", "tbody", "tr", "th", "td")
                .addAttributes("a", "href", "title")
                .addAttributes("img", "src", "alt", "title")
                .addAttributes("code", "class")
                .addAttributes("th", "align")
                .addAttributes("td", "align")
                .addAttributes("ol", "start")
                // 注意：这里刻意不使用 addProtocols(a/img)。
                // 站点自身的图片存的是相对路径 /images/xxx，而 jsoup 的协议白名单会把
                // “无协议”的相对 URL 判为非法从而把 src 整个丢掉（正文插图会全废）。
                // 因此改成“清理后手工校验”下方 SAFE_PROTOCOL：允许 http(s) 绝对地址与 / 开头的站内相对路径，
                // 其余（javascript:、data:、vbscript: 等）一律移除。
                .preserveRelativeLinks(true);

        Document doc = Jsoup.parse(html);
        doc.outputSettings().prettyPrint(false);
        Document.OutputSettings settings = doc.outputSettings();

        Document cleaned = Jsoup.parse(Jsoup.clean(doc.body().html(), "", safelist));
        cleaned.outputSettings(settings);
        cleaned.outputSettings().prettyPrint(false);

        // 外链补 rel/target；站内相对路径与锚点不加
        for (Element a : cleaned.select("a[href]")) {
            String href = a.attr("href");
            if (!SAFE_PROTOCOL.matcher(href).matches() && !href.startsWith("#")) {
                a.remove();
                continue;
            }
            if (href.startsWith("http")) {
                a.attr("rel", "nofollow noopener noreferrer");
                a.attr("target", "_blank");
            }
        }
        // 图片仅保留安全协议（jsoup 已按协议白名单过滤，这里再兜一层相对路径判断）
        for (Element img : cleaned.select("img[src]")) {
            String src = img.attr("src");
            if (!SAFE_PROTOCOL.matcher(src).matches()) {
                img.remove();
            } else {
                img.attr("loading", "lazy");
            }
        }
        return cleaned.body().html();
    }
}
