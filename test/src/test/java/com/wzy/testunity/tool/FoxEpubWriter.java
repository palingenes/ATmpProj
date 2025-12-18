package com.wzy.testunity.tool;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

/**
 * public static void main(String[] args) throws IOException {
 *     FoxEpubWriter writer = new FoxEpubWriter(new File("黄泉逆行.epub"), "黄泉逆行");
 *     writer.setBookCreator("作者名");
 *     // 设置封面（关键！）
 *     writer.setCoverImage(new File("cover.jpg")); // 支持 .jpg / .png
 *     writer.addChapter("第1章 起始", "夜很黑，也很静……");
 *     writer.addChapter("第2章 黄泉路", "十殿阎罗的虚影浮现……");
 *     writer.saveAll();
 *     System.out.println("EPUB 生成完成！");
 * }
 */
public class FoxEpubWriter {

    private final File ePubFile; // 生成的epub文件
    private FoxZipWriter zw; // epub 写为 zip文件
    private File TmpDir; // mobi 临时目录

    private boolean isEpub;

    private String BookName;
    private String BookCreator = "zzzia";
    private String CSS = """
            body{
             margin:10px;
             font-size: 1.0em;word-wrap:break-word;
            }
            ul,li{list-style-type:none;margin:0;padding:0;}
            p{text-indent:2em; line-height:1.5em; margin-top:0; margin-bottom:1.5em;}
            .catalog{padding: 1.5em 0;font-size: 0.8em;}
            li{border-bottom: 1px solid #D5D5D5;}
            h1{font-size:1.6em; font-weight:bold;}
            h2 {
                display: block;
                font-size: 1.2em;
                font-weight: bold;
                margin-bottom: 0.83em;
                margin-left: 0;
                margin-right: 0;
                margin-top: 1em;
            }
            .mbppagebreak {
                display: block;
                margin-bottom: 0;
                margin-left: 0;
                margin-right: 0;
                margin-top: 0 }
            a {
                color: inherit;
                text-decoration: none;
                cursor: default
                }
            a[href] {
                color: blue;
                text-decoration: none;
                cursor: pointer
                }
            
            .italic {
                font-style: italic
                }
            """;

    private final String DefNameNoExt = "FoxMake"; //默认文件名
    private final String BookUUID = UUID.randomUUID().toString();

    ArrayList<HashMap<String, Object>> Chapter = new ArrayList<HashMap<String, Object>>(200); //章节结构:1:ID 2:Title 3:Level
    int ChapterID = 100; //章节ID

    // ====== 新增：封面支持 ======
    private byte[] coverImageBytes = null;
    private String coverMimeType = "image/jpeg";

    public FoxEpubWriter(File oEpubFile, String iBookName) {
        ePubFile = oEpubFile;
        BookName = iBookName;

        isEpub = ePubFile.getName().toLowerCase().endsWith(".epub");

        if (ePubFile.exists()) {
            ePubFile.renameTo(new File(ePubFile.getPath() + System.currentTimeMillis()));
        }
        if (isEpub) {
            zw = new FoxZipWriter(ePubFile);
        } else { // mobi
            TmpDir = new File(ePubFile.getParentFile(), "FoxEpub_" + System.currentTimeMillis());
            if (TmpDir.exists()) {
                System.out.println("错误:目录存在: " + TmpDir.getPath());
            } else {
                new File(TmpDir, "html").mkdirs();
                new File(TmpDir, "META-INF").mkdirs();
            }
        }
    }

    // ====== 新增方法：设置封面 ======
    public void setCoverImage(byte[] imageBytes, String mimeType) {
        this.coverImageBytes = imageBytes;
        if (mimeType != null && mimeType.toLowerCase().startsWith("image/")) {
            this.coverMimeType = mimeType;
        } else {
            this.coverMimeType = "image/jpeg";
        }
    }

    public void setCoverImage(File imageFile) throws IOException {
        byte[] bytes = Files.readAllBytes(imageFile.toPath());
        String name = imageFile.getName().toLowerCase();
        String mime = name.endsWith(".png") ? "image/png" :
                name.endsWith(".gif") ? "image/gif" : "image/jpeg";
        setCoverImage(bytes, mime);
    }

