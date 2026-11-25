package org.example;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.example.Entities.Ticket;
import org.example.Entities.Train;
import org.example.Entities.User;

import org.example.Service.TrainService;
import org.example.Service.UserService;
import org.example.Utill.UserServiceUtil;

import java.io.IOException;
import java.util.*;

public class App {

    public static void main(String[] args) throws IOException {



        UserService userService = new UserService();
        TrainService trainService = new TrainService();

        Scanner scanner = new Scanner(System.in);

        boolean isLoggedIn = false;

        int option = 0;

        Train trainSelectedForBooking = null;

        String selectedSource = null;
        String selectedDestination = null;

        System.out.println("Running Train Booking System...");

        while (option != 7) {

            System.out.println("\n========== TRAIN BOOKING SYSTEM ==========");
            System.out.println("1. Sign up");
            System.out.println("2. Login");
            System.out.println("3. Fetch Bookings");
            System.out.println("4. Search Trains");
            System.out.println("5. Book a Seat");
            System.out.println("6. Cancel my Booking");
            System.out.println("7. Exit the App");

            System.out.println("Choose option:");

            option = scanner.nextInt();

            switch (option) {


                // 1. SIGNUP


                case 1: {

                    System.out.println("Enter username:");
                    String username = scanner.next();

                    System.out.println("Enter password:");
                    String password = scanner.next();

                    User newUser = new User(
                            username,
                            null,
                            UserServiceUtil.hashPassword(password),
                            new ArrayList<>(),
                            UUID.randomUUID().toString()
                    );

                    boolean registered = userService.signUp(newUser);

                    if (registered) {
                        System.out.println("Signup completed successfully!");
                    } else {
                        System.out.println("Username already exists!");
                    }

                    break;
                }


                // 2. LOGIN


                case 2: {

                    System.out.println("Enter username:");
                    String username = scanner.next();

                    System.out.println("Enter password:");
                    String password = scanner.next();

                    // Temporary object containing login credentials.

                    User loginAttempt = new User(
                            username,
                            password,
                            null,
                            new ArrayList<>(),
                            UUID.randomUUID().toString()
                    );

                    // Same UserService instance.

                    isLoggedIn = userService.loginUser(loginAttempt);

                    if (isLoggedIn) {

                        System.out.println("Login successful!");

                        System.out.println(
                                "Welcome " +
                                        userService.getLoggedInUser().getName()
                        );

                    } else {

                        System.out.println("Invalid username or password!");

                    }

                    break;
                }


                //  FETCH BOOKINGS


                case 3: {

                    if (!isLoggedIn) {
                        System.out.println("Please login first!");
                        break;
                    }

                    System.out.println("Fetching your bookings...");

                    List<Ticket> tickets = userService.fetchBooking();

                    if (tickets.isEmpty()) {

                        System.out.println("No bookings found!");

                    } else {

                        System.out.println("\nYour bookings:");

                        for (Ticket ticket : tickets) {
                            System.out.println(ticket);
                        }
                    }

                    break;
                }


                //  SEARCH TRAINS


                case 4: {

                    // Clear previous selection.

                    trainSelectedForBooking = null;
                    selectedSource = null;
                    selectedDestination = null;

                    System.out.println("Enter source station:");
                    String source = scanner.next();

                    System.out.println("Enter destination station:");
                    String destination = scanner.next();

                    List<Train> trains =
                            trainService.getTrains(source, destination);

                    if (trains.isEmpty()) {

                        System.out.println("No trains found for this route!");

                        break;
                    }

                    System.out.println("\nAvailable trains:");

                    for (int i = 0; i < trains.size(); i++) {

                        Train train = trains.get(i);

                        System.out.println(
                                (i + 1) +
                                        ". Train ID: " +
                                        train.getTrainId()
                        );

                        System.out.println(
                                "Train Number: " + train.getTrainNo()
                        );

                        if (train.getStationTimes() != null) {

                            for (Map.Entry<String, String> entry :
                                    train.getStationTimes().entrySet()) {

                                System.out.println(
                                        entry.getKey() +
                                                " time: " +
                                                entry.getValue()
                                );
                            }
                        }

                        System.out.println("-------------------------");
                    }

                    System.out.println("Select a train:");

                    int trainIndex = scanner.nextInt();

                    if (trainIndex < 1 || trainIndex > trains.size()) {

                        System.out.println("Invalid train selection!");

                        break;
                    }

                    trainSelectedForBooking = trains.get(trainIndex - 1);

                    selectedSource = source;
                    selectedDestination = destination;

                    System.out.println("Train selected successfully!");

                    break;
                }


                //  BOOK A SEAT


                case 5: {

                    if (!isLoggedIn) {
                        System.out.println("Please login first!");
                        break;
                    }

                    if (trainSelectedForBooking == null) {

                        System.out.println(
                                "Please search and select a train first!"
                        );

                        break;
                    }

                    System.out.println("\nAvailable seats:");

                    List<List<Integer>> seats =
                            trainService.fetchSeats(trainSelectedForBooking);

                    for (int i = 0; i < seats.size(); i++) {

                        System.out.print("Row " + (i + 1) + ": ");

                        for (int j = 0; j < seats.get(i).size(); j++) {

                            System.out.print(
                                    "Seat " + (j + 1) +
                                            "=" + seats.get(i).get(j) + "  "
                            );
                        }

                        System.out.println();
                    }

                    System.out.println("0 = Available, 1 = Booked");

                    System.out.println("Enter row number:");
                    int row = scanner.nextInt();

                    System.out.println("Enter seat number:");
                    int col = scanner.nextInt();

                    // Create Ticket before booking the seat.

                    Ticket ticket = createTicket(
                            trainSelectedForBooking,
                            selectedSource,
                            selectedDestination,
                            row,
                            col
                    );

                    if (ticket.getTicketId() == null) {
                        System.out.println("Unable to create ticket!");
                        break;
                    }

                    System.out.println("Booking your seat...");

                    // STEP 1: Book the seat in Train.json.

                    boolean booked = trainService.bookTrainSeat(
                            trainSelectedForBooking,
                            row,
                            col
                    );

                    if (!booked) {

                        System.out.println("Can't book this seat!");

                        break;
                    }

                    // STEP 2: Save ticket in logged-in user's account.

                    try {

                        boolean saved = userService.addBooking(ticket);

                        if (saved) {

                            System.out.println(
                                    "\nSeat booked successfully!"
                            );

                            System.out.println(
                                    "Ticket saved to your account!"
                            );

                            System.out.println(ticket);

                            trainSelectedForBooking = null;

                            selectedSource = null;
                            selectedDestination = null;

                        } else {



                            trainService.releaseTrainSeat(
                                    trainSelectedForBooking,
                                    row,
                                    col
                            );

                            System.out.println(
                                    "Ticket could not be saved. Seat released."
                            );
                        }

                    } catch (IOException | RuntimeException e) {

                        // If user.json saving fails,
                        // release the previously booked seat.

                        try {

                            trainService.releaseTrainSeat(
                                    trainSelectedForBooking,
                                    row,
                                    col
                            );

                        } catch (RuntimeException rollbackError) {

                            e.addSuppressed(rollbackError);

                            System.out.println(
                                    "Warning: Seat rollback also failed!"
                            );
                        }

                        System.out.println(
                                "Booking failed: " + e.getMessage()
                        );
                    }

                    break;
                }


                //  CANCEL BOOKING


                case 6: {

                    if (!isLoggedIn) {
                        System.out.println("Please login first!");
                        break;
                    }

                    System.out.println("Fetching your bookings...");

                    List<Ticket> myBookings =
                            userService.fetchBooking();

                    if (myBookings.isEmpty()) {

                        System.out.println("You have no bookings to cancel.");

                        break;
                    }

                    System.out.println("\nYour bookings:");

                    for (Ticket ticket : myBookings) {
                        System.out.println(ticket);
                    }

                    System.out.println("Enter Ticket ID to cancel:");

                    String ticketId = scanner.next();

                    try {

                        boolean cancelled =
                                userService.cancelBooking(ticketId);

                        if (cancelled) {

                            System.out.println(
                                    "Booking cancelled successfully!"
                            );

                        } else {

                            System.out.println(
                                    "Invalid Ticket ID or booking not found."
                            );
                        }

                    } catch (IOException e) {

                        System.out.println(
                                "Unable to cancel booking: " + e.getMessage()
                        );
                    }

                    break;
                }


                //  EXIT


                case 7: {

                    userService.logout();

                    System.out.println(
                            "Thank you for using Train Booking System!"
                    );

                    break;
                }

                default: {

                    System.out.println(
                            "Invalid option! Please select 1-7."
                    );

                    break;
                }
            }
        }

        scanner.close();
    }


    // CREATE TICKET


    private static Ticket createTicket(
            Train train,
            String source,
            String destination,
            int row,
            int seat
    ) {

        ObjectMapper mapper = new ObjectMapper();

        mapper.setPropertyNamingStrategy(
                PropertyNamingStrategies.SNAKE_CASE
        );

        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
        );

        Map<String, Object> ticketData = new HashMap<>();

        ticketData.put(
                "ticket_id",
                UUID.randomUUID().toString()
        );

        ticketData.put(
                "train_id",
                train.getTrainId()
        );

        ticketData.put(
                "train_no",
                String.valueOf(train.getTrainNo())
        );

        ticketData.put("source", source);

        ticketData.put("destination", destination);

        ticketData.put("row", row);

        ticketData.put("seat", seat);

        return mapper.convertValue(ticketData, Ticket.class);
    }
}