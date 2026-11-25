package org.example.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.example.Entities.Train;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TrainService {

    private List<Train> trainList;

    public static final String TRAIN_PATH =
            "/home/shovanparia/HelloGradle/app/src/main/java/org/example/LocalDb/Train.json";

    private final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {

        ObjectMapper mapper = new ObjectMapper();

        mapper.setPropertyNamingStrategy(
                PropertyNamingStrategies.SNAKE_CASE
        );

        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );

        return mapper;
    }

    // Constructor

    public TrainService() throws IOException {
        loadTrainsFromFile();
    }

    //  Load trains from JSON file

    public synchronized void loadTrainsFromFile() throws IOException {

        File trainsFile = new File(TRAIN_PATH);

        if (!trainsFile.exists() || trainsFile.length() == 0) {

            trainList = new ArrayList<>();

            saveTrainListToFile();

            return;
        }

        trainList = objectMapper.readValue(
                trainsFile,
                new TypeReference<List<Train>>() {}
        );

        if (trainList == null) {
            trainList = new ArrayList<>();
        }
    }

    //  Save train list into JSON

    public synchronized void saveTrainListToFile() throws IOException {

        File trainsFile = new File(TRAIN_PATH);

        File parent = trainsFile.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(trainsFile, trainList);
    }

    // 3. Fetch seats

    public List<List<Integer>> fetchSeats(Train train) {

        if (train == null || train.getSeats() == null) {
            return new ArrayList<>();
        }

        return train.getSeats();
    }

    // . Find train by ID

    public synchronized Train getTrainById(String trainId) {

        if (trainId == null || trainList == null) {
            return null;
        }

        return trainList.stream()
                .filter(train -> train != null)
                .filter(train -> train.getTrainId() != null)
                .filter(train ->
                        train.getTrainId().equalsIgnoreCase(trainId)
                )
                .findFirst()
                .orElse(null);
    }

    //  Validate source and destination

    public boolean validTrain(Train train, String src, String dest) {

        if (train == null ||
                src == null ||
                dest == null ||
                train.getStations() == null) {
            return false;
        }

        List<String> stations = train.getStations();

        int srcIdx = -1;
        int destIdx = -1;

        for (int i = 0; i < stations.size(); i++) {

            String station = stations.get(i);

            if (station == null) {
                continue;
            }

            if (station.equalsIgnoreCase(src)) {
                srcIdx = i;
            }

            if (station.equalsIgnoreCase(dest)) {
                destIdx = i;
            }
        }

        return srcIdx != -1 &&
                destIdx != -1 &&
                srcIdx < destIdx;
    }

    //  Search trains

    public synchronized List<Train> getTrains(
            String source,
            String dest
    ) {

        if (source == null || dest == null || trainList == null) {
            return new ArrayList<>();
        }

        return trainList.stream()
                .filter(train ->
                        validTrain(train, source, dest)
                )
                .collect(Collectors.toList());
    }

    //  Book train seat
    // User enters 1-based row and seat numbers.

    public synchronized boolean bookTrainSeat(
            Train train,
            int row,
            int seat
    ) {

        if (train == null || train.getSeats() == null) {
            return false;
        }

        int rowIndex = row - 1;
        int seatIndex = seat - 1;

        List<List<Integer>> seats = train.getSeats();

        // Validate row

        if (rowIndex < 0 || rowIndex >= seats.size()) {
            return false;
        }

        // Validate seat

        if (seats.get(rowIndex) == null ||
                seatIndex < 0 ||
                seatIndex >= seats.get(rowIndex).size()) {
            return false;
        }

        Integer currentSeat = seats.get(rowIndex).get(seatIndex);

        // 0 = Available, 1 = Booked

        if (currentSeat == null || currentSeat != 0) {
            return false;
        }

        // Book seat

        seats.get(rowIndex).set(seatIndex, 1);

        try {

            updateTrain(train);

            return true;

        } catch (IOException e) {

            // Rollback

            seats.get(rowIndex).set(seatIndex, 0);

            throw new UncheckedIOException(
                    "Unable to save train booking", e
            );

        } catch (RuntimeException e) {

            seats.get(rowIndex).set(seatIndex, 0);

            throw e;
        }
    }

    //  Release seat
    // Used when saving a user's ticket fails.

    public synchronized boolean releaseTrainSeat(
            Train train,
            int row,
            int seat
    ) {

        if (train == null || train.getSeats() == null) {
            return false;
        }

        int rowIndex = row - 1;
        int seatIndex = seat - 1;

        List<List<Integer>> seats = train.getSeats();

        if (rowIndex < 0 || rowIndex >= seats.size()) {
            return false;
        }

        if (seats.get(rowIndex) == null ||
                seatIndex < 0 ||
                seatIndex >= seats.get(rowIndex).size()) {
            return false;
        }

        Integer currentSeat = seats.get(rowIndex).get(seatIndex);

        if (currentSeat == null || currentSeat != 1) {
            return false;
        }

        // Make seat available again.

        seats.get(rowIndex).set(seatIndex, 0);

        try {

            updateTrain(train);

            return true;

        } catch (IOException e) {

            // Restore booked state if saving fails.

            seats.get(rowIndex).set(seatIndex, 1);

            throw new UncheckedIOException(
                    "Unable to release train seat", e
            );

        } catch (RuntimeException e) {

            seats.get(rowIndex).set(seatIndex, 1);

            throw e;
        }
    }

    //  Update train in local JSON database

    private synchronized void updateTrain(Train train)
            throws IOException {

        if (train == null || train.getTrainId() == null) {
            throw new IllegalArgumentException("Invalid train!");
        }

        for (int i = 0; i < trainList.size(); i++) {

            Train existingTrain = trainList.get(i);

            if (existingTrain != null &&
                    existingTrain.getTrainId() != null &&
                    existingTrain.getTrainId()
                            .equalsIgnoreCase(train.getTrainId())) {

                trainList.set(i, train);

                saveTrainListToFile();

                return;
            }
        }

        throw new IllegalArgumentException("Train not found!");
    }
}