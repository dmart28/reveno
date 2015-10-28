package org.reveno.atp.clustering.util;

import org.reveno.atp.utils.Exceptions;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ResourceLoader {

    public static final String PROPERTIES_REGEX = "\\$\\{(?<ctn>[^}]*)\\}";
    protected static final Pattern PROPERTIES_PATTERN = Pattern.compile(PROPERTIES_REGEX);

    public static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    public static String loadResource(File file, Properties properties) {
        try {
            return loadResource(new FileInputStream(file), properties);
        } catch (FileNotFoundException e) {
            throw Exceptions.runtime(e);
        }
    }

    public static String loadResource(InputStream stream, Properties properties) {
        try {
            String content = streamToString(stream);
            Matcher match = PROPERTIES_PATTERN.matcher(content);
            StringBuffer sb = new StringBuffer();
            while (match.find()) {
                String value = match.group("ctn");
                String[] split = value.split(":");
                if (properties.containsKey(split[0])) {
                    String prop = properties.getProperty(value);
                    match.appendReplacement(sb, prop);
                } else if (split.length > 1) {
                    match.appendReplacement(sb, split[1]);
                }
            }
            match.appendTail(sb);
            return sb.toString();
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    protected static String streamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
