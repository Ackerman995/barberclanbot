package com.ivan_degtev.telegrambotforpapablinov.mapper;


import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class PdfCompressor {
    public File compressPdfWithImages(File inputFile, String outputFilePath) throws IOException {
        try (PDDocument originalDocument = PDDocument.load(inputFile)) {
            PDDocument compressedDocument = new PDDocument();
            PDFRenderer renderer = new PDFRenderer(originalDocument);

            for (int pageIndex = 0; pageIndex < originalDocument.getNumberOfPages(); pageIndex++) {
                // Рендеринг страницы в изображение с увеличенным DPI
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 150); // Измените DPI на 150

                // Создание новой страницы в сжатом документе
                PDPage page = new PDPage(originalDocument.getPage(pageIndex).getMediaBox());
                compressedDocument.addPage(page);

                // Масштабирование изображения под размер страницы
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                // Рисование изображения на странице
                try (PDPageContentStream contentStream = new PDPageContentStream(compressedDocument, page)) {
                    contentStream.drawImage(LosslessFactory.createFromImage(compressedDocument, image), 0, 0, pageWidth, pageHeight);
                }
            }

            compressedDocument.save(outputFilePath);
            compressedDocument.close();
            return new File(outputFilePath);
        }

    }



}
