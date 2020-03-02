package net.semanticmetadata.lire.sampleapp;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import net.semanticmetadata.lire.aggregators.BOVW;
import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.local.simple.SimpleExtractor;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import net.semanticmetadata.lire.sampleapp.IndexingAndSearchWithLocalFeatures;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class MetadataFilter extends IndexingAndSearchWithLocalFeatures{
    public static String  IMG_DIRECTORY = "/Users/juanluisrto/Documents/Universidad/UPM/information retrieval/samples/simpleapplication/img/flickr30k_images/tiny";
    public static String IMG_TO_SEARCH = "/Users/juanluisrto/Documents/Universidad/UPM/information retrieval/samples/simpleapplication/img/flickr30k_images/search/952262215.jpg";


    public static void main(String[] args) throws IOException, ImageProcessingException {
        // indexing all images in "testdata"
        index("index", IMG_DIRECTORY);
        // searching through the images.
        search("index",IMG_TO_SEARCH, 350, 350);
    }

    public static void search(String indexPath,String imgPath, int minHeight, int minWidth) throws IOException, ImageProcessingException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

        // make sure that this matches what you used for indexing (see below) ...
        ImageSearcher imgSearcher = new GenericFastImageSearcher(10, CEDD.class, SimpleExtractor.KeypointDetector.CVSURF, new BOVW(), 128, true, reader, indexPath + ".config");
        // just a static example with a given image.
        ImageSearchHits hits = imgSearcher.search(ImageIO.read(new File(imgPath)), reader);

        ArrayList<ImageHit> filteredImages = new ArrayList<ImageHit> ();

        for (int i=0; i<hits.length(); i++) {
            String hitPath = reader.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            Metadata metadata = ImageMetadataReader.readMetadata(new File(hitPath));
            int height = 0, width = 0;
            for (Directory directory : metadata.getDirectories()) {
                if (directory.getName().equals("JPEG")){
                    for (Tag tag : directory.getTags()) {
                        //System.out.println("TAG: " + tag.getTagName() + " = " + tag.getDescription());
                        if(tag.getTagName().equals("Image Height")){
                            height = extractPixels(tag.getDescription());
                        }
                        else if(tag.getTagName().equals("Image Width")){
                            width = extractPixels(tag.getDescription());
                        }
                    }
                }
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        System.err.format("ERROR: %s", error);
                    }
                }
            }
            if (height > minHeight && width > minWidth){
                ImageHit image = new ImageHit(hitPath, hits.score(i), height, width);
                filteredImages.add(image);
            }
        }

        for (ImageHit  image : filteredImages){
            System.out.println(image.toString());

        }
        //System.out.printf("%.2f: (%d) %s --- %s\n", hits.score(i), hits.documentID(i), metadata.getDirectoryCount(), reader.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0]);
        //Metadata metadata = ImageMetadataReader.readMetadata(new File(IMG_TO_SEARCH));



    }
    public static int extractPixels(String pixels){
        String number = pixels.split(" ")[0];
        return Integer.parseInt(number);
    };
}


class ImageHit {
    public String path;
    public double score;
    public int height;
    public int width;

    public ImageHit(String path, double score, int height, int width){
        this.path = path;
        this.score = score;
        this.height = height;
        this.width = width;
    }

    public String toString(){
        return "Score: " + score + " [" + String.valueOf(height) + "x" + String.valueOf(width) + "] - path = " + path;
    }
}