    public void setBookName(String bookName) {
        this.BookName = bookName;
    }

    public void setEpub(boolean epub) {
        isEpub = epub;
    }

    public void setBookCreator(String creatorName) {
        this.BookCreator = creatorName;
    }

    public void setCSS(String css) {
        this.CSS = css;
    }

    public void addChapter(String Title, String Content) {
        addChapter(Title, Content, -1, 1);
    }

    public void addChapter(String Title, String Content, int iPageID) {
        addChapter(Title, Content, iPageID, 1);
    }

    public void addChapter(String Title, String Content, int iPageID, int iLevel) {
        if (iPageID < 0) {
            ++this.ChapterID;
        } else {
            this.ChapterID = iPageID;
        }

        HashMap<String, Object> cc = new HashMap<>();
        cc.put("id", this.ChapterID);
        cc.put("name", Title);
        cc.put("level", iLevel);
        Chapter.add(cc);

        this._CreateChapterHTML(Title, Content, this.ChapterID);
    }

    // ====== 新增：保存封面图片 ======
    private void _SaveCoverImage() {
        if (coverImageBytes == null) return;
        if (isEpub) {
            zw.putBinFile(coverImageBytes, "cover.jpg", false);
        } else {
            writeBin(coverImageBytes, new File(TmpDir, "cover.jpg").getPath());
        }
    }

    // ====== 新增：生成封面 HTML ======
    private void _CreateCoverHTML() {
        if (coverImageBytes == null) return;
        String ext = "jpg";
        if ("image/png".equals(coverMimeType)) ext = "png";
        else if ("image/gif".equals(coverMimeType)) ext = "gif";

        String html = "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "  <title>Cover</title>\n" +
                "  <style type=\"text/css\">\n" +
                "    body, div { margin: 0; padding: 0; }\n" +
                "    img { max-width: 100%; height: auto; display: block; margin: auto; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div><img src=\"cover." + ext + "\" alt=\"Cover\" /></div>\n" +
                "</body>\n" +
                "</html>";
        _SaveFile(html, "cover.html");
    }

