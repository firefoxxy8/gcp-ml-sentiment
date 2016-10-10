package io.pivotal;

import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import io.pivotal.Domain.QueryResultsViewMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mross on 10/10/16.
 */

@Controller
public class WebController {


    @RequestMapping("/")
    public String renderIndex(Model model) {

        return "index";
    }

    @RequestMapping("/results")
    public String renderResults(Model model) {
        return "results";
    }


    @RequestMapping(value="/upload", method = RequestMethod.POST)
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {


        QueryResultsViewMapping viewMapping = new QueryResultsViewMapping();
        System.out.println(file.getContentType());
        System.out.println(file.getName());
        System.out.println("You successfully uploaded " + file.getOriginalFilename() + "!");
        redirectAttributes.addFlashAttribute("message",
                "Processed File " + file.getOriginalFilename() + "!");


        VisionApiService vps = new VisionApiService();
        ArrayList<QueryResultsViewMapping> queryResults = new ArrayList<>();

        try {
            List<EntityAnnotation> landmarkInfoArray = vps.identifyLandmark(file.getBytes(),10);
            EntityAnnotation landmarkResult = landmarkInfoArray.get(0);
            String landmarkName = landmarkResult.getDescription();
            System.out.println("Landmark (from Vision API): " + landmarkName);

            BigQueryApiService bqs = new BigQueryApiService();
            String query = String.format(
                    "SELECT BookMeta_Title, BookMeta_Creator, BookMeta_Subjects FROM"
                            + " (TABLE_QUERY([gdelt-bq:internetarchivebooks], 'REGEXP_EXTRACT(table_id, r\"(\\d{4})\")"
                            + " BETWEEN \"1819\" AND \"2014\"')) WHERE LOWER(BookMeta_Subjects) CONTAINS LOWER(\"%s\")"
                    , landmarkName);

            java.util.List<TableRow> results =bqs.executeQuery(query);
            if (results != null) {
                for (TableRow row : results) {
                    viewMapping = new QueryResultsViewMapping(row);
                    queryResults.add(viewMapping);



                    System.out.println();
                }
            } else {
                System.out.println("Results are null");
                queryResults.add(new QueryResultsViewMapping());
            }
            System.out.println(queryResults);


            redirectAttributes.addFlashAttribute("queryResults",
                    queryResults);
        }  catch (Exception e) {
            System.out.println("THERE WAS EXCEPTION");
            System.out.println(e);
            redirectAttributes.addFlashAttribute("alert",
                    "There was a problem processing your file, please try another image");

            return "redirect:/";

        }
//        redirectAttributes.addFlashAttribute("alert",
//                "");

        return "redirect:/results";
    }
}
