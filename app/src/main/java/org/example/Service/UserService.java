package org.example.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import org.example.Entities.Ticket;
import org.example.Entities.User;
import org.example.Utill.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserService {

    private List<User> userList;

    // Actual logged-in user

    private User loggedInUser;

    private static final String USER_PATH =
            "/home/shovanparia/HelloGradle/app/src/main/java/org/example/LocalDb/user.json";

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

    public UserService() throws IOException {
        loadUserListFromFile();
    }

    //  Load users from JSON

    private synchronized void loadUserListFromFile()
            throws IOException {

        File userFile = new File(USER_PATH);

        if (!userFile.exists() || userFile.length() == 0) {

            userList = new ArrayList<>();

            saveUserListToFile();

            return;
        }

        userList = objectMapper.readValue(
                userFile,
                new TypeReference<List<User>>() {}
        );

        if (userList == null) {
            userList = new ArrayList<>();
        }
    }

    // 2. Signup

    public synchronized boolean signUp(User user1)
            throws IOException {

        if (user1 == null ||
                user1.getName() == null ||
                user1.getName().isBlank()) {
            return false;
        }

        // Check whether username already exists

        boolean alreadyExists = userList.stream()
                .filter(existingUser -> existingUser != null)
                .anyMatch(existingUser ->
                        existingUser.getName() != null &&
                                existingUser.getName()
                                        .equalsIgnoreCase(user1.getName())
                );

        if (alreadyExists) {
            return false;
        }

        userList.add(user1);

        try {

            saveUserListToFile();

            return true;

        } catch (IOException e) {

            userList.remove(user1);

            throw e;
        }
    }

    //  Login

    public synchronized boolean loginUser(User user1) {

        // Clear previous

        loggedInUser = null;

        if (user1 == null ||
                user1.getName() == null ||
                user1.getPassword() == null) {
            return false;
        }

        Optional<User> foundUser = userList.stream()
                .filter(checkingUser -> checkingUser != null)
                .filter(checkingUser ->
                        checkingUser.getName() != null
                )
                .filter(checkingUser ->
                        checkingUser.getName().equalsIgnoreCase(
                                user1.getName()
                        )
                )
                .filter(checkingUser ->
                        checkingUser.getHashedPassword() != null
                )
                .filter(checkingUser ->
                        UserServiceUtil.checkPassword(
                                user1.getPassword(),
                                checkingUser.getHashedPassword()
                        )
                )
                .findFirst();

        if (foundUser.isPresent()) {

            // Store actual user from JSON.

            loggedInUser = foundUser.get();

            return true;
        }

        return false;
    }

    // 4. Get logged-in user

    public User getLoggedInUser() {
        return loggedInUser;
    }

    // 5. Add booking to logged-in user

    public synchronized boolean addBooking(Ticket ticket)
            throws IOException {

        if (loggedInUser == null || ticket == null) {
            return false;
        }

        // Initialize ticket list if null

        if (loggedInUser.getTicketsBooked() == null) {

            loggedInUser.setTicketsBooked(new ArrayList<>());
        }

        List<Ticket> tickets = loggedInUser.getTicketsBooked();

        // Add ticket to actual logged-in user

        tickets.add(ticket);

        try {

            // Save complete user list into user.json

            saveUserListToFile();

            return true;

        } catch (IOException e) {

            // Rollback if saving fails

            tickets.remove(ticket);

            throw e;
        }
    }

    // 6. Fetch bookings

    public synchronized List<Ticket> fetchBooking() {

        if (loggedInUser == null ||
                loggedInUser.getTicketsBooked() == null) {

            return new ArrayList<>();
        }

        return new ArrayList<>(
                loggedInUser.getTicketsBooked()
        );
    }

    //  Find booking using ticket ID

    public synchronized Ticket findBookingByTicketId(
            String ticketId
    ) {

        if (loggedInUser == null ||
                ticketId == null ||
                loggedInUser.getTicketsBooked() == null) {

            return null;
        }

        return loggedInUser.getTicketsBooked()
                .stream()
                .filter(ticket -> ticket != null)
                .filter(ticket -> ticket.getTicketId() != null)
                .filter(ticket ->
                        ticket.getTicketId().equalsIgnoreCase(ticketId)
                )
                .findFirst()
                .orElse(null);
    }

    // 8. Cancel booking

    public synchronized boolean cancelBooking(String ticketId)
            throws IOException {

        if (loggedInUser == null || ticketId == null) {
            return false;
        }

        List<Ticket> booked = loggedInUser.getTicketsBooked();

        if (booked == null || booked.isEmpty()) {
            return false;
        }

        int ticketIndex = -1;

        // find ticket

        for (int i = 0; i < booked.size(); i++) {

            Ticket ticket = booked.get(i);

            if (ticket != null &&
                    ticket.getTicketId() != null &&
                    ticket.getTicketId().equalsIgnoreCase(ticketId)) {

                ticketIndex = i;

                break;
            }
        }

        if (ticketIndex == -1) {
            return false;
        }

        // Remove ticket

        Ticket removedTicket = booked.remove(ticketIndex);

        try {

            saveUserListToFile();

            return true;

        } catch (IOException e) {

            // Restore ticket if saving fails

            booked.add(ticketIndex, removedTicket);

            throw e;
        }
    }

    //  Logout

    public synchronized void logout() {
        loggedInUser = null;
    }

    //  Save users into JSON

    private synchronized void saveUserListToFile()
            throws IOException {

        File userFile = new File(USER_PATH);

        File parent = userFile.getParentFile();

        if (parent != null) {
            parent.mkdirs();
        }

        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(userFile, userList);
    }
}