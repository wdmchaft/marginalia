/**
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3
 * as published by the Free Software Foundation.
 */
package de.gbv.marginalia;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import jargs.gnu.CmdLineParser;

import com.itextpdf.text.Rectangle;

import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfBoolean;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfRectangle;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;

import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import de.gbv.xml.SimpleXMLWriter;
import de.gbv.xml.SimpleXMLCreator;

/**
 * Marginalia command line client.
 *
 * This is just a skeleton that can only parse PDF files and list all 
 * annotations for each page. To extend this script, you need to have 
 * a look at the PDF file format specification and iText.
 */
public class Marginalia {

    private static void printUsage() {
        System.err.println("usage: marginalia file.pdf");
    }

    public static void main( String[] args ) {
        if ( args.length == 0 ) {
            printUsage();
            System.exit(2);
        }

        try {
            PrintWriter writer = new PrintWriter(System.out);

            // String[] otherArgs = parser.getRemainingArgs();
            for ( int i = 0; i < args.length; ++i ) {
                inspect(writer, args[i]);

                // other actions:
                // remove all annotations from the document
                // reader.removeAnnotations()
            }

            writer.close();
        } catch (Exception e) {
            //System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
     * Inspect a PDF file and write the info to a writer
     * @param writer Writer to a text file
     * @param filename Path to the PDF file
     * @throws IOException
     */
    public static void inspect(PrintWriter writer, String filename)
        throws IOException, SAXException {
//        writer.println(filename);
        writer.flush();

        PdfReader reader = new PdfReader(filename);

        ContentHandler xmlhandler = new SimpleXMLWriter(writer);
        xmlhandler.startDocument();

        SimpleXMLCreator xml = new SimpleXMLCreator( xmlhandler, Annotation.namespaces, true );

/*
        writer.println("Number of pages: "+reader.getNumberOfPages());
        Rectangle mediabox = reader.getPageSize(1);
        writer.print("Size of page 1: [");
        writer.print(mediabox.getLeft());
        writer.print(',');
        writer.print(mediabox.getBottom());
        writer.print(',');
        writer.print(mediabox.getRight());
        writer.print(',');
        writer.print(mediabox.getTop());
        writer.println("]");
        writer.print("Rotation of page 1: ");
        writer.println(reader.getPageRotation(1));
        writer.print("Page size with rotation of page 1: ");
        writer.println(reader.getPageSizeWithRotation(1));
        writer.println();
        writer.flush();
*/
        List<Annotation> annots = new LinkedList<Annotation>();
        xml.startElement("annots");

       // TODO: The following elements may be added:
       // - optionally write <f href="Document.pdf"/>
       // - optionally write <ids original="ID" modified="ID" />
 
        xml.startElement("m","pages");
        for (int pageNum=1; pageNum<=reader.getNumberOfPages(); pageNum++) {
            PdfDictionary pageDic = reader.getPageN(pageNum);

            Map <String,String> attr = new HashMap<String,String>();
            attr.put("number", ""+pageNum );
            attr.put("rotate", ""+reader.getPageRotation(pageNum) );

            Rectangle mediabox = reader.getPageSize(pageNum);
            attr.put("left", ""+mediabox.getLeft());
            attr.put("bottom", ""+mediabox.getBottom() );
            attr.put("right", ""+mediabox.getRight());
            attr.put("top", ""+mediabox.getTop());

            xml.contentElement("m","page","",attr);

            PdfArray rawannots = pageDic.getAsArray(PdfName.ANNOTS);
            if ( rawannots == null || rawannots.isEmpty() ) {
                // writer.println("page "+pageNum+" contains no annotations");
                continue;
            }

            // writer.println("page "+pageNum+" has "+rawannots.size()+" annotations");

            for(int i=0; i<rawannots.size(); i++) {
                PdfObject obj = rawannots.getDirectObject(i);
                if (!obj.isDictionary()) continue;
                Annotation a = new Annotation( (PdfDictionary)obj, pageNum );
                annots.add(a);
            }

            /**
            // Now we have all highlight and similar annotations, we need
            // to find out what words are actually highlighted! PDF in fact
            // is a dump format to express documents.
            // For some hints see
            // http://stackoverflow.com/questions/4028240/extract-each-column-of-a-pdf-file

            // We could reuse code from LocationTextExtractionStrategy (TODO)
            // LocationTextExtractionStrategy extr = new LocationTextExtractionStrategy();
            String fulltext = PdfTextExtractor.getTextFromPage(reader,pageNum);//,extr
            writer.println(fulltext);
            */
        }
        xml.endElement();

        for (Annotation a : annots) {
            a.serializeXML( xmlhandler );
        }
        // TODO: add page information (page size and orientation)

        xml.endAll();
    }

    // helper class (to be removed)
    public static void dumpArray(PdfArray a) {
        if (a == null) return;
        for(int i=0; i<a.size(); i++) {
            System.out.println( i + a.getPdfObject(i).toString() );
        }
    }
}
