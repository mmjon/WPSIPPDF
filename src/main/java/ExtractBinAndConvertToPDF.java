import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.opc.PackagePart;
import java.io.*;
import java.util.*;

public class ExtractBinAndConvertToPDF {
    private static final Logger logger = LogManager.getLogger(ExtractBinAndConvertToPDF.class);

    public static void main(String[] args) {
        String excelFilePath = "xxx.xlsx";  //excel文件路径
        String outputDir = "output/";    //输出目录

        try {
            // 验证输入文件
            File excelFile = new File(excelFilePath);
            if (!excelFile.exists() || !excelFile.canRead()) {
                logger.error("Excel 文件不存在或无法读取: {}", excelFilePath);
                return;
            }
            logger.info("开始处理 Excel 文件: {}", excelFilePath);

            // 确保输出目录存在
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists() && !outputDirFile.mkdirs()) {
                logger.error("无法创建输出目录: {}", outputDir);
                return;
            }

            // 读取 Excel 文件
            try (FileInputStream fis = new FileInputStream(excelFilePath);
                 XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
                logger.info("成功加载 Excel 文件: {}", excelFilePath);

                // 获取所有嵌入对象
                List<PackagePart> packageParts = workbook.getAllEmbeddedParts();
                logger.info("找到 {} 个嵌入对象", packageParts.size());

                int pdfCount = 0;

                // 处理每个嵌入对象
                for (PackagePart part : packageParts) {
                    String contentType = part.getContentType();
                    String partName = part.getPartName().getName();
                    logger.debug("嵌入对象: {}, ContentType: {}, Size: {} bytes",
                            partName, contentType, part.getSize());

                    // 尝试转换为 PDF
                    try (InputStream is = part.getInputStream()) {
                        byte[] data = readStream(is);
                        String content = new String(data, "ISO-8859-1");
                        // 查找 PDF 数据起始位置
                        int pdfStart = content.indexOf("%PDF-");
                        if (pdfStart != -1) {
                            pdfCount++;
                            // 使用默认命名
                            String fileName = "converted_pdf_" + pdfCount + ".pdf";
                            String outputFileName = outputDir + fileName;

                            // 保存 PDF 数据（添加 %PDF-1.7 并跳过头部）
                            try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
                                fos.write("%PDF-1.7\n".getBytes());
                                fos.write(data, pdfStart, data.length - pdfStart);
                            }
                            logger.info("转换后的 PDF 已保存至: {}", outputFileName);
                        } else {
                            logger.warn("未在对象 {} 中找到 PDF 数据", partName);
                        }
                    } catch (Exception e) {
                        logger.warn("转换对象 {} 为 PDF 失败: {}", partName, e.getMessage());
                    }
                }

                // 打印统计信息
                if (pdfCount == 0) {
                    logger.warn("未找到可转换为 PDF 的嵌入对象");
                } else {
                    logger.info("成功转换 {} 个 PDF 文件", pdfCount);
                }

            }

        } catch (Exception e) {
            logger.error("处理 Excel 文件时发生错误: {}", e.getMessage(), e);
        }
    }

    private static byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
    }
}
