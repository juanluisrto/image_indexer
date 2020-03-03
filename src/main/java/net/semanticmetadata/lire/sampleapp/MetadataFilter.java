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

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class MetadataFilter extends IndexingAndSearchWithLocalFeatures{
    // Path to directory with all the images to be indexed
    public static String  IMG_DIRECTORY = "/Users/juanluisrto/Documents/Universidad/UPM/information retrieval/samples/simpleapplication/img/flickr30k_images/tiny";
    // Path to image to which is going to be searched
    public static String IMG_TO_SEARCH = "/Users/juanluisrto/Documents/Universidad/UPM/information retrieval/samples/simpleapplication/img/flickr30k_images/search/952262215.jpg";


    public static void main(String[] args) throws IOException, ImageProcessingException {
        // indexing all images in "IMG_DIRECTORY"
        index("index", IMG_DIRECTORY);
        // searching through the images.
        search("index",IMG_TO_SEARCH, 350, 350);
    }
    /**
     * Our extension to the LIRE framework consists in adding a filter which selects the images which have certain size dimensions.
     * To know the size of the images we use the metadata-extractor package, and then we filter out the ones that are too small.
     * @param indexPath : Path to the folder where the index is stored.
     * @param imgPath: Path to the img which is going to be searched
     * @param minHeight: Minimum height desired for the images
     * @param minWidth: Minimum widht desired for the images
    */
    public static void search(String indexPath, String imgPath, int minHeight, int minWidth) throws IOException, ImageProcessingException {
        //We initialize the reader which will retrieve the index information from the stored file.
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

        //We initialize an image searcher
        ImageSearcher imgSearcher = new GenericFastImageSearcher(10, CEDD.class, SimpleExtractor.KeypointDetector.CVSURF, new BOVW(), 128, true, reader, indexPath + ".config");

        ImageSearchHits hits = imgSearcher.search(ImageIO.read(new File(imgPath)), reader);
        // We initialize this arraylist to store the hits which comply to our requested size,
        ArrayList<ImageHit> filteredImages = new ArrayList<ImageHit> ();

        for (int i=0; i<hits.length(); i++) {
            String hitPath = reader.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            Metadata metadata = ImageMetadataReader.readMetadata(new File(hitPath));
            int height = 0, width = 0;
            for (Directory directory : metadata.getDirectories()) {
                if (directory.getName().equals("JPEG")){
                    for (Tag tag : directory.getTags()) {
                        // we store the image height
                        if(tag.getTagName().equals("Image Height")){
                            height = extractPixels(tag.getDescription());
                        }
                        // we store the image width
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
            // if the height and width are greater than required, then we create a new ImageHit object and store it on filteredImages.
            if (height > minHeight && width > minWidth){
                ImageHit image = new ImageHit(hitPath, hits.score(i), height, width);
                filteredImages.add(image);
            }
        }

        for (ImageHit  image : filteredImages){
            // We just print out the images which are similar and comply with the height and width requirements.
            System.out.println(image.toString());

        }


    }
    public static int extractPixels(String pixels){
        String number = pixels.split(" ")[0];
        return Integer.parseInt(number);
    };
}


class ImageHit {
    public String path; //path to the image
    public double score; //score obtained
    public int height; //height of the image in pixels
    public int width;  //width of the image in pixels

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
