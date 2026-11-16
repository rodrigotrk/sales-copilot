package app.salescopilot.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Conferência do .docx antes do Tika: descompacta cada parte contando os bytes de fato produzidos, sem
 * confiar em tamanhos declarados, e recusa o arquivo se uma parte XML passa de 20 MB ou se o total passa
 * de 100 MB. O POI carrega partes XML, como os metadados, inteiras na memória antes de qualquer limite de
 * texto; imagens não são lidas como texto e só contam no total. As partes vêm do diretório central, como o
 * parser as lê, e o que é XML vem do nome e de [Content_Types].xml, inclusive tipos sobrescritos.
 */
final class ZipBudget {

    static final long MAX_XML_PART_BYTES = 20L * 1024 * 1024;
    static final long MAX_TOTAL_BYTES = 100L * 1024 * 1024;
    private static final long MAX_CONTENT_TYPES_BYTES = 1024 * 1024;
    private static final String CONTENT_TYPES = "[Content_Types].xml";

    private ZipBudget() {
    }

    static void check(Path file) throws IOException, ImportFailure {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            XmlParts xml = xmlParts(zip);
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                boolean isXml = xml.contains(entry.getName());
                long expanded = 0;
                try (InputStream in = zip.getInputStream(entry)) {
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        expanded += read;
                        total += read;
                        if ((isXml && expanded > MAX_XML_PART_BYTES) || total > MAX_TOTAL_BYTES) {
                            throw new ImportFailure("text_too_long");
                        }
                    }
                }
            }
        }
    }

    /** Extensões e partes que o pacote declara como XML. */
    private record XmlParts(Set<String> extensions, Set<String> parts) {

        boolean contains(String entryName) {
            String name = entryName.toLowerCase(Locale.ROOT);
            int dot = name.lastIndexOf('.');
            String extension = dot < 0 ? "" : name.substring(dot + 1);
            return extension.equals("xml") || extension.equals("rels") || extensions.contains(extension)
                    || parts.contains(name);
        }
    }

    /** Tipos que o POI lê como XML; "openxmlformats" no nome de um tipo binário, como o de OLE, não conta. */
    private static boolean isXml(String contentType) {
        return contentType.endsWith("+xml") || contentType.equals("application/xml") || contentType.equals("text/xml");
    }

    private static XmlParts xmlParts(ZipFile zip) throws IOException, ImportFailure {
        Set<String> extensions = new HashSet<>();
        Set<String> parts = new HashSet<>();
        ZipEntry types = zip.getEntry(CONTENT_TYPES);
        if (types == null) {
            return new XmlParts(extensions, parts);
        }
        byte[] content;
        try (InputStream in = zip.getInputStream(types)) {
            content = in.readNBytes((int) MAX_CONTENT_TYPES_BYTES + 1);
        }
        if (content.length > MAX_CONTENT_TYPES_BYTES) {
            throw new ImportFailure("text_too_long");
        }
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.newSAXParser().parse(new java.io.ByteArrayInputStream(content), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    String type = attributes.getValue("ContentType");
                    if (type == null || !isXml(type.toLowerCase(Locale.ROOT).strip())) {
                        return;
                    }
                    if (localName.equals("Default") && attributes.getValue("Extension") != null) {
                        extensions.add(attributes.getValue("Extension").toLowerCase(Locale.ROOT));
                    } else if (localName.equals("Override") && attributes.getValue("PartName") != null) {
                        parts.add(attributes.getValue("PartName").replaceFirst("^/", "").toLowerCase(Locale.ROOT));
                    }
                }
            });
        } catch (SAXException | ParserConfigurationException e) {
            throw new ImportFailure("import_failed", e);
        }
        return new XmlParts(extensions, parts);
    }
}
