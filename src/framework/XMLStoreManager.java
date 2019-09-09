package framework;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLStoreManager {
    private final String targetPath;
    private Document doc = null;

    public XMLStoreManager(String filePath) {
        this.targetPath = filePath;
    }

    public XMLStoreManager openTarget() {
        File to = new File(targetPath);
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            if (!to.exists()) {
                if (!to.getParentFile().exists()) {
                    to.getParentFile().mkdirs();
                }
                to.createNewFile();
                File dtd = new File(to.getParent() + "/" + "format.dtd");
                if (!dtd.exists()) {
                    dtd.createNewFile();

                    FileWriter writer = new FileWriter(dtd);
                    writer.write("<!ELEMENT store (page*)>\n" +
                            "<!ELEMENT page (list-node*, map-node*)>\n" +
                            "<!ELEMENT list-node (value-store*)>\n" +
                            "<!ATTLIST list-node name CDATA #REQUIRED>\n" +
                            "<!ELEMENT value-store EMPTY>\n" +
                            "<!ATTLIST value-store name CDATA #IMPLIED>\n" +
                            "<!ATTLIST value-store value CDATA #REQUIRED>\n" +
                            "<!ELEMENT map-node (value-store*)>\n" +
                            "<!ATTLIST map-node name CDATA #REQUIRED>\n" +
                            "<!ATTLIST page id ID #REQUIRED>\n");
                    writer.flush();
                    writer.close();
                }


                doc = builder.newDocument();
                doc.appendChild(doc.createElement("store"));
            } else {
                doc = builder.parse(to);
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    public Page getPage(String pageName) {
        if (doc.getDocumentElement() == null) {
            Element root = doc.createElement("store");
            doc.appendChild(root);
        }

        Element target = doc.getElementById(pageName);

        if (target == null) {
            target = doc.createElement("page");
            target.setAttribute("id", pageName);

            doc.getDocumentElement().appendChild(target);
        }
        return new Page(target, this);
    }

    public void save() {
        // 创建TransformerFactory对象
        TransformerFactory tff = TransformerFactory.newInstance();

        File dest = new File(this.targetPath);
        if (!dest.exists()) {
            try {
                dest.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 创建Transformer对象
        Transformer tf = null;
        try {
            tf = tff.newTransformer();

            tf.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "format.dtd");

            // 使用Transformer的transform()方法将DOM树转换成XML
            tf.transform(new DOMSource(doc), new StreamResult(dest));

        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public static class Page {
        private final Element pageElm;
        private final XMLStoreManager holder;

        private Page(Element pageElm, XMLStoreManager holder) {
            this.pageElm = pageElm;
            this.holder = holder;
        }

        public void putStringList(String name, List<String> list) {
            Node targetListNode = null;

            // 查找节点
            NodeList nodes = pageElm.getElementsByTagName("list-node");
            int i = 0;
            for (; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                if (((Element) node).getAttribute("name").equals(name)) {
                    targetListNode = node;
                    break;
                }
            }
            if (i >= nodes.getLength()) {
                targetListNode = pageElm.getOwnerDocument().createElement("list-node");
                ((Element) targetListNode).setAttribute("name", name);
                pageElm.appendChild(targetListNode);
            }


            // 清空节点
            NodeList values_store = targetListNode.getChildNodes();
            for (int wwwwssss = 0; wwwwssss < values_store.getLength(); ) {
                targetListNode.removeChild(values_store.item(wwwwssss));
            }

            // 存储数据
            for (String value : list) {
                Element newElm = pageElm.getOwnerDocument().createElement("value-store");
                newElm.setAttribute("value", value);
                targetListNode.appendChild(newElm);
            }
        }

        public List<String> getStringList(String name) {
            NodeList nodes = pageElm.getElementsByTagName("list-node");
            Node targetListNode = null;

            // 查找节点
            int i = 0;
            for (; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                if (((Element) node).getAttribute("name").equals(name)) {
                    targetListNode = node;
                    break;
                }
            }
            if (i >= nodes.getLength()) {
                return new ArrayList<>();
            }


            // 获取内容
            List<String> values = new ArrayList<>();
            NodeList values_store = targetListNode.getChildNodes();
            for (int wwwwssss = 0; wwwwssss < values_store.getLength(); ++wwwwssss) {
                Node node = values_store.item(wwwwssss);

                if (node instanceof Element) {
                    Element e = (Element) node;
                    String v = e.getAttribute("value");
                    if (!v.isEmpty()) {
                        values.add(v);
                    }
                }
            }

            return values;
        }

        public void putStringMap(String name, Map<String, String> map) {
            NodeList nodes = pageElm.getElementsByTagName("map-node");
            Node targetNode = null;

            // 查找节点
            int i = 0;
            for (; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                if (((Element) node).getAttribute("name").equals(name)) {
                    targetNode = node;
                    break;
                }
            }
            if (i >= nodes.getLength()) {
                targetNode = pageElm.getOwnerDocument().createElement("map-node");
                ((Element) targetNode).setAttribute("name", name);
                pageElm.appendChild(targetNode);
            }

            // 清空节点
            NodeList values_store = targetNode.getChildNodes();
            for (int wwwwssss = 0; wwwwssss < values_store.getLength(); ) {
                targetNode.removeChild(values_store.item(wwwwssss));
            }

            for (Map.Entry<String, String> entry : map.entrySet()) {
                Element newElm = pageElm.getOwnerDocument().createElement("value-store");
                newElm.setAttribute("name", entry.getKey());
                newElm.setAttribute("value", entry.getValue());
                targetNode.appendChild(newElm);
            }
        }

        public Map<String, String> getStringMap(String name) {
            NodeList nodes = pageElm.getElementsByTagName("map-node");
            Node targetNode = null;

            // 查找节点
            int i = 0;
            for (; i < nodes.getLength(); ++i) {
                Node node = nodes.item(i);
                if (((Element) node).getAttribute("name").equals(name)) {
                    targetNode = node;
                    break;
                }
            }
            if (i >= nodes.getLength()) {
                return new HashMap<>();
            }

            Map<String, String> map = new HashMap<>();
            // 清空节点
            NodeList values_store = targetNode.getChildNodes();
            for (int wwwwssss = 0; wwwwssss < values_store.getLength(); ++wwwwssss) {
                Node e = values_store.item(wwwwssss);
                if (e instanceof Element) {
                    Element node = (Element) e;
                    map.put(node.getAttribute("name"), node.getAttribute("value"));
                }
            }

            return map;
        }

        public XMLStoreManager getManager() {
            return holder;
        }
    }
}
