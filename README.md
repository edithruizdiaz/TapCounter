# TapCounter
Algorithm that computes trip costs

## Prerequisites

You will need the following things properly installed on your computer.

* [Git](https://git-scm.com/)
* [Java 8+](https://www.oracle.com/java/technologies/downloads/)
* [Maven](https://maven.apache.org/)


## Running / Development
* Open the console 
* Execute mvn install on the root of the project
* Execute ./mvnw spring-boot:run to run SpringBoot or open in a IDE of your preference
* Open your browser at [http://localhost:8080](http://localhost:8080)
* You can find a file example (taps.csv) on the demo folder named
* Upload the file and after the processing end it will download a new csv file with the result

## Assumptions
* Stops id is a numeric value
* Records corresponds to the same bus, and the same company
* Each row of the record is complete with all columns
* Only the stops mentioned in the problem
* There are only the 3 stops mentioned in the statement of the problem
