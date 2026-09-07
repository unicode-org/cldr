package org.unicode.cldr.tool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.TempPrintWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class GenerateKeyboardCharts {
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    static final String SUBDIR = "keyboards";
    static IOException copyErr = null;

    public static void main(String args[]) throws IOException {
        final boolean makeSymlink = (args.length > 0 && args[0].equals("-l"));
        if (makeSymlink) {
            System.err.println("-l: making symlinks");
        } else {
            System.err.println("(use -l to symlink)");
        }
        final File mainDir = new File(CLDRPaths.CHART_DIRECTORY);
        if (mainDir.mkdirs()) {
            System.err.println("Created: " + mainDir);
        }
        if (!mainDir.isDirectory()) {
            throw new IOException("Main dir doesn't exist: " + mainDir);
        }
        final File kbdDir =
                new File(
                        CLDRPaths.BASE_DIRECTORY,
                        "tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/keyboard-charts");
        if (!kbdDir.exists()) {
            throw new IOException("Keyboards root dir doesn't exist: " + kbdDir);
        }
        final File staticTarg = new File(mainDir, SUBDIR + "/");
        final File staticDataTarg = new File(mainDir, SUBDIR + "/data");
        if (staticDataTarg.mkdirs()) {
            System.err.println("Created: " + staticDataTarg);
        }
        System.out.println("Copying: " + kbdDir + " to " + staticTarg);

        final String kbdStaticPrefix = kbdDir.getAbsolutePath();
        Files.walk(kbdDir.toPath())
                .forEach(
                        path -> {
                            if (!path.toFile().isFile()) return;
                            path.getParent().toFile().mkdirs();

                            System.out.println(path.toFile().getAbsolutePath());
                            /** path from static prefix */
                            final String rel =
                                    path.toFile()
                                            .getAbsolutePath()
                                            .substring(kbdStaticPrefix.length());
                            try {
                                final Path out = new File(staticTarg, rel).toPath();
                                System.out.println(" -> " + out);
                                if (!makeSymlink) {
                                    if (Files.isSymbolicLink(out)) {
                                        Files.delete(out);
                                    }
                                    Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
                                } else {
                                    if (out.toFile().isFile()) {
                                        out.toFile().delete();
                                    }
                                    Files.createSymbolicLink(out, path);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.err.println("Error copying " + path);
                                // will overwrite if more than one-  but at least one will be
                                // thrown.
                                copyErr = new IOException("Error copying " + path, e);
                            }
                        });
        // rethrow any error
        if (copyErr != null) throw copyErr;

        // now, generate data
        final Path keyboards3Dir = Path.of(CLDRPaths.KEYBOARDS_3_DIRECTORY);
        System.err.println("Looking for keyboards in " + keyboards3Dir);
        final List<Path> keyboardFiles =
                Files.walk(keyboards3Dir, 1, FileVisitOption.FOLLOW_LINKS)
                        .filter(
                                p ->
                                        p.toFile().isFile()
                                                && p.toString().endsWith(".xml")
                                                && !p.toString()
                                                        .endsWith(
                                                                "-test.xml")) // exclude test files
                        .collect(Collectors.toList());
        keyboardFiles.forEach(p -> System.err.println("- " + p.getFileName()));

        // calculate data
        JsonObject root = new JsonObject();

        JsonObject keyboards = generateKeyboards(keyboardFiles);

        root.add("keyboards", keyboards);
        // write the data
        final File outData = new File(staticDataTarg, "keyboard-data.json");

        try (TempPrintWriter pw = new TempPrintWriter(outData)) {
            pw.noDiff();
            pw.append(gson.toJson(root));
        } finally {
            System.err.println("Wrote: " + outData.toString());
        }

        // copy to .js because of loading issues
        final File outJs = new File(staticDataTarg, "keyboard-data.js");
        try (TempPrintWriter pw = new TempPrintWriter(outJs)) {
            pw.noDiff();
            pw.println("const _KeyboardData = ");
            pw.append(gson.toJson(root));
            pw.println(";");
        } finally {
            System.err.println("Wrote: " + outData.toString());
        }
    }

    private static JsonObject generateKeyboards(List<Path> keyboardFiles) {
        final JsonObject keyboards = new JsonObject();

        for (final Path p : keyboardFiles) {
            try {
                keyboards.add(p.getFileName().toString(), generateKeyboard(p));
            } catch (Throwable t) {
                throw new RuntimeException("While processing " + p, t);
            }
        }

        return keyboards;
    }

    private static JsonElement generateKeyboard(Path p) throws SAXException, IOException {
        final InputSource is = KeyboardFlatten.getInputSource(p.toString());
        Document doc = KeyboardFlatten.flattenDoc(is, p.toString());
        JsonObject o = new JsonObject();

        return appendObject(o, p, doc, doc.getDocumentElement());
    }

    private static JsonObject appendObject(JsonObject o, Path p, Document doc, Element e) {
        JsonObject oo = new JsonObject();

        NamedNodeMap attrs = e.getAttributes();
        appendAttrs(oo, p, doc, e, attrs);

        NodeList childNodes = e.getChildNodes();
        appendChildren(oo, p, doc, e, childNodes);

        String nodeName = e.getNodeName();
        if (o.has(nodeName)) {
            JsonElement existing = o.get(nodeName);
            if (existing.isJsonArray()) {
                // add to array
                existing.getAsJsonArray().add(oo);
            } else {
                // convert obj to array
                o.remove(nodeName);
                JsonArray a = new JsonArray();
                a.add(existing);
                a.add(oo);
                o.add(nodeName, a);
            }
        } else {
            o.add(nodeName, oo);
        }
        return o;
    }

    private static void appendChildren(
            JsonObject o, Path p, Document doc, Element e, NodeList childNodes) {
        if (childNodes == null) return;
        for (int i = 0; i < childNodes.getLength(); i++) {
            final Node n = childNodes.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                appendObject(o, p, doc, (Element) n);
            }
        }
    }

    private static void appendAttrs(
            JsonObject oo, Path p, Document doc, Element e, NamedNodeMap attrs) {
        if (attrs == null) return;
        for (int i = 0; i < attrs.getLength(); i++) {
            final Node attr = attrs.item(i);
            oo.addProperty("@_" + attr.getNodeName(), attr.getNodeValue());
        }
    }
}
