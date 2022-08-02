package com.littlepay.tapcounter.controller;

import com.littlepay.tapcounter.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.ParseException;

@ServletComponentScan
@RestController
@RequestMapping("/trip")
public class TripController {

    @Autowired
    TripService tripService;

    @PostMapping(value = "/costs")
    @ResponseBody
    public ResponseEntity calculateCosts(@RequestParam("file") MultipartFile multipartFile, HttpServletResponse response, ModelMap modelMap) {


        try {
            File reportFile = tripService.calculateTripsToCsv(multipartFile);
            String reportFileName = "trip_cost.csv";
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"" + reportFileName + "\""));
            response.setContentLength((int) reportFile.length());
            InputStream inputStream = new BufferedInputStream(new FileInputStream(reportFile));

            FileCopyUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();

            return new ResponseEntity<>(reportFile, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("IOException when processing the file");
        } catch (ParseException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("ParseException when processing the file");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Generic error when processing the file");
        }

    }

}
