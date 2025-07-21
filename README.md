<font style="color:rgb(31, 35, 40);">批量导出WPS的excel表格中的PDF文件</font>

<font style="color:rgb(31, 35, 40);">文件中包含两个java类分别是</font><font style="color:#080808;background-color:#ffffff;">ExtractBinAndConvertToPDF和SingleImagePdfRecognitionApp</font>

## <font style="color:#080808;background-color:#ffffff;">ExtractBinAndConvertToPDF</font>
### 作用
<font style="color:rgb(31, 35, 40);">主要用于提取excel中的pdf文件</font>

### <font style="color:rgb(31, 35, 40);">使用方法</font>
首先我们需要更新我们pom文件中的相关依赖

之后将文件中的<font style="color:#080808;background-color:#ffffff;">excelFilePath和outputDir改为我们的文件路径和pdf输出路径</font>

 之后运行即可

## <font style="color:#080808;background-color:#ffffff;">SingleImagePdfRecognitionApp</font>
### 作用
<font style="color:rgb(31, 35, 40);">使用ocr识别pdf中特定位置的文字来给我们的pdf文件进行重命名（需要下载Tesseract-OCR以及相关语言包，可以到官网进行下载</font>[https://tesseract-ocr.cn/tessdoc/](https://tesseract-ocr.cn/tessdoc/)<font style="color:rgb(31, 35, 40);">）这里建议下载Tesseract-best中的语言包</font>

### <font style="color:rgb(31, 35, 40);">使用方法</font>
同上，需要修改<font style="color:#080808;background-color:#ffffff;">tessDataPath和path路径  tessDataPath为</font><font style="color:rgb(31, 35, 40);">Tesseract-OCR语言包路径  </font><font style="color:#080808;background-color:#ffffff;">path为pdf文件路径</font>

##   
  

### <font style="color:rgb(31, 35, 40);">  
</font>
