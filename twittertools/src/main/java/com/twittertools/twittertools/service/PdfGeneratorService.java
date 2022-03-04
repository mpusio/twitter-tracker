package com.twittertools.twittertools.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import com.twittertools.twittertools.entity.User;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGeneratorService {
    public void export(HttpServletResponse response, List<User> users) throws IOException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        fontTitle.setSize(18);

        Paragraph paragraph = new Paragraph("New following report", fontTitle);
        paragraph.setAlignment(Paragraph.ALIGN_CENTER);

        Font fontParagraph = FontFactory.getFont(FontFactory.HELVETICA);
        fontParagraph.setSize(12);

        Font fontFollowing = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
        Font fontDescription = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font fontLink = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLUE);
        Font fontLink2 = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLUE);

        document.add(paragraph);
        document.addAuthor("michu");
        document.addCreationDate();

        for (User user : users) {
            Image instance = Image.getInstance("https://img.redro.pl/obrazy/znak-zapytania-na-glowie-ilustracji-wektorowych-400-120453337.jpg");
            try{
                instance = Image.getInstance(user.getProfileImageUrl());
            }
            catch (Exception ignored){}

            document.add(instance);
            document.add(new Paragraph(user.getName(), fontParagraph));
            Chunk chunk2 = new Chunk(user.getUsername(), fontLink2);
            chunk2.setAnchor("https://twitter.com/" + user.getUsername());
            document.add(chunk2);
            document.add(new Paragraph(user.getDescription(), fontDescription));
            document.add(new Paragraph("New following:", fontFollowing));

            if (user.getUsers().size()==0){
                document.add(new Paragraph("No new followers.", fontDescription));
                continue;
            }

            for (User following : user.getUsers()) {
                Image image = Image.getInstance(following.getProfileImageUrl());
                try{
                    image = Image.getInstance(following.getProfileImageUrl());
                }
                catch (Exception ignored){}

                image.scalePercent(50);
                document.add(Image.getInstance(image));
                Chunk chunk = new Chunk(following.getUsername(), fontLink);
                chunk.setAnchor("https://twitter.com/" + following.getUsername());
                document.add(chunk);
                document.add(new Paragraph(following.getDescription(), fontDescription));
            }
        }

        document.add(new Chunk("Created by michu ;)"));
        document.close();
    }
}
