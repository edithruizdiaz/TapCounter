package com.littlepay.tapcounter.service;

import com.littlepay.tapcounter.bean.Tap;
import com.littlepay.tapcounter.bean.Trip;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class TripService {

    private final String COMMA_DELIMITER = ",";

    private enum TRIP_STATES {
        COMPLETED("COMPLETED"),
        INCOMPLETED("INCOMPLETED"),
        CANCELED("CANCELED");


        private final String label;

        TRIP_STATES(String label) {
            this.label = label;
        }
    }

    private enum TAP_TYPE {
        ON("ON"),
        OFF("OFF");


        private final String label;

        TAP_TYPE(String label) {
            this.label = label;
        }
    }


    /**
     * Method that read a csv file and compute the trips and store the data into a CSV file
     *
     * @return a csv file
     */
    public File calculateTripsToCsv(MultipartFile multipartFile) throws IOException, ParseException {

        //Read the csv file
        List<Tap> tapList = readCsvFile(multipartFile);
        //Calculate trip costs
        List<Trip> tripList = calculateTripCost(tapList);
        //Create and return Csv file
        return createCsv(tripList);
    }


    /**
     * Method that iterates the csv file and converts the records to a list
     *
     * @return the list of taps
     */
    private List<Tap> readCsvFile(MultipartFile multipartFile) throws IOException, ParseException {

        List<Tap> tapList = new ArrayList<>();

        BufferedReader br;

        InputStream is = multipartFile.getInputStream();
        br = new BufferedReader(new InputStreamReader(is));

        String line;
        int index = 0;

        while ((line = br.readLine()) != null) {
            if (index > 0) {
                String[] values = line.split(COMMA_DELIMITER);
                Tap tap = new Tap();
                tap.setId(new Long(values[0]));
                tap.setDateTime(convertStringToTimeStamp(values[1].replace("\"", "")));
                tap.setTapType(values[2]);
                //Extract the number from the stop and convert to int type so we can have just the stop code
                tap.setStopID(Integer.valueOf(values[3].substring(4, values[3].length())));
                tap.setCompanyId(values[4]);
                tap.setBusId(values[5]);
                tap.setPan(values[6]);
                tapList.add(tap);
            }
            index++;
        }


        return tapList;
    }


    /**
     * Method that parses the list of taps and maps taps on with taps off and found incompleted taps, compute the trip
     * costs, status, duration
     *
     * @param tapList the lists of passengers taps
     * @return a lists of trips with costs and status
     */
    private List<Trip> calculateTripCost(List<Tap> tapList) {
        //This constant represents a value when the passenger did not tap off. It is used for time and destination stop id
        final int didnottapoff = -1;

        List<Trip> trips = new ArrayList<>();

        for (int i = 0; i < tapList.size(); i++) {
            Trip trip = new Trip();

            for (int j = i + 1; j < tapList.size(); j++) {

                if (tapList.get(i).getPan().equalsIgnoreCase(tapList.get(j).getPan())) {
                    //completed trip
                    if (tapList.get(i).getTapType().equals(TAP_TYPE.ON.label) && tapList.get(j).getTapType().equals(TAP_TYPE.OFF.label) &&
                            !tapList.get(i).isVerified() && !tapList.get(j).isVerified()) {
                        trip.setStarted(tapList.get(i).getDateTime());
                        trip.setFinished(tapList.get(j).getDateTime());
                        //calculates the trip duration in seconds
                        trip.setDuration((trip.getFinished().getTime() - trip.getStarted().getTime()) / 1000);
                        trip.setFromStopId(tapList.get(i).getStopID());
                        trip.setToStopId(tapList.get(j).getStopID());
                        trip.setChargeAmount(calculateCost(trip.getFromStopId(), trip.getToStopId()));
                        trip.setCompanyId(tapList.get(i).getCompanyId());
                        trip.setBusId(tapList.get(i).getBusId());
                        trip.setPan(tapList.get(i).getPan());
                        if (tapList.get(i).getStopID() == tapList.get(j).getStopID()) {
                            trip.setStatus(TRIP_STATES.CANCELED.label);
                        } else {
                            trip.setStatus(TRIP_STATES.COMPLETED.label);
                        }
                        trips.add(trip);
                        tapList.get(i).setVerified(true);
                        tapList.get(j).setVerified(true);

                        break;
                        //incompleted trip
                    } else if (tapList.get(i).getTapType().equals(TAP_TYPE.ON.label) && tapList.get(j).getTapType().equals(TAP_TYPE.ON.label) &&
                            !tapList.get(i).isVerified() && !tapList.get(j).isVerified()) {
                        trip.setStarted(tapList.get(i).getDateTime());
                        //calculates the trip duration in seconds
                        trip.setDuration(didnottapoff);
                        trip.setFromStopId(tapList.get(i).getStopID());
                        trip.setToStopId(didnottapoff);
                        trip.setChargeAmount(calculateCost(trip.getFromStopId(), trip.getToStopId()));
                        trip.setCompanyId(tapList.get(i).getCompanyId());
                        trip.setBusId(tapList.get(i).getBusId());
                        trip.setPan(tapList.get(i).getPan());
                        trip.setStatus(TRIP_STATES.INCOMPLETED.label);
                        trips.add(trip);
                        tapList.get(i).setVerified(true);
                        break;
                    }
                }
            }
            //incompleted trip
            if (!tapList.get(i).isVerified()) {
                trip.setStarted(tapList.get(i).getDateTime());
                trip.setDuration(didnottapoff);
                trip.setFromStopId(tapList.get(i).getStopID());
                trip.setToStopId(didnottapoff);
                trip.setChargeAmount(calculateCost(trip.getFromStopId(), trip.getToStopId()));
                trip.setCompanyId(tapList.get(i).getCompanyId());
                trip.setBusId(tapList.get(i).getBusId());
                trip.setPan(tapList.get(i).getPan());
                trip.setStatus(TRIP_STATES.INCOMPLETED.label);
                trips.add(trip);
                tapList.get(i).setVerified(true);
            }
        }

        return trips;
    }


    /**
     * Method that create and save a CSV file
     *
     * @param trips the list of trips to be stored in the file
     * @throws IOException
     */
    private File createCsv(List<Trip> trips) throws IOException {
        File reportFile = new File("trips.csv");
        FileWriter outputfile = new FileWriter(reportFile);

        List<String[]> csvLines = createTripRegisters(trips);

        CSVWriter writer = new CSVWriter(outputfile);
        writer.writeAll(csvLines);
        writer.close();

        return reportFile;
    }


    /**
     * Method that converts a list of the trips in a String array format
     *
     * @param trips is the list of trips that should be stored on the file
     * @return the list of the trips in an array format
     */
    private List<String[]> createTripRegisters(List<Trip> trips) {

        String[] header = {"Started", "Finished", "DurationSecs", "FromStopId", "ToStopId", "ChargeAmount", "CompanyId",
                "BusID", "PAN", "Status"};
        List<String[]> list = new ArrayList<>();
        list.add(header);
        for (Trip t : trips) {
            String[] line = {t.getStarted().toString(), ((t.getFinished() == null) ? "N/A" : t.getFinished().toString()),
                    Long.toString(t.getDuration()), String.valueOf(t.getFromStopId()), String.valueOf(t.getToStopId()),
                    String.valueOf(t.getChargeAmount()), t.getCompanyId(), t.getBusId(), t.getPan(), t.getStatus()};
            list.add(line);
        }

        return list;
    }


    /**
     * Method that converts a String value to Timestamp value
     *
     * @param dateTime the value to convert
     * @return the value in Timestamp type
     */
    private Timestamp convertStringToTimeStamp(String dateTime) throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        Date parsedDate = dateFormat.parse(dateTime);
        return new java.sql.Timestamp(parsedDate.getTime());
    }


    /**
     * Calculate the trip costs from a cost pre-filled matrix
     * If the passenger doesn't tap off, the value of the toStopId is -1 and
     *
     * @param fromStopId origin stop
     * @param toStopId   destination stop. The value is -1 if the passenger doesn't tap off
     * @return the cost of the trip
     */
    public double calculateCost(int fromStopId, int toStopId) {

        double[][] costArray = {{0, 3.25, 7.3},
                {3.25, 0, 5.5},
                {7.3, 5.5, 0}};
        double cost;
        //If the passenger doesn t tap off
        if (toStopId < 0) {
            //Calculate the maximun cost from the stop
            double maxCost = 0;
            for (int i = 0; i < costArray[fromStopId - 1].length; i++) {
                if (costArray[fromStopId - 1][i] > maxCost) {
                    maxCost = costArray[fromStopId - 1][i];
                }
            }
            cost = maxCost;
        } else {
            //Obtain the cost from the array
            cost = costArray[fromStopId - 1][toStopId - 1];
        }
        return cost;
    }

}
