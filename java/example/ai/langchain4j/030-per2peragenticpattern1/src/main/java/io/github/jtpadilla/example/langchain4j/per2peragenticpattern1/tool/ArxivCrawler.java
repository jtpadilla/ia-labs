package io.github.jtpadilla.example.langchain4j.per2peragenticpattern1.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// Tool que consulta la API pública de arXiv (Atom/XML) para buscar y recuperar papers científicos.
public class ArxivCrawler {

    private static final String ARXIV_API = "https://export.arxiv.org/api/query";

    private final HttpClient http = HttpClient.newHttpClient();

    @Tool("Busca artículos científicos en arxiv.org. Devuelve títulos, autores, resúmenes y URLs de los artículos encontrados.")
    public String searchPapers(
            @P("palabras clave o tema a buscar en arxiv.org") String query,
            @P("número máximo de artículos a devolver, entre 1 y 10") int maxResults) {

        int limit = Math.clamp(maxResults, 1, 10);
        String url = ARXIV_API
                + "?search_query=all:" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&start=0&max_results=" + limit
                + "&sortBy=relevance&sortOrder=descending";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            String body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return parseAtomFeed(body);
        } catch (Exception e) {
            return "Error querying arXiv: " + e.getMessage();
        }
    }

    @Tool("Obtiene el resumen y los metadatos de un artículo de arXiv a partir de su ID (p.ej. '2301.07041') o URL completa.")
    public String getPaper(@P("ID del artículo en arXiv o URL completa") String arxivId) {
        String id = arxivId.replaceAll(".*/abs/", "").replaceAll("v\\d+$", "").strip();
        String url = ARXIV_API + "?id_list=" + URLEncoder.encode(id, StandardCharsets.UTF_8);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            String body = http.send(request, HttpResponse.BodyHandlers.ofString()).body();
            return parseAtomFeed(body);
        } catch (Exception e) {
            return "Error fetching paper from arXiv: " + e.getMessage();
        }
    }

    private String parseAtomFeed(String xml) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList entries = doc.getElementsByTagNameNS("*", "entry");
        if (entries.getLength() == 0) {
            return "No papers found.";
        }

        var sb = new StringBuilder();
        for (int i = 0; i < entries.getLength(); i++) {
            Node entry = entries.item(i);
            sb.append("### Paper ").append(i + 1).append('\n');
            sb.append("Title: ").append(childText(entry, "title").strip()).append('\n');
            sb.append("Authors: ").append(String.join(", ", collectAuthors(entry))).append('\n');
            sb.append("Published: ").append(childText(entry, "published").strip()).append('\n');
            sb.append("URL: ").append(childText(entry, "id").strip()).append('\n');
            sb.append("Abstract: ").append(childText(entry, "summary").strip()).append('\n').append('\n');
        }
        return sb.toString();
    }

    private List<String> collectAuthors(Node parent) {
        var names = new ArrayList<String>();
        if (parent instanceof Element el) {
            NodeList authorNodes = el.getElementsByTagNameNS("*", "author");
            for (int j = 0; j < authorNodes.getLength(); j++) {
                String name = childText(authorNodes.item(j), "name");
                if (!name.isBlank()) names.add(name.strip());
            }
        }
        return names;
    }

    private String childText(Node parent, String tagName) {
        if (parent instanceof Element el) {
            NodeList nodes = el.getElementsByTagNameNS("*", tagName);
            if (nodes.getLength() > 0) {
                String text = nodes.item(0).getTextContent();
                return text != null ? text : "";
            }
        }
        return "";
    }

}
