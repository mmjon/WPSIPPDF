import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SingleImagePdfRecognitionApp extends JFrame {
    public static void main(String[] args) {
        String tessDataPath = "D:\\Tesseract-OCR\\tessdata";   //Tesseract-OCR语言包路径
        String path = "D:\\excelpdf\\output\\";     //pdf文件存储路径
        List<String> list = new ArrayList<>(); // 存储成功提取的图号
        List<String> failedFiles = new ArrayList<>(); // 存储提取失败的 PDF 文件名
        File dir = new File(path);
        String[] children = dir.list();
        if (children == null) {
            System.out.println("目录不存在或它不是一个目录");
        } else {
            for (int i = 0; i < children.length; i++) {
                String filename = children[i];
//                String filename = "converted_pdf_"+(23+i)+".pdf";
                String pdfFilePath = path + filename;


                try {
                    // 加载 PDF 文件
                    File file = new File(pdfFilePath);
                    if (!file.exists()) {
                        System.err.println("错误：PDF 文件不存在，请检查路径：" + pdfFilePath);
                        return;
                    }

                    PDDocument document = PDDocument.load(file);
                    if (document.isEncrypted()) {
                        System.err.println("错误：PDF 文件已加密，请提供密码或解密文件");
                        document.close();
                        return;
                    }

                    // 初始化 PDFRenderer
                    PDFRenderer pdfRenderer = new PDFRenderer(document);

                    // 初始化 Tesseract
                    Tesseract tesseract = new Tesseract();
                    tesseract.setDatapath(tessDataPath);
                    tesseract.setLanguage("eng+chi_sim");
                    tesseract.setPageSegMode(7);
                    tesseract.setOcrEngineMode(1);
                    tesseract.setTessVariable("user_defined_dpi", "800");

                    // 仅处理第一页
                    int page = 0;
                    System.out.println("正在处理页面 " + (page + 1) + "，文件: " + filename + "...");

                    // 转换为高分辨率图像
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 800);

                    // 保存原始图像用于调试
                    File debugFile = new File("page_" + (page + 1) + ".png");
                    ImageIO.write(image, "png", debugFile);
                    System.out.println("已保存原始图像：page_" + (page + 1) + ".png");

                    // 初始裁剪参数
                    double xRatio = 0.9;
                    double yRatio = 0.88;
                    double widthRatio = 0.10;
                    double heightRatio = 0.03;
                    int maxAttempts = 3; // 最大重试次数
                    int attempt = 0;
                    String figureNumber = null;

                    while (attempt < maxAttempts && figureNumber == null) {
                        BufferedImage croppedImage = cropImage(image, xRatio, yRatio, widthRatio, heightRatio);
                        File croppedDebugFile = new File("images1/cropped_page_" + (page + 1) + "_" + i + "_" + attempt + ".png");
                        ImageIO.write(croppedImage, "png", croppedDebugFile);
                        System.out.println("已保存裁剪图像：cropped_page_" + (page + 1) + "_" + i + "_" + attempt + ".png");

                        // 图号区域 OCR
                        String figureNumberText = tesseract.doOCR(croppedImage);
                        System.out.println("页面 " + (page + 1) + " 第 " + (attempt + 1) + " 次尝试提取的文本：");
                        System.out.println(figureNumberText.isEmpty() ? "(空)" : figureNumberText);

                        // 提取图号
                        figureNumber = extractFigureNumber(figureNumberText);
                        if (figureNumber != null) {
                            System.out.println("页面 " + (page + 1) + " 提取的图号：" + figureNumber);
                        } else {
                            System.out.println("页面 " + (page + 1) + " 第 " + (attempt + 1) + " 次尝试未找到包含 BJDCJC- 的图号");
                            // 向下偏移裁剪区域
                            yRatio += heightRatio*0.8; // 向下移动一个 heightRatio 的距离
                            attempt++;
                        }
                    }

                    // 关闭 PDF 文档
                    document.close();

                    if (figureNumber != null) {
                        // 重命名文件
                        String newFileName = figureNumber + ".pdf";
                        File newFile = new File(path + newFileName);
                        int suffix = 1;
                        while (newFile.exists()) {
                            newFileName = figureNumber + "_" + suffix + ".pdf";
                            newFile = new File(path + newFileName);
                            suffix++;
                        }
                        if (file.renameTo(newFile)) {
                            System.out.println("成功将 " + filename + " 重命名为 " + newFileName);
                        } else {
                            System.out.println("重命名 " + filename + " 失败");
                        }
                        list.add(figureNumber);
                    } else {
                        System.out.println("页面 " + (page + 1) + " 所有尝试失败，未找到包含 BJDCJC- 的图号");
                        failedFiles.add(filename); // 记录失败的文件名
                        list.add("(未识别)");
                    }



                } catch (IOException e) {
                    System.err.println("处理 PDF 文件时发生错误: " + e.getMessage());
                    e.printStackTrace();
                } catch (TesseractException e) {
                    System.err.println("OCR 处理时发生错误: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 输出所有图号
            System.out.println("所有图号：");
            for (String figureNumber : list) {
                System.out.println(figureNumber);
            }

            // 输出提取失败的文件名
            if (!failedFiles.isEmpty()) {
                System.out.println("提取图号失败的文件：");
                for (String failedFile : failedFiles) {
                    System.out.println(failedFile);
                }
            }
        }
    }

    // 裁剪图像
    private static BufferedImage cropImage(BufferedImage image, double xRatio, double yRatio, double widthRatio, double heightRatio) {
        int width = image.getWidth();
        int height = image.getHeight();
        int x = (int) (width * xRatio);
        int y = (int) (height * yRatio);
        int cropWidth = (int) (width * widthRatio);
        int cropHeight = (int) (height * heightRatio);

        if (x + cropWidth > width) cropWidth = width - x;
        if (y + cropHeight > height) cropHeight = height - y;

        return image.getSubimage(x, y, cropWidth, cropHeight);
    }

    // 提取图号
    private static String extractFigureNumber(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        // 匹配包含 BJDCJC- 或 BIDCJC- 后跟三个两位数字段的模式
        Pattern pattern = Pattern.compile(".*(B[IJ]DCJC-\\d{2}-\\d{2}-\\d{2}).*");   //需要截取的文字片段
        Matcher matcher = pattern.matcher(text.replaceAll("\\s+", ""));
        if (matcher.find()) {
            String figureNumber = matcher.group(1);
            // 替换 "BIDCJC" 为 "BJDCJC"，并转换为大写
            figureNumber = figureNumber.replace("BIDCJC", "BJDCJC").toUpperCase();
            return figureNumber;
        }
        return null;
    }
}