    // ====== 辅助方法：写入二进制文件（用于 MOBI 模式）======
    private static void writeBin(byte[] data, String path) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAll() {
        // 先处理封面
        if (coverImageBytes != null) {
            _SaveCoverImage();
            _CreateCoverHTML();
        }

        this._CreateIndexHTM();
        this._CreateNCX();
        this._CreateOPF();
        this._CreateMiscFiles();

        if (isEpub) {
            zw.close();
        } else { // 生成mobi
            try {
                Process cmd = Runtime.getRuntime().exec("kindlegen " + DefNameNoExt + ".opf", null, TmpDir);
                InputStream iput = cmd.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(iput));
                while (br.readLine() != null) ;
                br.close();
                iput.close();
                cmd.waitFor();
            } catch (Exception e) {
                System.err.println(e.toString());
            }
            File tmpF = new File(TmpDir, DefNameNoExt + ".mobi");
            if (tmpF.exists() && tmpF.length() > 555) {
                tmpF.renameTo(ePubFile);
                ToolJava.deleteDir(TmpDir);
            }
        }
    }

    private void _CreateNCX() {
        StringBuilder NCXList = new StringBuilder(4096);
        int DisOrder = 1;
        int chapterCount = Chapter.size();
        int lastIDX = chapterCount - 1;

        for (int i = 0; i < chapterCount; i++) {
            HashMap<String, Object> mm = Chapter.get(i);
            int nowID = (Integer) mm.get("id");
            String nowTitle = (String) mm.get("name");
            int nowLevel = (Integer) mm.get("level");

            ++DisOrder;
            int nextLevel = (i == lastIDX) ? 1 : (Integer) Chapter.get(i + 1).get("level");

            if (nowLevel < nextLevel) {
                NCXList.append("\t<navPoint id=\"").append(nowID)
                        .append("\" playOrder=\"").append(DisOrder)
                        .append("\"><navLabel><text>").append(nowTitle)
                        .append("</text></navLabel><content src=\"html/").append(nowID)
                        .append(".html\" />\n");
            } else if (nowLevel == nextLevel) {
                NCXList.append("\t\t<navPoint id=\"").append(nowID)
                        .append("\" playOrder=\"").append(DisOrder)
                        .append("\"><navLabel><text>").append(nowTitle)
                        .append("</text></navLabel><content src=\"html/").append(nowID)
                        .append(".html\" /></navPoint>\n");
            } else { // nowLevel > nextLevel
                NCXList.append("\t\t<navPoint id=\"").append(nowID)
                        .append("\" playOrder=\"").append(DisOrder)
                        .append("\"><navLabel><text>").append(nowTitle)
                        .append("</text></navLabel><content src=\"html/").append(nowID)
                        .append(".html\" /></navPoint>\n\t</navPoint>\n");
            }
        }

        StringBuffer XML = new StringBuffer(4096);
        XML.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE ncx PUBLIC \"-//NISO//DTD ncx 2005-1//EN\" \"http://www.daisy.org/z3986/2005/ncx-2005-1.dtd\">\n<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\" xml:lang=\"zh-cn\">\n<head>\n\t<meta name=\"dtb:uid\" content=\"")
                .append(BookUUID).append("\"/>\n\t<meta name=\"dtb:depth\" content=\"1\"/>\n\t<meta name=\"dtb:totalPageCount\" content=\"0\"/>\n\t<meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n\t<meta name=\"dtb:generator\" content=\"")
                .append(BookCreator).append("\"/>\n</head>\n<docTitle><text>")
                .append(BookName).append("</text></docTitle>\n<docAuthor><text>")
                .append(BookCreator).append("</text></docAuthor>\n<navMap>\n\t<navPoint id=\"toc\" playOrder=\"1\"><navLabel><text>目录:")
                .append(BookName).append("</text></navLabel><content src=\"").append(DefNameNoExt).append(".htm\"/></navPoint>\n")
                .append(NCXList).append("\n</navMap></ncx>\n");

        _SaveFile(XML.toString(), DefNameNoExt + ".ncx");
    }

    private void _CreateOPF() {
        String AddXMetaData = "";
        StringBuffer NowHTMLMenifest = new StringBuffer(4096);
        StringBuffer NowHTMLSpine = new StringBuffer(4096);

        // 处理封面
        String NowImgMenifest = "";
        if (coverImageBytes != null) {
            String coverExt = "jpg";
            if ("image/png".equals(coverMimeType)) coverExt = "png";
            else if ("image/gif".equals(coverMimeType)) coverExt = "gif";

            NowImgMenifest = "\t<item id=\"cover-image\" media-type=\"" + coverMimeType + "\" href=\"cover." + coverExt + "\" />\n";
            AddXMetaData += "\t<meta name=\"cover\" content=\"cover-image\"/>\n";

            // 封面页面加入 manifest 和 spine
            NowHTMLMenifest.insert(0, "\t<item id=\"cover\" media-type=\"application/xhtml+xml\" href=\"cover.html\" />\n");
            NowHTMLSpine.insert(0, "\t<itemref idref=\"cover\" linear=\"no\"/>\n");
        }

        Iterator<HashMap<String, Object>> itr = Chapter.iterator();
        while (itr.hasNext()) {
            HashMap<String, Object> mm = itr.next();
            int nowID = (Integer) mm.get("id");
            NowHTMLMenifest.append("\t<item id=\"page").append(nowID).append("\" media-type=\"application/xhtml+xml\" href=\"html/").append(nowID).append(".html\" />\n");
            NowHTMLSpine.append("\t<itemref idref=\"page").append(nowID).append("\" />\n");
        }

        if (!isEpub) {
            AddXMetaData += "\t<x-metadata><output encoding=\"utf-8\"></output></x-metadata>\n";
        }

        StringBuffer XML = new StringBuffer(4096);
        XML.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"FoxUUID\">\n<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n\t<dc:title>")
                .append(BookName).append("</dc:title>\n\t<dc:identifier opf:scheme=\"uuid\" id=\"FoxUUID\">").append(BookUUID)
                .append("</dc:identifier>\n\t<dc:creator>").append(BookCreator).append("</dc:creator>\n\t<dc:publisher>")
                .append(BookCreator).append("</dc:publisher>\n\t<dc:language>zh-cn</dc:language>\n").append(AddXMetaData)
                .append("</metadata>\n\n\n<manifest>\n\t<item id=\"FoxNCX\" media-type=\"application/x-dtbncx+xml\" href=\"")
                .append(DefNameNoExt).append(".ncx\" />\n\t<item id=\"FoxIDX\" media-type=\"application/xhtml+xml\" href=\"")
                .append(DefNameNoExt).append(".htm\" />\n\n").append(NowHTMLMenifest).append("\n\n")
                .append(NowImgMenifest).append("\n</manifest>\n\n<spine toc=\"FoxNCX\">\n\t<itemref idref=\"FoxIDX\"/>\n\n\n")
                .append(NowHTMLSpine).append("\n</spine>\n\n\n<guide>\n\t<reference type=\"text\" title=\"正文\" href=\"")
                .append("html/").append(Chapter.get(0).get("id")).append(".html\"/>\n\t<reference type=\"toc\" title=\"目录\" href=\"")
                .append(DefNameNoExt).append(".htm\"/>\n")
                .append(coverImageBytes != null ? "\t<reference type=\"cover\" title=\"封面\" href=\"cover.html\"/>\n" : "")
                .append("</guide>\n\n</package>\n\n");
        _SaveFile(XML.toString(), DefNameNoExt + ".opf");
    }

    private void _CreateMiscFiles() {
        _SaveFile(CSS, DefNameNoExt + ".css");

        if (isEpub) {
            zw.putBinFile("application/epub+zip".getBytes(), "mimetype", true);
        } else {
            ToolJava.writeText("application/epub+zip", TmpDir.getPath() + File.separator + "mimetype");
        }

        StringBuffer XML = new StringBuffer(256);
        XML.append("<?xml version=\"1.0\"?>\n<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n\t<rootfiles>\n\t\t<rootfile full-path=\"")
                .append(this.DefNameNoExt).append(".opf")
                .append("\" media-type=\"application/oebps-package+xml\"/>\n\t</rootfiles>\n</container>\n");
        _SaveFile(XML.toString(), "META-INF/container.xml");
    }

    private void _CreateChapterHTML(String Title, String Content, int iPageID) {
        StringBuffer HTML = new StringBuffer(20480);
        HTML.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"zh-CN\">\n<head>\n\t<title>")
                .append(Title)
                .append("</title>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n\t<link rel=\"stylesheet\" type=\"text/css\" href=\"../")
                .append(DefNameNoExt)
                .append(".css\" />\n</head>\n<body>\n<h2><span style=\"border-bottom:1px solid\">")
                .append(Title)
                .append("</span></h2>\n<div>\n\n\n")
                .append(Content)
                .append("\n\n\n</div>\n</body>\n</html>\n");
        _SaveFile(HTML.toString(), "html/" + iPageID + ".html");
    }

    private void _CreateIndexHTM() {
        StringBuffer NowTOC = new StringBuffer(4096);
        Iterator<HashMap<String, Object>> itr = Chapter.iterator();
        while (itr.hasNext()) {
            HashMap<String, Object> mm = itr.next();
            int nowID = (Integer) mm.get("id");
            String nowTitle = (String) mm.get("name");
            NowTOC.append("<div><a href=\"html/").append(nowID).append(".html\">").append(nowTitle).append("</a></div>\n");
        }

        StringBuffer XML = new StringBuffer(4096);
        XML.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"zh-CN\">\n<head>\n\t<title>")
                .append(BookName).append("</title>\n\t<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n\t<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(DefNameNoExt).append(".css\" />\n</head>\n<body>\n<h2>")
                .append(BookName).append("</h2>\n<div>\n\n").append(NowTOC).append("\n\n</div>\n</body>\n</html>\n");
        _SaveFile(XML.toString(), DefNameNoExt + ".htm");
    }

    private void _SaveFile(String content, String saveRelatePath) {
        if (isEpub) {
            zw.putTextFile(content, saveRelatePath);
        } else {
            ToolJava.writeText(content, new File(TmpDir, saveRelatePath).getPath());
        }
    }
}