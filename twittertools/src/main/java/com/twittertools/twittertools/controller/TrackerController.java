package com.twittertools.twittertools.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twittertools.twittertools.entity.User;
import com.twittertools.twittertools.json.TwitterResponseMapper;
import com.twittertools.twittertools.json.UserMapper;
import com.twittertools.twittertools.service.PdfGeneratorService;
import com.twittertools.twittertools.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class TrackerController {

    private final List<String> bearerTokens = List.of(
            "AAAAAAAAAAAAAAAAAAAAANawYAEAAAAAKeDRyXb7oaA%2FfVVr5WNC00fEMc0%3DI4AfCJxUUjuE36mpp3yJLZ8U9BRbQZ54gIEmAkeFXrabgYEO2X",
            "AAAAAAAAAAAAAAAAAAAAAJv%2FYQEAAAAA5JziViWqOLVRMC8%2F%2B72Jebj3Ic0%3DtPSAxfwjpGmx7WgUwlYmOGwWSVKwDGP38AcQMt7yV51zvErpGp",
            "AAAAAAAAAAAAAAAAAAAAAD8AYgEAAAAAsH96qourEpw2wpI0e5oXq92S1h0%3DfsnYbjHgA0MC0DQ1FS9KLQqJOTQdwuFsxuHkcBQRI3M6azCyU6",
            "AAAAAAAAAAAAAAAAAAAAALkAYgEAAAAAq0Z1PSGgrvN2eQZ%2FJctBV0GNr5U%3DUKR9fY9P01cJsemPpV49L64e4bKE06TPU7jiNMFy6VjVXfw0Qq"
    );
    private String bearerToken = bearerTokens.get(0);
    private final UserService userService;
    private final PdfGeneratorService pdfGeneratorService;

    public TrackerController(UserService trackerService, PdfGeneratorService pdfGeneratorService) {
        this.userService = trackerService;
        this.pdfGeneratorService = pdfGeneratorService;
    }

    @GetMapping("/find")
    public User byId(@RequestParam String id){
        return userService.findById(id);
    }

    @GetMapping("/save-users-info")
    public void saveUsersInfo() throws IOException {
        String ids = getUsersIds().stream()
                .map(UserMapper::getId)
                .collect(Collectors.joining(","));

        URL url = new URL("https://api.twitter.com/2/users?ids=" + ids + "&user.fields=description,profile_image_url");
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        http.setRequestProperty("Accept", "application/json");
        http.setRequestProperty("Authorization", "Bearer " + bearerToken);

        String json = readHttpContent(http);

        ObjectMapper objectMapper = new ObjectMapper();
        TwitterResponseMapper twitterResponseMapper = objectMapper.readValue(json, TwitterResponseMapper.class);
        userService.saveUsers(twitterResponseMapper.getData());

        http.disconnect();
    }

    @GetMapping("/get-users-ids")
    public List<UserMapper> getUsersIds() throws IOException {
        Path path = Paths.get("C:\\Users\\Michal\\IdeaProjects\\twittertools\\twittertools\\src\\main\\resources\\usersToObserve.txt");
        List<String> usernames = Files.readAllLines(path);

        List<UserMapper> userMapperList = new ArrayList<>();

        for (String username : usernames) {
            try {
                URL url = new URL("https://api.twitter.com/2/users/by?usernames=" + username);
                HttpURLConnection http = (HttpURLConnection)url.openConnection();
                http.setRequestProperty("Accept", "application/json");
                http.setRequestProperty("Authorization", "Bearer " + bearerToken);

                if (http.getResponseCode()==429){
                    bearerToken = getNextBearerToken(bearerToken);
                    continue;
                }

                String json = readHttpContent(http);

                ObjectMapper objectMapper = new ObjectMapper();
                TwitterResponseMapper twitterResponseMapper = objectMapper.readValue(json, TwitterResponseMapper.class);
                http.disconnect();
                userMapperList.add(twitterResponseMapper.getData().get(0));
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return userMapperList;
    }

    @GetMapping("/save-users-following")
    public void saveUsersFollowing(
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to) throws IOException
    {
        List<String> ids = userService.getUsers(Optional.ofNullable(from), Optional.ofNullable(to)).stream().map(User::getId).collect(Collectors.toList());

        for (String id : ids) {
            List<UserMapper> usersMapperList = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
            String nextPageToken = "start";
            try {
                while (nextPageToken != null) {
                    String urlStr = "https://api.twitter.com/2/users/" + id + "/following?user.fields=description,profile_image_url&max_results=1000" + "&pagination_token=" + nextPageToken;

                    if (nextPageToken.equals("start"))
                        urlStr = "https://api.twitter.com/2/users/" + id + "/following?user.fields=description,profile_image_url&max_results=1000";

                    URL url = new URL(urlStr);
                    HttpURLConnection http = (HttpURLConnection) url.openConnection();
                    http.setRequestProperty("Accept", "application/json");
                    http.setRequestProperty("Authorization", "Bearer " + bearerToken);

                    if (http.getResponseCode() == 429) {
                        bearerToken = getNextBearerToken(bearerToken);
                        continue;
                    }

                    String json = readHttpContent(http);

                    TwitterResponseMapper twitterResponseMapper = objectMapper.readValue(json, TwitterResponseMapper.class);
                    nextPageToken = twitterResponseMapper.getMeta().getNextToken();
                    usersMapperList.addAll(twitterResponseMapper.getData());

                    http.disconnect();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            userService.updateUser(id, usersMapperList);
        }
    }

    @GetMapping("/generate")
    public void generateReport(
            HttpServletResponse response,
            @RequestParam(required = false) Integer from,
            @RequestParam(required = false) Integer to) throws IOException
    {
        List<User> usersToReport = new ArrayList<>();

        List<String> ids = userService.getUsers(Optional.ofNullable(from), Optional.ofNullable(to)).stream().map(User::getId).collect(Collectors.toList());

        for (String id : ids) {
            List<UserMapper> usersMapperList = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
            String nextPageToken = "start";
            try {
                while (nextPageToken != null) {
                    String urlStr = "https://api.twitter.com/2/users/" + id + "/following?user.fields=description,profile_image_url&max_results=1000" + "&pagination_token=" + nextPageToken;

                    if (nextPageToken.equals("start"))
                        urlStr = "https://api.twitter.com/2/users/" + id + "/following?user.fields=description,profile_image_url&max_results=1000";

                    URL url = new URL(urlStr);
                    HttpURLConnection http = (HttpURLConnection) url.openConnection();
                    http.setRequestProperty("Accept", "application/json");
                    http.setRequestProperty("Authorization", "Bearer " + bearerToken);

                    if (http.getResponseCode()==429){
                        bearerToken = getNextBearerToken(bearerToken);
                        continue;
                    }

                    String json = readHttpContent(http);

                    TwitterResponseMapper twitterResponseMapper = objectMapper.readValue(json, TwitterResponseMapper.class);
                    nextPageToken = twitterResponseMapper.getMeta().getNextToken();
                    usersMapperList.addAll(twitterResponseMapper.getData());

                    http.disconnect();
                }
            }
            catch (Exception e){
                e.printStackTrace();
                continue;
            }

            List<User> userInDb = userService.findById(id).getUsers();
            List<User> following = usersMapperList.stream()
                    .map(UserMapper::mapToUser)
                    .collect(Collectors.toList());

            List<String> userDbIds = userInDb.stream().map(User::getId).collect(Collectors.toList());
            List<String> followingIds = following.stream().map(User::getId).collect(Collectors.toList());
            followingIds.removeAll(userDbIds);

            List<User> users = following.stream()
                    .filter(user -> followingIds.contains(user.getId()))
                    .collect(Collectors.toList());

            User byId = userService.findById(id);
            byId.setUsers(users);
            usersToReport.add(byId);

            if (users.size()!=0) {
                User byId1 = userService.findById(id);
                byId1.setUsers(following);
                userService.saveUser(byId1);
            };
        }

        generatePDF(response, usersToReport);
//        saveUsersInfo();
//        saveUsersFollowing(null, null);
    }

    @GetMapping("/users")
    public List<User> getAllUsers(){
        List<User> users = userService.getUsers();
        return users;
    }

    private void generatePDF(HttpServletResponse response, List<User> users) throws IOException {
        response.setContentType("application/pdf");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());

        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=pdf_" + currentDateTime + ".pdf";
        response.setHeader(headerKey, headerValue);

        this.pdfGeneratorService.export(response, users);
    }

    private String readHttpContent(HttpURLConnection http) throws IOException {
        BufferedReader reader;
        String line;
        StringBuilder responseContent = new StringBuilder();

        reader = new BufferedReader(new InputStreamReader(http.getInputStream()));
        while ((line = reader.readLine()) != null) {
            responseContent.append(line);
        }
        reader.close();

        return responseContent.toString();
    }

    private String getNextBearerToken(String bearerToken){
        int indexOfActualToken = bearerTokens.indexOf(bearerToken);

        if (bearerTokens.size()-1 > indexOfActualToken) return bearerTokens.get(indexOfActualToken+1);
        return bearerTokens.get(0);
    }
}
